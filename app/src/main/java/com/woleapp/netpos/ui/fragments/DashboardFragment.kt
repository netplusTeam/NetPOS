@file:Suppress("DEPRECATION")

package com.woleapp.netpos.ui.fragments

import android.app.AlertDialog
import android.app.DatePickerDialog
import android.app.ProgressDialog
import android.content.DialogInterface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.GridLayoutManager
import com.danbamitale.epmslib.entities.* // ktlint-disable no-wildcard-imports
import com.danbamitale.epmslib.extensions.formatCurrencyAmount
import com.danbamitale.epmslib.processors.TransactionProcessor
import com.danbamitale.epmslib.utils.IsoAccountType
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.pixplicity.easyprefs.library.Prefs
import com.woleapp.netpos.BuildConfig
import com.woleapp.netpos.R
import com.woleapp.netpos.adapter.ServiceAdapter
import com.woleapp.netpos.database.AppDatabase
import com.woleapp.netpos.databinding.FragmentDashboardBinding
import com.woleapp.netpos.databinding.LayoutPrintEndOfDayBinding
import com.woleapp.netpos.model.* // ktlint-disable no-wildcard-imports
import com.woleapp.netpos.network.NetPOSGatewayApi
import com.woleapp.netpos.network.StormApiClient
import com.woleapp.netpos.nibss.NetPosTerminalConfig
import com.woleapp.netpos.util.* // ktlint-disable no-wildcard-imports
import com.woleapp.netpos.util.ModelMapper.mapRowToTransactionResponse
import com.woleapp.netpos.util.ModelMapper.mapTransFromGateWayToEntity
import com.woleapp.netpos.util.RandomNumUtil.getDateInMilliSecsForLocal
import com.woleapp.netpos.util.RandomNumUtil.getDateInMilliSecsForLocalForEndOfDay
import com.woleapp.netpos.util.RandomNumUtil.getDateInTheFormatExpectedByTheNewService
import com.woleapp.netpos.util.RandomNumUtil.getDateInTheFormatExpectedByTheNewServiceForEnd
import com.woleapp.netpos.viewmodels.NetPosViewModelFactories
import com.woleapp.netpos.viewmodels.TransactionsViewModel
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import timber.log.Timber
import java.sql.Timestamp
import java.text.SimpleDateFormat
import java.time.Instant
import java.util.* // ktlint-disable no-wildcard-imports

class DashboardFragment : BaseFragment() {

    private lateinit var progressDialog: ProgressDialog
    private lateinit var binding: FragmentDashboardBinding
    private lateinit var adapter: ServiceAdapter
    private var compositeDisposable = CompositeDisposable()
    private val gateWayService = NetPOSGatewayApi.getInstance()
    private val stormApiService = StormApiClient.getStormApiLoginInstance()
    private lateinit var endOfDayProgressDialog: ProgressDialog
    private val transactionViewModel by activityViewModels<TransactionsViewModel> {
        NetPosViewModelFactories(AppDatabase.getDatabaseInstance(requireContext()))
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
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
        return binding.root
    }

    override fun onResume() {
        super.onResume()
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

    private fun setUpDefaultAdapter() {
        adapter = ServiceAdapter {
            when (it.id) {
                0 -> addFragmentWithoutRemove(TransactionsFragment())
                1 -> getBalance()
                2 -> {
                    if (BuildConfig.FLAVOR == "zenith")
                        showPayWithTransferDialog(requireContext())
                    else
                        addFragmentWithoutRemove(NipNotificationFragment.newInstance())
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
                if (BuildConfig.FLAVOR.equals("wemacashout", true).not())
                    add(
                        Service(
                            2,
                            if (BuildConfig.FLAVOR == "zenith") "Pay With Transfer" else "Bank Transfer",
                            R.drawable.ic_lending
                        )
                    )
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
        val disposable = processor.processTransaction(requireContext(), requestData, cardData)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe { response, error ->
                if (progressDialog.isShowing)
                    progressDialog.dismiss()
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
            }
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

    private fun getEndOfDayTransactions(timestamp: Long? = null) {
        endOfDayProgressDialog.show()
        val be: Long = getBeginningOfDay(timestamp)
        val be1: Long = Timestamp.from(Instant.ofEpochMilli(be).plusSeconds(86400)).time
        Timber.e("DISCOVER2%s", be.toString())
        Timber.e("DISCOVER3%s", be1.toString())
        val df = SimpleDateFormat("dd:MM:yyyy hh:mm:ss", Locale.getDefault())
        Timber.e("DISCOVER4%s", df.format(be))
        Timber.e("DISCOVER5%s", df.format(be1))

        val data = HashMap<String, String>().apply {
            put("terminalId", NetPosTerminalConfig.getTerminalId())
            put("count", "1000")
            put("from", df.format(be))
            put("to", df.format(be1))
        }
        Timber.d(df.format(be))
        Timber.d(df.format(be1))

        val parameters = GetEodFromNewServiceModel(
            NetPosTerminalConfig.getTerminalId(),
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

            Timber.d("ISOK==>" + it.data.rows.mapRowToTransactionResponse().toString())
            println("====CHECKING_PAYLOAD" + it.data.rows.toString())
            Timber.d(it.data.rows.toString())

            it.data.rows = it.data.rows.map { transaction ->
                transaction.amount = transaction.amount.times(100)
                transaction
            }
            Single.just(it)
        }.flatMap {
            if (it.data.rows.isEmpty())
                return@flatMap getEndOfDayLocal(
                    getDateInMilliSecsForLocal(df.format(be)),
                    getDateInMilliSecsForLocalForEndOfDay(df.format(be))
                )
            Single.just(it)
        }.retry(2)
            .onErrorResumeNext {
                Timber.e("error resume next ${it.localizedMessage}")
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
                    Timber.e("DISCOVER61" + it.toString())
                    if (it is GetEndOfDayModelFromNewServer) {
                        Timber.e("DISCOVER6%s", it.data.count.toString())
                        showEndOfDayBottomSheetDialog(it.data.rows.mapRowToTransactionResponse())
                    } else {
                        showEndOfDayBottomSheetDialog(listOf<TransactionResponse>())
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
            .flatMap { transactionList ->
                Timber.d("NN_TIME1" + be.toString())
                Timber.d("NN_TIME2" + be1.toString())
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
        // MqttHelper.sendPayload(MqttTopics.AUTHENTICATION, event)
        // Timber.e(Singletons.gson.toJson(event))
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        when (BuildConfig.FLAVOR) {
            "konga" -> setupKongaAdapter()
            else -> setUpDefaultAdapter()
        }
        binding.rvDashboard.layoutManager = GridLayoutManager(context, 2)
        binding.rvDashboard.adapter = adapter
    }
}
