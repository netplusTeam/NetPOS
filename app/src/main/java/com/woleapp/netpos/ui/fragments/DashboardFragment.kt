@file:Suppress("DEPRECATION")

package com.woleapp.netpos.ui.fragments

import android.app.AlertDialog
import android.app.DatePickerDialog
import android.app.ProgressDialog
import android.content.Context
import android.content.DialogInterface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.GridLayoutManager
import androidx.work.Constraints
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.danbamitale.epmslib.entities.* // ktlint-disable no-wildcard-imports
import com.danbamitale.epmslib.extensions.formatCurrencyAmount
import com.danbamitale.epmslib.processors.TransactionProcessor
import com.danbamitale.epmslib.utils.IsoAccountType
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.gson.Gson
import com.pixplicity.easyprefs.library.Prefs
import com.woleapp.netpos.BuildConfig
import com.woleapp.netpos.R
import com.woleapp.netpos.adapter.ServiceAdapter
import com.woleapp.netpos.database.AppDatabase
import com.woleapp.netpos.databinding.FragmentDashboardBinding
import com.woleapp.netpos.databinding.LayoutPrintEndOfDayBinding
import com.woleapp.netpos.model.* // ktlint-disable no-wildcard-imports
import com.woleapp.netpos.network.StormApiClient
import com.woleapp.netpos.network.TokenPassportRequest
import com.woleapp.netpos.network.getTokenClient
import com.woleapp.netpos.nibss.NetPosTerminalConfig
import com.woleapp.netpos.util.* // ktlint-disable no-wildcard-imports
import com.woleapp.netpos.util.ModelMapper.mapEntityToTransFromGateWay
import com.woleapp.netpos.util.ModelMapper.mapRowToTransactionResponse
import com.woleapp.netpos.util.ModelMapper.mapTransFromGateWayToEntity
import com.woleapp.netpos.util.RandomNumUtil.getDateInMilliSecsForLocal
import com.woleapp.netpos.util.RandomNumUtil.getDateInMilliSecsForLocalForEndOfDay
import com.woleapp.netpos.util.RandomNumUtil.getDateInTheFormatExpectedByTheNewService
import com.woleapp.netpos.util.RandomNumUtil.getDateInTheFormatExpectedByTheNewServiceForEnd
import com.woleapp.netpos.viewmodels.NetPosViewModelFactories
import com.woleapp.netpos.viewmodels.PayByZenithViewModel
import com.woleapp.netpos.viewmodels.TransactionsViewModel
import com.woleapp.netpos.worker.RepushFailedTransactionToBackendWorker
import dagger.hilt.android.AndroidEntryPoint
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import timber.log.Timber
import java.sql.Timestamp
import java.text.SimpleDateFormat
import java.time.Instant
import java.util.*
import javax.inject.Inject

@AndroidEntryPoint
class DashboardFragment : BaseFragment() {
    private lateinit var workManager: WorkManager
    private lateinit var progressDialog: ProgressDialog
    private lateinit var binding: FragmentDashboardBinding
    private lateinit var adapter: ServiceAdapter
    private var compositeDisposable = CompositeDisposable()
    private val stormApiService = StormApiClient.getStormApiLoginInstance()
    private lateinit var endOfDayProgressDialog: ProgressDialog
    private val transactionViewModel by activityViewModels<TransactionsViewModel> {
        NetPosViewModelFactories(AppDatabase.getDatabaseInstance(requireContext()))
    }
    private val zenithPbtViewModel by activityViewModels<PayByZenithViewModel>()
    private var userZenithPbtVirtualAccount: GetPayByTransferUserAccountModel? = null

    @Inject
    lateinit var gson: Gson

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        workManager = WorkManager.getInstance(requireActivity().applicationContext)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        zenithPbtViewModel.saveTestTransactions(testPbtTransactions)
        binding = FragmentDashboardBinding.inflate(inflater, container, false)
        progressDialog = ProgressDialog(requireContext())
        endOfDayProgressDialog = ProgressDialog(requireContext()).apply {
            this.setCancelable(false)
            this.setMessage("Please wait...")
            this.setButton(DialogInterface.BUTTON_POSITIVE, "Cancel") { dialog, _ ->
                compositeDisposable.clear()
                dialog.cancel()
            }
        }
        getIswToken(requireContext())
        return binding.root
    }

    override fun onResume() {
        super.onResume()
        getIntentDataSentInFromFirebaseService()
        repushTransactionsToBackend()
        val savedUserVirtualAccount = Prefs.getString(PREF_ZENITH_PBT_USER_ACCOUNT, "")
        userZenithPbtVirtualAccount =
            gson.fromJson(savedUserVirtualAccount, GetPayByTransferUserAccountModel::class.java)
        if (savedUserVirtualAccount.trim().isEmpty()) {
            zenithPbtViewModel.getZenithPbtUserAccount()
            val virtualAccount = Prefs.getString(PREF_ZENITH_PBT_USER_ACCOUNT, "")
            userZenithPbtVirtualAccount =
                gson.fromJson(virtualAccount, GetPayByTransferUserAccountModel::class.java)
        }
    }

    private fun setupKongaAdapter() {
        adapter = ServiceAdapter {
            when (it.id) {
                0 -> addFragmentWithoutRemove(TransactionsFragment())
                1 -> getBalance()
                2 -> addFragmentWithoutRemove(NipNotificationFragment.newInstance())
                3 -> addFragmentWithoutRemove(BillsFragment())
                4 -> showCalendarDialog()
                else -> {
                    sendPayload()
                }
            }
            // addFragmentWithoutRemove(nextFrag)
        }
        val listOfServices = arrayListOf<Service>(
            Service(0, "Transaction", R.drawable.ic_trans),
            Service(1, "Balance Inquiry", R.drawable.ic_write),
            Service(2, "Bank Transfer", R.drawable.ic_lending),
            // add(Service(3, "Pay Bills", R.drawable.ic_bill))
            Service(4, "View End Of Day Transactions", R.drawable.ic_print)
        )
        adapter.submitList(listOfServices)
    }

    private fun setUpAdapterForAellaCredit() {
        adapter = ServiceAdapter {
            when (it.id) {
                0 -> addFragmentWithoutRemove(TransactionsFragment())
                1 -> getBalance()
                2 -> {
                    if (BuildConfig.FLAVOR == "zenith") {
                        addFragmentWithoutRemove(ZenithPayByTransferFragment())
                    } else {
                        addFragmentWithoutRemove(NipNotificationFragment.newInstance())
                    }
                }
                3 -> addFragmentWithoutRemove(BillsFragment())
                4 -> showCalendarDialog()
                5 -> {
                    parentFragmentManager.beginTransaction()
                        .replace(R.id.container_main, SettingsFragment())
                        .addToBackStack(null)
                        .commit()
                }
                else -> {
                    sendPayload()
                }
            }
            // addFragmentWithoutRemove(nextFrag)
        }
        val listOfServices = ArrayList<Service>()
            .apply {
                add(Service(0, "Purchase", R.drawable.ic_trans))
//                add(Service(1, "Balance Inquiry", R.drawable.ic_write))
                if (BuildConfig.FLAVOR.equals("wemacashout", true).not()) {
                    add(
                        Service(
                            2,
                            if (BuildConfig.FLAVOR == "zenith") "Pay With Transfer" else "Bank Transfer",
                            R.drawable.ic_lending
                        )
                    )
                }
//                add(Service(3, "Pay Bills", R.drawable.ic_bill))
                add(Service(4, "View End Of Day Transactions", R.drawable.ic_print))
                add(Service(5, "Settings", R.drawable.ic_baseline_settings))
            }
        adapter.submitList(listOfServices)
    }

    private fun getIswToken(context: Context) {
        Timber.d("CALLED")
        val req = TokenPassportRequest(
            context.getString(R.string.userMD),
            Singletons.getCurrentlyLoggedInUser()!!.terminal_id!!
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
                    }
            )
            disposable.clear()
        } catch (e: Exception) {
            Timber.d("GET_ISW_TOKEN_ERROR===>%s$e")
        }
    }

    private fun setUpDefaultAdapter() {
        adapter = ServiceAdapter {
            when (it.id) {
                0 -> addFragmentWithoutRemove(TransactionsFragment())
                1 -> getBalance()
                2 -> {
                    if (BuildConfig.FLAVOR == "zenith") {
                        addFragmentWithoutRemove(ZenithPayByTransferFragment())
                    } else {
                        addFragmentWithoutRemove(NipNotificationFragment.newInstance())
                    }
                }
                3 -> addFragmentWithoutRemove(BillsFragment())
                4 -> showCalendarDialog()
                5 -> {
                    parentFragmentManager.beginTransaction()
                        .replace(R.id.container_main, SettingsFragment())
                        .addToBackStack(null)
                        .commit()
                }
                else -> {
                    sendPayload()
                }
            }
            // addFragmentWithoutRemove(nextFrag)
        }
        val listOfServices = ArrayList<Service>()
            .apply {
                add(Service(0, "Transaction", R.drawable.ic_trans))
                add(Service(1, "Balance Inquiry", R.drawable.ic_write))
                if (BuildConfig.FLAVOR.equals("wemacashout", true).not()) {
                    add(
                        Service(
                            2,
                            if (BuildConfig.FLAVOR == "zenith") "Pay With Transfer" else "Bank Transfer",
                            R.drawable.ic_lending
                        )
                    )
                }
                add(Service(3, "Pay Bills", R.drawable.ic_bill))
                add(Service(4, "View End Of Day Transactions", R.drawable.ic_print))
                add(Service(5, "Settings", R.drawable.ic_baseline_settings))
            }
        adapter.submitList(listOfServices)
    }

    private fun getBalance() {
        showCardDialog(
            requireActivity(),
            viewLifecycleOwner,
            1000,
            0L
        ).observe(viewLifecycleOwner) { event ->
            event.getContentIfNotHandled()?.let {
                it.error?.let { error ->
                    Timber.d(error)
                    Toast.makeText(requireContext(), error.localizedMessage, Toast.LENGTH_SHORT)
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
        accountType: IsoAccountType = IsoAccountType.DEFAULT_UNSPECIFIED
    ) {
        if (NetPosTerminalConfig.getKeyHolder() == null) {
            Toast.makeText(requireContext(), "Terminal not configured", Toast.LENGTH_LONG).show()
            return
        }

        val hostConfig = HostConfig(
            NetPosTerminalConfig.getTerminalId(),
            NetPosTerminalConfig.connectionData,
            NetPosTerminalConfig.getKeyHolder()!!,
            NetPosTerminalConfig.getConfigData()!!
        )
        val requestData =
            TransactionRequestData(TransactionType.BALANCE, 0L, accountType = accountType)
        progressDialog.setMessage("Checking Balance...")
        progressDialog.show()
        val processor = TransactionProcessor(hostConfig)
        // processor.
        processor.processTransaction(requireContext(), requestData, cardData)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe { response, error ->
                if (progressDialog.isShowing) {
                    progressDialog.dismiss()
                }
                error?.let {
                    it.printStackTrace()
                    Toast.makeText(
                        requireContext(),
                        "Error ${it.localizedMessage}",
                        Toast.LENGTH_SHORT
                    ).show()
                }

                response?.let {
                    if (it.responseCode == "A3") {
                        Prefs.remove(PREF_CONFIG_DATA)
                        Prefs.remove(PREF_KEYHOLDER)
                        NetPosTerminalConfig.init(
                            requireContext().applicationContext,
                            configureSilently = true
                        )
                    }

                    val messageString = if (it.isApproved) {
                        "Account Balance:\n " + it.accountBalances.joinToString("\n") { accountBalance ->
                            "${accountBalance.accountType}, ${
                            accountBalance.amount.div(100).formatCurrencyAmount()
                            }"
                        }
                    } else {
                        "${it.responseMessage}(${it.responseCode})"
                    }

                    showMessage(if (it.isApproved) "Approved" else "Declined", messageString)
                }
            }.disposeWith(compositeDisposable)
    }

    private fun showMessage(s: String, messageString: String) {
        AlertDialog.Builder(requireContext())
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
            LayoutPrintEndOfDayBinding.inflate(LayoutInflater.from(requireContext()), null, false)
        endOfDay.apply {
            approvedCount.text = approvedList.size.toString()
            declinedCount.text = declinedList.size.toString()
            totalTransactionsAmount.text =
                getString(
                    R.string.total_transaction_amount,
                    approvedList.sumOf { it.amount }.div(100).formatCurrencyAmount()
                )
            totalTransactions.text =
                getString(R.string.total_transaction_count, transactions.size.toString())
            print.setOnClickListener {
                if (transactions.isEmpty()) {
                    Toast.makeText(
                        context,
                        getString(R.string.noTransactionsToPrint),
                        Toast.LENGTH_LONG
                    ).show()
                } else {
                    when (chipGroup.checkedChipId) {
                        R.id.print_approved -> approvedList
                        R.id.print_declined -> declinedList
                        else -> transactions
                    }.apply {
                        if (isEmpty()) {
                            Toast.makeText(
                                requireContext(),
                                "No transactions to print",
                                Toast.LENGTH_SHORT
                            ).show()
                            return@setOnClickListener
                        }
                    }.printEndOfDay(requireContext())
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe({ printResp ->
                            Timber.e(printResp.toString())
                        }, { err ->
                            Toast.makeText(
                                requireContext(),
                                err.localizedMessage,
                                Toast.LENGTH_LONG
                            )
                                .show()
                            // Timber.e(err.localizedMessage)
                        }).disposeWith(CompositeDisposable())
                }
            }
        }
        val bottomSheet = BottomSheetDialog(requireContext(), R.style.SheetDialog)
            .apply {
                dismissWithAnimation = true
                setCancelable(false)
                setContentView(endOfDay.root)
                show()
            }
        endOfDay.view.setOnClickListener {
            if (transactions.isNotEmpty()) {
                transactionViewModel.setEndOfDayList(transactions)
                bottomSheet.dismiss()
                addFragmentWithoutRemove(TransactionHistoryFragment.newInstance(HISTORY_ACTION_EOD))
            } else {
                Toast.makeText(context, getString(R.string.noTransactionsToView), Toast.LENGTH_LONG)
                    .show()
            }
        }
        endOfDay.closeButton.setOnClickListener {
            bottomSheet.dismiss()
        }
    }

    private fun getEndOfDayTransactionsSecondImplementation(timestamp: Long? = null) {
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
            getDateInTheFormatExpectedByTheNewService(df.format(be)),
            getDateInTheFormatExpectedByTheNewServiceForEnd(df.format(be)),
            1,
            1000
        )
        stormApiService.getTransactionsFromNewService(
            parameters.terminalId,
            parameters.from,
            parameters.to,
            parameters.page,
            parameters.pageSize
        ).flatMap {
            it.data.rows = it.data.rows.map { transaction ->
                transaction.amount =
                    if (transaction.amount is Int) (transaction.amount as Int).times(100) else (transaction.amount as Double)
                        .times(100)
                transaction
            }
            Single.just(it)
        }.flatMap {
            if (it.data.rows.isEmpty()) {
                return@flatMap getEndOfDayLocal(
                    getDateInMilliSecsForLocal(df.format(be)),
                    getDateInMilliSecsForLocalForEndOfDay(df.format(be))
                )
            }
            Single.just(it)
        }.retry(2)
            .onErrorResumeNext {
                getEndOfDayLocal(
                    getDateInMilliSecsForLocal(df.format(be)),
                    getDateInMilliSecsForLocalForEndOfDay(df.format(be))
                )
            }
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .doFinally {
                endOfDayProgressDialog.dismiss()
            }
            .subscribe { t1, t2 ->
                t1?.let {
                    when (it) {
                        is GetEndOfDayModelFromNewServer -> {
                            showEndOfDayBottomSheetDialog(it.data.rows.mapRowToTransactionResponse())
                        }
                        is GateWayTransactionResponse -> {
                            showEndOfDayBottomSheetDialog(mapEntityToTransFromGateWay(it.result))
                        }
                        else -> {
                            showEndOfDayBottomSheetDialog(listOf<TransactionResponse>())
                        }
                    }
                }
                t2?.let {
                    Timber.d(it)
                    Toast.makeText(
                        requireContext(),
                        "An error occurred while fetching end of day, try again",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }.disposeWith(compositeDisposable)
//        val livedata = AppDatabase.getDatabaseInstance(requireContext())
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

    private fun getEndOfDayLocal(be: Long, be1: Long) =
        AppDatabase.getDatabaseInstance(requireContext())
            .transactionResponseDao()
            .getEndOfDayTransactionSingle(be, be1, NetPosTerminalConfig.getTerminalId())
            .doOnError {
                Timber.d("ERROR_HAPPENING=========>%s", it.localizedMessage)
            }
            .flatMap { transactionList ->
                Single.just(
                    GateWayTransactionResponse(
                        mapTransFromGateWayToEntity(transactionList),
                        transactionList.size,
                        1,
                        1000
                    )
                )
            }

    private fun showCalendarDialog() {
        val calendar = Calendar.getInstance()
        DatePickerDialog(
            requireContext(),
            { _, i, i2, i3 ->
                getEndOfDayTransactions(
                    Calendar.getInstance().apply { set(i, i2, i3) }.timeInMillis
                )
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    private fun sendPayload() {
        // val user = Singletons.gson.fromJson(Prefs.getString(PREF_USER, ""), User::class.java)
        val event = MqttEvent<AuthenticationEventData>()
        val authEventData =
            AuthenticationEventData(event.business_name!!, event.storm_id!!, event.deviceSerial!!)
        event.apply {
            this.event = MqttEvents.AUTHENTICATION.event
            this.status = MqttStatus.SUCCESS.name
            this.code = MqttStatus.SUCCESS.code
            timestamp = System.currentTimeMillis()
            this.geo = "lat:51.507351-long:-0.127758"
            this.data = authEventData
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        when (BuildConfig.FLAVOR) {
            "konga" -> setupKongaAdapter()
            "aellacredit" -> setUpAdapterForAellaCredit()
            else -> setUpDefaultAdapter()
        }
        binding.rvDashboard.layoutManager = GridLayoutManager(context, 2)
        binding.rvDashboard.adapter = adapter
    }

    private fun repushTransactionsToBackend() {
        val workRequest = OneTimeWorkRequestBuilder<RepushFailedTransactionToBackendWorker>()
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(
                        NetworkType.CONNECTED
                    ).build()
            ).build()
        workManager.enqueue(workRequest)
    }

    private fun getIntentDataSentInFromFirebaseService() {
        requireActivity().intent?.action?.let { intentAction ->
            requireActivity().intent.getBooleanExtra(TAG_NOTIFICATION_RECEIVED_FROM_BACKEND, false)
                .let { intentExtra ->
                    if (intentAction == STRING_FIREBASE_INTENT_ACTION) {
                        if (intentExtra) {
                            addFragmentWithoutRemove(ZenithPayByTransferTransactionPage())
                        }
                    }
                }
        }
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
            getDateInTheFormatExpectedByTheNewService(df.format(be)),
            getDateInTheFormatExpectedByTheNewServiceForEnd(df.format(be)),
            1,
            1000
        )

        getEndOfDayLocal(
            getDateInMilliSecsForLocal(df.format(be)),
            getDateInMilliSecsForLocalForEndOfDay(df.format(be))
        )
            .flatMap { gateWayResp ->
                if (gateWayResp.result.isEmpty()) {
                    return@flatMap stormApiService.getTransactionsFromNewService(
                        parameters.terminalId,
                        parameters.from,
                        parameters.to,
                        parameters.page,
                        parameters.pageSize
                    ).map {
                        val dataWithModifiedAmount = it.data.rows.map { it1 ->
                            it1.copy(amount = (it1.amount as Int * 100))
                        }
                        val modifiedData = it.data.copy(
                            rows = dataWithModifiedAmount
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
                    parameters.pageSize
                ).map {
                    val dataWithModifiedAmount = it.data.rows.map { it1 ->
                        it1.copy(amount = (it1.amount as Double * 100))
                    }
                    val modifiedData = it.data.copy(
                        rows = dataWithModifiedAmount
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
                        Timber.d("ERROR_CHECKER_1======>%s", "$it")
                        val response = gson.fromJson(gson.toJson(it), NewEodModel::class.java)
                        showEndOfDayBottomSheetDialog(response.value.data.rows.mapRowToTransactionResponse())
                    } catch (e: Exception) {
                        Timber.d("ERROR_CHECKER_2======>%s", "$it")
                        when (it) {
                            is GetEndOfDayModelFromNewServer -> {
                                Timber.d("ERROR_CHECKER_3======>%s", "$it")
                                showEndOfDayBottomSheetDialog(it.data.rows.mapRowToTransactionResponse())
                            }
                            is GateWayTransactionResponse -> {
                                Timber.d("ERROR_CHECKER_4======>%s", "$it")
                                showEndOfDayBottomSheetDialog(mapEntityToTransFromGateWay(it.result))
                            }
                            is NewEodModel -> {
                                Timber.d("ERROR_CHECKER_5======>%s", "$it")
                                showEndOfDayBottomSheetDialog(it.value.data.rows.mapRowToTransactionResponse())
                            }
                            else -> {
                                Timber.d("ERROR_CHECKER_6======>%s", "$it")
                                showEndOfDayBottomSheetDialog(listOf())
                            }
                        }
                    }
                }
                t2?.let {
                    Timber.d(it)
                    Toast.makeText(
                        requireContext(),
                        "An error occurred while fetching end of day, try again",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }.disposeWith(compositeDisposable)
//        val livedata = AppDatabase.getDatabaseInstance(requireContext())
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
}
