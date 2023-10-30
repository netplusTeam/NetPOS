package com.woleapp.netpos.ui.activities

import android.Manifest
import android.annotation.SuppressLint
import android.app.AlertDialog
import android.app.DatePickerDialog
import android.app.ProgressDialog
import android.content.* // ktlint-disable no-wildcard-imports
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import com.danbamitale.epmslib.entities.* // ktlint-disable no-wildcard-imports
import com.danbamitale.epmslib.extensions.formatCurrencyAmount
import com.danbamitale.epmslib.processors.TransactionProcessor
import com.danbamitale.epmslib.utils.IsoAccountType
import com.google.android.gms.tasks.OnCompleteListener
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.firebase.messaging.FirebaseMessaging
import com.google.gson.Gson
import com.pixplicity.easyprefs.library.Prefs
import com.woleapp.netpos.R
import com.woleapp.netpos.database.AppDatabase
import com.woleapp.netpos.databinding.ActivityMainBinding
import com.woleapp.netpos.databinding.LayoutPrintEndOfDayBinding
import com.woleapp.netpos.model.* // ktlint-disable no-wildcard-imports
import com.woleapp.netpos.model.AppConstants.FIREBASE_TOPIC_UPDATE
import com.woleapp.netpos.mqtt.MqttHelper
import com.woleapp.netpos.network.StormApiClient
import com.woleapp.netpos.network.TokenPassportRequest
import com.woleapp.netpos.network.getTokenClient
import com.woleapp.netpos.nibss.CONFIGURATION_STATUS
import com.woleapp.netpos.nibss.NetPosTerminalConfig
import com.woleapp.netpos.receivers.BatteryReceiver
import com.woleapp.netpos.ui.fragments.DashboardFragment
import com.woleapp.netpos.ui.fragments.SettingsFragment
import com.woleapp.netpos.ui.fragments.TransactionHistoryFragment
import com.woleapp.netpos.ui.fragments.TransactionsFragment
import com.woleapp.netpos.util.* // ktlint-disable no-wildcard-imports
import com.woleapp.netpos.util.ModelMapper.mapRowToTransactionResponse
import com.woleapp.netpos.viewmodels.NetPosViewModelFactories
import com.woleapp.netpos.viewmodels.PayByZenithViewModel
import com.woleapp.netpos.viewmodels.TransactionsViewModel
import dagger.hilt.android.AndroidEntryPoint
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import pub.devrel.easypermissions.EasyPermissions
import timber.log.Timber
import java.sql.Timestamp
import java.text.SimpleDateFormat
import java.time.Instant
import java.util.*

const val TAG = "FIRE_BASE_TOKEN"
const val TAG1 = "FIRE_BASE_TOKEN_1"
const val TAG2 = "FIRE_BASE_TOKEN_2"

@AndroidEntryPoint
class MainActivity : AppCompatActivity(), EasyPermissions.PermissionCallbacks {
    private lateinit var bottomNavView: BottomNavigationView
    private val transactionViewModel by viewModels<TransactionsViewModel> {
        NetPosViewModelFactories(AppDatabase.getDatabaseInstance(this))
    }
    private val gson: Gson = Gson()
    private val stormApiService = StormApiClient.getStormApiLoginInstance()
    private lateinit var endOfDayProgressDialog: ProgressDialog

    private lateinit var firebaseInstance: FirebaseMessaging
    private val payByTransferViewModel: PayByZenithViewModel by viewModels()

    private var progressDialog: ProgressDialog? = null
    private lateinit var alertDialog: AlertDialog
    private lateinit var binding: ActivityMainBinding
    private var compositeDisposable: CompositeDisposable = CompositeDisposable()

    // private lateinit var client: MqttAndroidClient
    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            intent?.let {
                when (it.getIntExtra(CONFIGURATION_STATUS, -1)) {
                    0 -> showProgressDialog()
                    1 -> {
                        Toast.makeText(context!!, "Terminal Configured", Toast.LENGTH_LONG).show()
                        dismissProgressDialogIfShowing()
                    }
                    -1 -> {
                        dismissProgressDialogIfShowing()
                        showAlertDialog()
                    }
                }
            }
        }
    }
    private val iFilter = IntentFilter().apply {
        addAction(Intent.ACTION_POWER_CONNECTED)
        addAction(Intent.ACTION_POWER_DISCONNECTED)
        addAction(Intent.ACTION_BATTERY_CHANGED)
        addAction(Intent.ACTION_BATTERY_LOW)
        addAction(Intent.ACTION_BATTERY_OKAY)
    }
    private val batteryReceiver = BatteryReceiver()
    private fun showAlertDialog() {
        alertDialog.apply {
            setMessage("Terminal Configuration Failed, You won't be able to make any transactions, if the problem persists contact an Administrator")
            show()
        }
    }

    override fun onStop() {
        super.onStop()
        // LocalBroadcastManager.getInstance(this).unregisterReceiver(receiver)
        unregisterReceiver(batteryReceiver)
    }

    override fun onStart() {
        super.onStart()

        registerReceiver(batteryReceiver, iFilter)
        // LocalBroadcastManager.getInstance(this).registerReceiver(receiver, IntentFilter(CONFIGURATION_ACTION))
        when ( // NetPosTerminalConfig.isConfigurationInProcess -> showProgressDialog()
            NetPosTerminalConfig.configurationStatus
        ) {
            -1 -> NetPosTerminalConfig.init(
                applicationContext,
            )
            1 -> {
                dismissProgressDialogIfShowing()
            }
        }
//        checkTokenExpiry()
    }

    private fun logoutConfirmation() {
        AlertDialog.Builder(this)
            .setMessage(R.string.logout_confirmation_dialog_message) // Specifying a listener allows you to take an action before dismissing the dialog.
            .setPositiveButton(android.R.string.yes) { _: DialogInterface, _: Int ->
                logout()
            } // A null listener allows the button to dismiss the dialog and take no further action.
            .setNegativeButton(android.R.string.no, null).show()
    }

    private fun logout() {
        NetPosTerminalConfig.disposeDisposables()
        Prefs.clear()
        MqttHelper.disconnect()
        val intent = Intent(this, AuthenticationActivity::class.java)
        intent.flags =
            Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    private fun checkTokenExpiry() {
        val token = Prefs.getString(PREF_USER_TOKEN, null)
        token?.let {
            if (JWTHelper.isExpired(it)) {
                Timber.e("is expired")
                logout()
            }
        }
    }

    private fun dismissProgressDialogIfShowing() {
        progressDialog?.dismiss()
    }

    private fun showProgressDialog() {
        progressDialog?.show()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_main)
        bottomNavView = binding.bottomNavigationView

        endOfDayProgressDialog = ProgressDialog(this).apply {
            this.setCancelable(false)
            this.setMessage(getString(R.string.please_wait))
            this.setButton(
                DialogInterface.BUTTON_POSITIVE,
                getString(R.string.cancel),
            ) { dialog, _ ->
                compositeDisposable.clear()
                dialog.cancel()
            }
        }
        getIswToken(this)

        progressDialog = ProgressDialog(this).apply {
            setMessage("Configuring Terminal, Please wait")
            setCancelable(false)
        }

        val user = Singletons.gson.fromJson(Prefs.getString(PREF_USER, ""), User::class.java)
        if (Prefs.getString(PREF_USER, "").isEmpty()) {
            Toast.makeText(this, getString(R.string.please_login), Toast.LENGTH_LONG).show()
            onBackPressed()
            return
        } else {
            binding.dashboardHeader.username.text = user.business_name
        }
        firebaseInstance = FirebaseMessaging.getInstance()

        subscribeToFireBaseMessagingTopic(firebaseInstance, FIREBASE_TOPIC_UPDATE)

        getFireBaseToken(firebaseInstance) {
            val previousDeviceToken = Prefs.getString(PREF_FIREBASE_APP_TOKEN, "")
            if (previousDeviceToken != it) {
                sendTokenToBackend(it)
            }
        }

        // loadCerts()
        if (!EasyPermissions.hasPermissions(
                applicationContext,
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.READ_EXTERNAL_STORAGE,
            )
        ) {
            EasyPermissions.requestPermissions(
                this,
                "Accept Location and Storage Permissions",
                1,
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.READ_EXTERNAL_STORAGE,
            )
        }
        alertDialog = AlertDialog.Builder(this).run {
            setCancelable(false)
            title = "Message"
            setPositiveButton("Retry") { dialog, _ ->
                NetPosTerminalConfig.init(applicationContext)
                dialog.dismiss()
            }
            setNegativeButton("Cancel") { dialog, _ ->
                Toast.makeText(applicationContext, "Configuration cancelled", Toast.LENGTH_LONG)
                    .show()
                dialog.dismiss()
            }
            create()
        }
        binding.dashboardHeader.logout.setOnClickListener {
            logoutConfirmation()
        }
        showFragment(DashboardFragment(), DashboardFragment::class.java.simpleName)

        // Handle Bottom Sheet Item selection
        bottomNavView.setOnItemSelectedListener {
            when (it.itemId) {
                R.id.transaction -> {
                    showFragment(
                        TransactionsFragment(),
                        TransactionsFragment::class.java.simpleName,
                    )
                    return@setOnItemSelectedListener true
                }

                R.id.balance -> {
                    getBalance()
                    return@setOnItemSelectedListener true
                }

                R.id.endOfDay -> {
                    showCalendarDialog()
                    return@setOnItemSelectedListener true
                }

                R.id.settings -> {
                    showFragment(SettingsFragment(), SettingsFragment::class.java.simpleName)
                    return@setOnItemSelectedListener true
                }
                else -> {
                    showFragment(DashboardFragment(), DashboardFragment::class.java.simpleName)
                    return@setOnItemSelectedListener true
                }
            }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray,
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        EasyPermissions.onRequestPermissionsResult(requestCode, permissions, grantResults, this)
    }

    override fun onPermissionsGranted(requestCode: Int, perms: MutableList<String>) {
        getLocationUpdates()
    }

    override fun onPermissionsDenied(requestCode: Int, perms: MutableList<String>) {
    }

    @SuppressLint("MissingPermission")
    private fun getLocationUpdates() {
        val locationManager = this.getSystemService(LOCATION_SERVICE) as LocationManager
        val locationListener: LocationListener = object : LocationListener {
            override fun onLocationChanged(location: Location) {
                // Called when a new location is found by the network location provider.
                location.let {
                    Prefs.putString(PREF_LAST_LOCATION, "lat:${it.latitude} long:${it.longitude}")
                    // Timber.e("lat:${it.latitude} long:${it.longitude}")
                }
            }

            override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {
                Timber.e("On status changed")
            }

            override fun onProviderEnabled(provider: String) {
                Timber.e("On Provider enabled: $provider")
            }

            override fun onProviderDisabled(provider: String) {
                Timber.e("On Provider disabled $provider")
            }
        }
        locationManager.requestLocationUpdates(
            LocationManager.GPS_PROVIDER,
            0L,
            0f,
            locationListener,
        )
        // locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
        locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
    }

    private fun showFragment(targetFragment: Fragment, className: String) {
        try {
            supportFragmentManager.beginTransaction()
                .apply {
                    replace(R.id.container_main, targetFragment, className)
                    setCustomAnimations(R.anim.right_to_left, android.R.anim.fade_out)
                    commit()
                }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        compositeDisposable.dispose()
    }

    private fun sendTokenToBackend(token: String) {
        Timber.d("FIRE_BASE_TOKEN_1==>%s", token)
        payByTransferViewModel.registerDeviceToken(token)
    }

    private fun getFireBaseToken(
        firebaseMessagingInstance: FirebaseMessaging,
        actionToPerformWithTheReceivedToken: (received: String) -> Unit,
    ) {
        firebaseMessagingInstance.token.addOnCompleteListener(
            OnCompleteListener { task ->
                if (!task.isSuccessful) {
                    Log.w(TAG1, "Fetching FCM registration token failed", task.exception)
                    return@OnCompleteListener
                }

                // Get new FCM registration token
                val token = task.result
                Prefs.putString(PREF_FIREBASE_APP_TOKEN, token)
                actionToPerformWithTheReceivedToken(token)
            },
        )
    }

    private fun subscribeToFireBaseMessagingTopic(
        firebaseMessagingInstance: FirebaseMessaging,
        fireBaseTopic: String,
    ) {
        firebaseMessagingInstance.subscribeToTopic(fireBaseTopic)
    }

    private fun getBalance() {
        showCardDialog(
            this,
            this,
            0,
            0L,
        ).observe(this) { event ->
            event.getContentIfNotHandled()?.let {
                it.error?.let { error ->
                    Timber.d(error)
                    Toast.makeText(this, error.localizedMessage, Toast.LENGTH_SHORT)
                        .show()
                }
                it.cardData?.let { cardData ->
                    checkBalance(cardData, it.accountType!!)
                }
            }
        }
    }

    private fun checkBalance(
        cardData: CardData,
        accountType: IsoAccountType = IsoAccountType.DEFAULT_UNSPECIFIED,
    ) {
        if (NetPosTerminalConfig.getKeyHolder() == null) {
            Toast.makeText(
                this,
                getString(R.string.terminal_not_configured),
                Toast.LENGTH_LONG,
            ).show()
            return
        }

        val hostConfig = HostConfig(
            NetPosTerminalConfig.getTerminalId(),
            NetPosTerminalConfig.connectionData,
            NetPosTerminalConfig.getKeyHolder()!!,
            NetPosTerminalConfig.getConfigData()!!,
        )
        val requestData =
            TransactionRequestData(TransactionType.BALANCE, 0L, accountType = accountType)
        progressDialog!!.setMessage(getString(R.string.checking_bal))
        progressDialog!!.show()
        val processor = TransactionProcessor(hostConfig)
        // processor.
        processor.processTransaction(this, requestData, cardData)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe { response, error ->
                if (progressDialog!!.isShowing) {
                    progressDialog!!.dismiss()
                }
                error?.let {
                    it.printStackTrace()
                    Toast.makeText(
                        this,
                        "Error ${it.localizedMessage}",
                        Toast.LENGTH_SHORT,
                    ).show()
                }

                response?.let {
                    if (it.responseCode == "A3") {
                        Prefs.remove(PREF_CONFIG_DATA)
                        Prefs.remove(PREF_KEYHOLDER)
                        NetPosTerminalConfig.init(
                            this.applicationContext,
                            configureSilently = true,
                        )
                    }

                    val messageString = if (it.isApproved) {
                        "${getString(R.string.acc_bal)}\n " + it.accountBalances.joinToString("\n") { accountBalance ->
                            "${accountBalance.accountType}, ${
                                accountBalance.amount.div(100).formatCurrencyAmount()
                            }"
                        }
                    } else {
                        "${it.responseMessage}(${it.responseCode})"
                    }

                    showMessage(
                        if (it.isApproved) getString(R.string.approved) else getString(R.string.declined),
                        messageString,
                    )
                }
            }.disposeWith(compositeDisposable)
    }

    private fun showMessage(s: String, messageString: String) {
        AlertDialog.Builder(this)
            .apply {
                setTitle(s)
                setMessage(messageString)
                setPositiveButton("Ok") { dialog, _ ->
                    dialog.dismiss()
                }
                create().show()
            }
    }

    private fun showEndOfDayBottomSheetDialog(transactions: List<TransactionResponse>) {
        val approvedList = transactions.filter { it.responseCode == "00" }
        val declinedList = transactions.filter { it.responseCode != "00" }
        val endOfDay =
            LayoutPrintEndOfDayBinding.inflate(LayoutInflater.from(this@MainActivity), null, false)
        endOfDay.apply {
            approvedCount.text = approvedList.size.toString()
            declinedCount.text = declinedList.size.toString()
            totalTransactionsAmount.text =
                getString(
                    R.string.total_transaction_amount,
                    approvedList.sumOf { it.amount }.div(100).formatCurrencyAmount(),
                )
            totalTransactions.text =
                getString(R.string.total_transaction_count, transactions.size.toString())
            print.setOnClickListener {
                if (transactions.isEmpty()) {
                    Toast.makeText(
                        this@MainActivity,
                        getString(R.string.noTransactionsToPrint),
                        Toast.LENGTH_LONG,
                    ).show()
                } else {
                    when (chipGroup.checkedChipId) {
                        R.id.print_approved -> approvedList
                        R.id.print_declined -> declinedList
                        else -> transactions
                    }.apply {
                        if (isEmpty()) {
                            Toast.makeText(
                                this@MainActivity,
                                getString(R.string.noTransactionsToPrint),
                                Toast.LENGTH_SHORT,
                            ).show()
                            return@setOnClickListener
                        }
                    }.printEndOfDay(this@MainActivity)
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe({ printResp ->
                            Timber.e(printResp.toString())
                        }, { err ->
                            Toast.makeText(
                                this@MainActivity,
                                err.localizedMessage,
                                Toast.LENGTH_LONG,
                            )
                                .show()
                            // Timber.e(err.localizedMessage)
                        }).disposeWith(CompositeDisposable())
                }
            }
        }
        val bottomSheet = BottomSheetDialog(this@MainActivity, R.style.SheetDialog)
            .apply {
                dismissWithAnimation = true
                setCancelable(false)
                setContentView(endOfDay.root)
                show()
            }
        endOfDay.view.setOnClickListener {
            if (transactions.isNotEmpty()) {
                transactionViewModel.setEndOfDayList(
                    transactions.map { trns ->
                        trns.copy(
                            localDate_13 = trns.localDate_13 + PDF_REPRINT_IDENTIFIER,
                        )
                    },
                )
                bottomSheet.dismiss()
                showFragment(
                    TransactionHistoryFragment.newInstance(HISTORY_ACTION_EOD),
                    TransactionHistoryFragment::class.java.simpleName,
                )
            } else {
                Toast.makeText(this, getString(R.string.noTransactionsToView), Toast.LENGTH_LONG)
                    .show()
            }
        }
        endOfDay.closeButton.setOnClickListener {
            bottomSheet.dismiss()
        }
    }

    private fun getEndOfDayLocal(be: Long, be1: Long) =
        AppDatabase.getDatabaseInstance(this@MainActivity)
            .transactionResponseDao()
            .getEndOfDayTransactionSingle(be, be1, NetPosTerminalConfig.getTerminalId())
            .doOnError {
                Timber.d("ERROR_HAPPENING=========>%s", it.localizedMessage)
            }
            .flatMap { transactionList ->
                Single.just(
                    GateWayTransactionResponse(
                        ModelMapper.mapTransFromGateWayToEntity(transactionList),
                        transactionList.size,
                        1,
                        1000,
                    ),
                )
            }

    private fun showCalendarDialog() {
        val calendar = Calendar.getInstance()
        DatePickerDialog(
            this@MainActivity,
            { _, i, i2, i3 ->
                getEndOfDayTransactions(
                    Calendar.getInstance().apply { set(i, i2, i3) }.timeInMillis,
                )
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH),
        ).show()
    }

    private fun getEndOfDayTransactions(timestamp: Long? = null) {
        endOfDayProgressDialog.show()
        val be: Long = getBeginningOfDay(timestamp)
        val be1: Long = Timestamp.from(Instant.ofEpochMilli(be).plusSeconds(86400)).time
        val df = SimpleDateFormat("dd:MM:yyyy hh:mm:ss", Locale.getDefault())

        val data = HashMap<String, String>().apply {
            put("terminalId", NetPosTerminalConfig.getTerminalId())
            put("count", "1000")
            put("from", df.format(be))
            put("to", df.format(be1))
        }

        val parameters = GetEodFromNewServiceModel(
            NetPosTerminalConfig.getTerminalId().trim(),
            RandomNumUtil.getDateInTheFormatExpectedByTheNewService(df.format(be)),
            RandomNumUtil.getDateInTheFormatExpectedByTheNewServiceForEnd(df.format(be)),
            1,
            1000,
        )

        getEndOfDayLocal(
            RandomNumUtil.getDateInMilliSecsForLocal(df.format(be)),
            RandomNumUtil.getDateInMilliSecsForLocalForEndOfDay(df.format(be)),
        )
            .flatMap { gateWayResp ->
                if (gateWayResp.result.isEmpty()) {
                    return@flatMap stormApiService.getTransactionsFromNewService(
                        parameters.terminalId,
                        parameters.from,
                        parameters.to,
                        parameters.page,
                        parameters.pageSize,
                    ).map {
                        val dataWithModifiedAmount = it.data.rows.map { it1 ->
                            it1.copy(amount = (it1.amount as Int * 100))
                        }
                        val modifiedData = it.data.copy(
                            rows = dataWithModifiedAmount,
                        )
                        Single.just(it.copy(data = modifiedData))
                    }
                } else {
                    Single.just(gateWayResp)
                }
            }.retry(2)
            .onErrorResumeNext {
                stormApiService.getTransactionsFromNewService(
                    parameters.terminalId,
                    parameters.from,
                    parameters.to,
                    parameters.page,
                    parameters.pageSize,
                ).map {
                    val dataWithModifiedAmount = it.data.rows.map { it1 ->
                        it1.copy(amount = (it1.amount as Double * 100))
                    }
                    val modifiedData = it.data.copy(
                        rows = dataWithModifiedAmount,
                    )
                    Single.just(it.copy(data = modifiedData))
                }
            }
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .doFinally {
                endOfDayProgressDialog.dismiss()
            }
            .subscribe { t1, t2 ->
                t1?.let {
                    try {
                        val response = gson.fromJson(gson.toJson(it), NewEodModel::class.java)
                        showEndOfDayBottomSheetDialog(response.value.data.rows.mapRowToTransactionResponse())
                    } catch (e: Exception) {
                        when (it) {
                            is GetEndOfDayModelFromNewServer -> {
                                showEndOfDayBottomSheetDialog(it.data.rows.mapRowToTransactionResponse())
                            }
                            is GateWayTransactionResponse -> {
                                showEndOfDayBottomSheetDialog(
                                    ModelMapper.mapEntityToTransFromGateWay(
                                        it.result,
                                    ),
                                )
                            }
                            is NewEodModel -> {
                                showEndOfDayBottomSheetDialog(it.value.data.rows.mapRowToTransactionResponse())
                            }
                            else -> {
                                showEndOfDayBottomSheetDialog(listOf())
                            }
                        }
                    }
                }
                t2?.let {
                    Timber.d(it)
                    Toast.makeText(
                        this@MainActivity,
                        getString(R.string.error_occured),
                        Toast.LENGTH_LONG,
                    ).show()
                }
            }.disposeWith(compositeDisposable)
//        val livedata = AppDatabase.getDatabaseInstance(this@MainActivity)
//            .transactionResponseDao()
//            .getEndOfDayTransaction(
//                getBeginningOfDay(timestamp),
//                getEndOfDayTimeStamp(timestamp),
//                NetPosTerminalConfig.getTerminalId()
//            )
//        livedata.observe(viewLifecycleOwner) {
//            showEndOfDayBottomSheetDialog(it)
//            livedata.removeObservers(viewLifecycleOwner)
//        }
    }

    private fun getIswToken(context: Context) {
        Timber.d("CALLED")
        val req = TokenPassportRequest(
            context.getString(R.string.userMD),
            Singletons.getCurrentlyLoggedInUser()!!.terminal_id!!,
        )
        try {
            val disposable = CompositeDisposable()
            disposable.add(
                getTokenClient.getToken(req)
                    .doOnError {
                        Timber.d("TOKEN_ERROR==>${it.localizedMessage}")
                    }
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe { t1, t2 ->
                        t1?.let {
                            Timber.d("TOKEN_RESPONSE==>${it.token}")
                            Prefs.putString(AppConstants.ISW_TOKEN, it.token)
                        }
                        t2?.let {
                        }
                    },
            )
            disposable.clear()
        } catch (e: Exception) {
            Timber.d("GET_ISW_TOKEN_ERROR===>%s$e")
        }
    }
}
