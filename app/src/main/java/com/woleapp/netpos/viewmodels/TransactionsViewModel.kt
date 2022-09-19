package com.woleapp.netpos.viewmodels

import android.content.Context
import android.os.Build
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.paging.LivePagedListBuilder
import androidx.paging.PagedList
import com.danbamitale.epmslib.entities.*
import com.danbamitale.epmslib.processors.TransactionProcessor
import com.danbamitale.epmslib.utils.IsoAccountType
import com.netpluspay.netpossdk.printer.PrinterResponse
import com.pixplicity.easyprefs.library.Prefs
import com.woleapp.netpos.database.AppDatabase
import com.woleapp.netpos.database.TransactionBoundaryCallBack
import com.woleapp.netpos.model.GetEodFromNewServiceModel
import com.woleapp.netpos.model.User
import com.woleapp.netpos.network.StormApiClient
import com.woleapp.netpos.nibss.NetPosTerminalConfig
import com.woleapp.netpos.util.*
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import timber.log.Timber

class TransactionsViewModel(private val appDatabase: AppDatabase) : ViewModel() {
    private lateinit var endOfDayList: List<TransactionResponse>
    var cardData: CardData? = null
    private val compositeDisposable = CompositeDisposable()
    val lastTransactionResponse = MutableLiveData<TransactionResponse>()
    private val _selectedAction = MutableLiveData<String>()
    val inProgress = MutableLiveData(false)
    private val _done = MutableLiveData(false)
    private val _beginGetCardDetails = MutableLiveData<Event<Boolean>>()
    private var accountType: IsoAccountType = IsoAccountType.DEFAULT_UNSPECIFIED
    private lateinit var cardHolderName: String
    private val _message = MutableLiveData<Event<String>>()
    private var cardScheme: String? = null
    private val _showProgressDialog = MutableLiveData<Event<Boolean>>()
    private val _showPrintDialog = MutableLiveData<Event<String>>()
    private var user: User?

    private val _showPrinterError = MutableLiveData<Event<String>>()

    val showPrinterError: LiveData<Event<String>>
        get() = _showPrinterError

    private val _showReceiptTypeMutableLiveData = MutableLiveData<Event<Boolean>>()

    val showReceiptType: LiveData<Event<Boolean>>
        get() = _showReceiptTypeMutableLiveData

    private val _shouldRefreshNibssKeys = MutableLiveData<Event<Boolean>>()
    val shouldRefreshNibssKeys: LiveData<Event<Boolean>>
        get() = _shouldRefreshNibssKeys

    private val _smsSent = MutableLiveData<Event<Boolean>>()
    val smsSent: LiveData<Event<Boolean>>
        get() = _smsSent

    private val _toastMessage = MutableLiveData<Event<String>>()
    val toastMessage: LiveData<Event<String>>
        get() = _toastMessage

    val showPrintDialog: LiveData<Event<String>>
        get() = _showPrintDialog

    val showProgressDialog: LiveData<Event<Boolean>>
        get() = _showProgressDialog

    val message: LiveData<Event<String>>
        get() = _message
    val beginGetCardDetails: LiveData<Event<Boolean>>
        get() = _beginGetCardDetails

    val done: LiveData<Boolean>
        get() = _done
    val selectedAction: LiveData<String>
        get() = _selectedAction

    val pagedTransaction: LiveData<PagedList<TransactionResponse>>

    init {
        val initialParams =
            GetEodFromNewServiceModel(
                terminalId = NetPosTerminalConfig.getTerminalId(),
                from = "",
                to = "",
                page = 1,
                pageSize = 20
            )
        user = Singletons.getCurrentlyLoggedInUser()
        val config = PagedList.Config.Builder()
            .setPageSize(20)
            .setEnablePlaceholders(false)
            .build()
        val transactionBoundaryCallBack = TransactionBoundaryCallBack(
            HashMap<String, String>().apply {
                put("terminalId", NetPosTerminalConfig.getTerminalId())
            },
            initialParams,
            StormApiClient.getStormApiLoginInstance(),
            appDatabase.transactionResponseDao()
        )

        pagedTransaction = LivePagedListBuilder(
            appDatabase.transactionResponseDao()
                .getTransactions(NetPosTerminalConfig.getTerminalId()),
            config
        ).setBoundaryCallback(transactionBoundaryCallBack)
            .build()
    }

    fun setSelectedTransaction(transactionResponse: TransactionResponse) {
        lastTransactionResponse.value = transactionResponse
    }

    fun insertIntoDatabase(transactionResponse: List<TransactionResponse>) {
        compositeDisposable.add(
            appDatabase.transactionResponseDao().insertNewTransaction(transactionResponse)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe { t1, t2 ->
                    t1?.let {
                        Timber.d("size" + it.size)
                    }
                    t2?.let {
                        Timber.d(it.localizedMessage)
                    }
                }
        )
    }

//    fun getTransactions() =
//        when (_selectedAction.value) {
//            HISTORY_ACTION_PREAUTH -> appDatabase!!.transactionResponseDao()
//                .getTransactionByTransactionType(TransactionType.PRE_AUTHORIZATION)
//            HISTORY_ACTION_REFUND -> appDatabase!!.transactionResponseDao()
//                .getRefundableTransactions()
//            else -> appDatabase!!.transactionResponseDao()
//                .getTransactions(NetPosTerminalConfig.getTerminalId())
//        }

    fun setAction(action: String?) {
        _selectedAction.value = action!!
    }

    fun performAction(context: Context) {
        when (_selectedAction.value) {
            HISTORY_ACTION_REPRINT -> printReceipt(context)
            HISTORY_ACTION_REFUND -> {
                _beginGetCardDetails.value = Event(true)
            }
        }
    }

    fun refundTransaction(context: Context) {
        refundTransaction(lastTransactionResponse.value!!, context)
    }

    fun reset() {
        _done.value = false
    }

    private fun refundTransaction(transactionResponse: TransactionResponse, context: Context) {
        val originalDataElements = transactionResponse.toOriginalDataElements()

        val hostConfig = HostConfig(
            NetPosTerminalConfig.getTerminalId(),
            NetPosTerminalConfig.connectionData,
            NetPosTerminalConfig.getKeyHolder()!!,
            NetPosTerminalConfig.getConfigData()!!
        )

        val requestData = TransactionRequestData(
            transactionType = TransactionType.REVERSAL,
            amount = originalDataElements.originalAmount,
            originalDataElements = originalDataElements,
            accountType = accountType
        )
        inProgress.value = true
        TransactionProcessor(hostConfig).processTransaction(
            context,
            requestData,
            cardData!!
        ).flatMap {
            if (it.responseCode == "A3") {
                _shouldRefreshNibssKeys.postValue(Event(true))
            }
            _message.postValue(Event("Transaction: ${it.responseMessage}"))
            it.cardHolder = cardHolderName
            it.cardLabel = cardScheme!!
            it.id = transactionResponse.id
            lastTransactionResponse.postValue(it)
            appDatabase.transactionResponseDao().updateTransaction(it)
        }
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe { response, error ->
                error?.let {
                    inProgress.value = false
                    _message.value = Event(it.localizedMessage ?: "")
                    Timber.e(it)
                    it.printStackTrace()
                }
                response?.let {
                    startPrintingReceipt(context)
                }
            }.disposeWith(compositeDisposable)
    }

    private fun printReceipt2(
        context: Context
    ): Single<PrinterResponse> {
        return if (Build.MODEL == "P3" && lastTransactionResponse.value != null) lastTransactionResponse.value!!.print(
            context
        )
        else {
            _showPrintDialog.postValue(
                Event(
                    lastTransactionResponse.value?.buildSMSText().toString()
                )
            )
            Single.just(PrinterResponse())
        }
    }

    private fun printReceipt(context: Context) {
        val transactionResponse = lastTransactionResponse.value!!
            .apply {
                this.cardExpiry = ""
            }

        if (Build.MODEL.equals("Pro", true) || Build.MODEL.equals("P3", true)) {
            when (Prefs.getString(PREF_PRINTER_SETTINGS, PREF_VALUE_PRINT_CUSTOMER_COPY_ONLY)) {
                PREF_VALUE_PRINT_CUSTOMER_COPY_ONLY -> startPrintingReceipt(
                    context,
                    isMerchantCopy = false
                )
                PREF_VALUE_PRINT_CUSTOMER_AND_MERCHANT_COPY -> startPrintingReceipt(
                    context,
                    printBoth = true
                )
                PREF_VALUE_PRINT_SMS -> _showPrintDialog.postValue(
                    Event(transactionResponse.buildSMSText().toString())
                )
                PREF_VALUE_PRINT_ASK_BEFORE_PRINTING -> _showReceiptTypeMutableLiveData.postValue(
                    Event(true)
                )
            }
        } else {
            _showPrintDialog.postValue(
                Event(transactionResponse.buildSMSText().toString())
            )
        }

//        if (Build.MODEL.equals("Pro", true) || Build.MODEL.equals(
//                "P3",
//                true
//            )
//        ) transactionResponse.print(context, remark.value ?: "")
//            .subscribeOn(Schedulers.io()) else {
//            _showPrintDialog.postValue(
//                Event(
//                    transactionResponse.buildSMSText(remark.value ?: "").toString()
//                )
//            )
//            Single.just(PrinterResponse(0, "SMS"))
//        }.subscribeOn(Schedulers.io())
//            .observeOn(AndroidSchedulers.mainThread())
//            .subscribe { t1, t2 ->
//
//            }
//            .disposeWith(compositeDisposable)
    }

    fun startPrintingReceipt(
        context: Context,
        isMerchantCopy: Boolean = false,
        printBoth: Boolean = false
    ) {
        inProgress.value = true
        val transactionResponse = lastTransactionResponse.value
        transactionResponse?.apply {
            this.cardExpiry = ""
            // this.cardHolder = this
        }
        transactionResponse?.print(context, isMerchantCopy = isMerchantCopy, isReprint = true)
            ?.subscribeOn(Schedulers.io())
            ?.observeOn(AndroidSchedulers.mainThread())
            ?.subscribe { t1, t2 ->
                t1?.let {
                    if (printBoth) {
                        if (isMerchantCopy) {
                            _done.value = true
                            inProgress.value = false
                        } else {
                            startPrintingReceipt(context, isMerchantCopy = true, printBoth = true)
                        }
                    } else {
                        _done.value = true
                        inProgress.value = false
                    }
                }
                t2?.let {
                    _done.value = true
                    inProgress.value = false
                    _showPrinterError.value = Event(it.localizedMessage ?: "Error")
                    Timber.e(it)
                    _message.value = Event(it.localizedMessage ?: "Error")
                }
                // MqttHelper.sendPayload(MqttTopics.PRINTING_RECEIPT, printerEvent)
            }?.disposeWith(compositeDisposable)
    }

//    fun startPrintingReceipt2(
//        context: Context
//    ) {
//        inProgress.value = true
//        printReceipt(context)
//            .subscribeOn(Schedulers.io())
//            .observeOn(AndroidSchedulers.mainThread())
//            .subscribe { t1, t2 ->
//                t1?.let {
//                    event.apply {
//                        this.event = MqttEvents.PRINTING_RECEIPT.event
//                        this.code = it.code.toString()
//                        this.timestamp = System.currentTimeMillis()
//                        this.data =
//                            lastTransactionResponse.value?.let { it1 ->
//                                PrinterEventData(
//                                    it1.RRN,
//                                    it.message
//                                )
//                            }
//                        this.status = it.message
//                    }
//                    MqttHelper.sendPayload(MqttTopics.PRINTING_RECEIPT, event)
//                }
//                _done.value = true
//                inProgress.value = false
//
//                t2?.let {
//                    _showPrinterError.value = Event(it.localizedMessage ?: "")
//                    Timber.e(it)
//                    _message.value = Event(it.localizedMessage ?: "")
//                }
//            }?.disposeWith(compositeDisposable)
//    }

    fun showReceiptDialog() {
        _showPrintDialog.value = Event(
            lastTransactionResponse.value!!.buildSMSText()
                .toString()
        )
    }

    fun setCustomerName(cardHolderName: String) {
        this.cardHolderName = cardHolderName
    }

    fun setAccountType(accountType: IsoAccountType) {
        this.accountType = accountType
    }

    fun setCardScheme(cardScheme: String?) {
        this.cardScheme = if (cardScheme.equals("no match", true)) "VERVE" else cardScheme
    }

    fun doSaleCompletion(context: Context) {
        val transactionResponse = lastTransactionResponse.value!!
        val originalDataElements = transactionResponse.toOriginalDataElements()
        val hostConfig = HostConfig(
            NetPosTerminalConfig.getTerminalId(),
            NetPosTerminalConfig.connectionData,
            NetPosTerminalConfig.getKeyHolder()!!,
            NetPosTerminalConfig.getConfigData()!!
        )

        val requestData = TransactionRequestData(
            transactionType = TransactionType.PRE_AUTHORIZATION_COMPLETION,
            amount = originalDataElements.originalAmount,
            originalDataElements = originalDataElements
        )

        _showProgressDialog.value = Event(true)
        TransactionProcessor(hostConfig).processTransaction(
            context,
            requestData,
            cardData!!
        ).flatMap {
            if (it.responseCode == "A3") {
                _shouldRefreshNibssKeys.postValue(Event(true))
            }
            _showProgressDialog.postValue(Event(false))
            _message.postValue(Event("Transaction: ${it.responseMessage}"))
            it.cardHolder = cardHolderName
            it.cardLabel = cardScheme!!
            it.id = transactionResponse.id
            lastTransactionResponse.postValue(it)
            appDatabase.transactionResponseDao().updateTransaction(it)
        }.subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe { response, error ->
                error?.let {
                    _message.value = Event(it.localizedMessage)
                    Timber.e(it)
                    it.printStackTrace()
                }

                response?.let {
                    startPrintingReceipt(context)
                }
            }.disposeWith(compositeDisposable)
    }

    fun preAuthRefund(context: Context) {
        val transactionResponse = lastTransactionResponse.value!!
        val originalDataElements = transactionResponse.toOriginalDataElements()

        val hostConfig = HostConfig(
            NetPosTerminalConfig.getTerminalId(),
            NetPosTerminalConfig.connectionData,
            NetPosTerminalConfig.getKeyHolder()!!,
            NetPosTerminalConfig.getConfigData()!!
        )

        val requestData = TransactionRequestData(
            transactionType = TransactionType.REFUND,
            amount = originalDataElements.originalAmount,
            originalDataElements = originalDataElements
        )
        _showProgressDialog.value = Event(true)
        TransactionProcessor(hostConfig).processTransaction(context, requestData, cardData!!)
            .flatMap {
                if (it.responseCode == "A3") {
                    _shouldRefreshNibssKeys.postValue(Event(true))
                }
                _showProgressDialog.postValue(Event(false))
                _message.postValue(Event("Transaction: ${it.responseMessage}"))
                it.cardHolder = cardHolderName
                it.cardLabel = cardScheme!!
                it.id = transactionResponse.id
                lastTransactionResponse.postValue(it)
                appDatabase!!.transactionResponseDao().updateTransaction(it)
            }.subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe { response, error ->
                error?.let {
                    _message.value = Event(it.localizedMessage)
                    Timber.e(it)
                    it.printStackTrace()
                }

                response?.let {
                    startPrintingReceipt(context)
                }
            }.disposeWith(compositeDisposable)
    }

    fun sendSmS(number: String) {
        sendSmS(
            lastTransactionResponse.value!!,
            number,
            _smsSent,
            _message,
            compositeDisposable
        )
    }

    fun setEndOfDayList(eodList: List<TransactionResponse>) {
        this.endOfDayList = eodList
        Log.d("SIZESIZE", eodList.size.toString())
        eodList.forEach {
            println("ANOTHER_V" + it.localDate_13)
            println("ANOTHER_V" + it.transmissionDateTime)
            println("ANOTHER_V" + it.transactionTimeInMillis.toString())
            println("ANOTHER_V" + it.localTime_12)
            print("============================================\n")
        }
    }

    fun getEodList() = endOfDayList
}
