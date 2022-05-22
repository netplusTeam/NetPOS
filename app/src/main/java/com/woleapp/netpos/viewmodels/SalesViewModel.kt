package com.woleapp.netpos.viewmodels

import android.content.Context
import android.os.Build
import android.util.Log
import android.widget.Toast
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.danbamitale.epmslib.entities.* // ktlint-disable no-wildcard-imports
import com.danbamitale.epmslib.entities.TransactionResponse
import com.danbamitale.epmslib.entities.TransactionType
import com.danbamitale.epmslib.processors.TransactionProcessor
import com.danbamitale.epmslib.utils.IsoAccountType
import com.danbamitale.epmslib.utils.MessageReasonCode
import com.google.gson.JsonObject
import com.netpluspay.netpossdk.printer.PrinterResponse
import com.pixplicity.easyprefs.library.Prefs
import com.woleapp.netpos.BuildConfig
import com.woleapp.netpos.database.dao.TransactionResponseDao
import com.woleapp.netpos.model.* // ktlint-disable no-wildcard-imports
import com.woleapp.netpos.network.NetPOSCashService
import com.woleapp.netpos.network.StormApiClient
import com.woleapp.netpos.network.StormApiService
import com.woleapp.netpos.nibss.NetPosTerminalConfig
import com.woleapp.netpos.util.* // ktlint-disable no-wildcard-imports
import com.woleapp.netpos.util.RandomNumUtil.formattedTime
import com.woleapp.netpos.util.RandomNumUtil.generateRandomRrn
import com.woleapp.netpos.util.RandomNumUtil.getCurrentDateTime
import com.woleapp.netpos.util.RandomNumUtil.getDate
import com.woleapp.netpos.util.RandomNumUtil.mapDanbamitaleResponseToResponseX
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import retrofit2.HttpException
import timber.log.Timber
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.PrintWriter
import java.net.InetSocketAddress
import java.net.Socket

class SalesViewModelProvider(private val transactionResponseDao: TransactionResponseDao) :
    ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SalesViewModel::class.java))
            return SalesViewModel(transactionResponseDao) as T
        throw IllegalArgumentException("Cannot provide viewmodel")
    }
}

class SalesViewModel(private val transactionResponseDao: TransactionResponseDao) : ViewModel() {
    var stormApiService: StormApiService? = null
    var transResp: TransactionResponse? = null
    private var isVend: Boolean = false
    var cardData: CardData? = null
    private var netPOSCashService: NetPOSCashService = StormApiClient.getCashInstance()
    private val compositeDisposable: CompositeDisposable by lazy { CompositeDisposable() }
    val transactionState = MutableLiveData(STATE_PAYMENT_STAND_BY)
    private val lastTransactionResponse = MutableLiveData<TransactionResponse>()
    val amount: MutableLiveData<String> = MutableLiveData<String>("")
    var amountLong = 0L
    var pin = MutableLiveData("")
    val customerName = MutableLiveData("")
    val remark = MutableLiveData("")
    private var isoAccountType: IsoAccountType? = null
    private var cardScheme: String? = null
    private val _showPrintDialog = MutableLiveData<Event<String>>()
    private var amountDbl: Double = 0.0
    private val _shouldRefreshNibssKeys = MutableLiveData<Event<Boolean>>()
    val shouldRefreshNibssKeys: LiveData<Event<Boolean>>
        get() = _shouldRefreshNibssKeys
    private val _finish = MutableLiveData<Event<Boolean>>()
    private val _showPrinterError = MutableLiveData<Event<String>>()
    private var user: User?

    val showPrinterError: LiveData<Event<String>>
        get() = _showPrinterError

    val finish: LiveData<Event<Boolean>>
        get() = _finish
    private val _message: MutableLiveData<Event<String>> by lazy {
        MutableLiveData<Event<String>>()
    }
    private val _smsSent = MutableLiveData<Event<Boolean>>()
    val smsSent: LiveData<Event<Boolean>>
        get() = _smsSent

    private val _toastMessage = MutableLiveData<Event<String>>()
    val toastMessage: LiveData<Event<String>>
        get() = _toastMessage
    private val _getCardData = MutableLiveData<Event<Boolean>>()

    val showPrintDialog: LiveData<Event<String>>
        get() = _showPrintDialog

    val getCardData: LiveData<Event<Boolean>>
        get() = _getCardData

    val message: LiveData<Event<String>>
        get() = _message

    private val _showReceiptTypeMutableLiveData = MutableLiveData<Event<Boolean>>()

    init {
        stormApiService = StormApiClient.getStormApiLoginInstance()
        user = Singletons.getCurrentlyLoggedInUser()
    }

    val showReceiptType: LiveData<Event<Boolean>>
        get() = _showReceiptTypeMutableLiveData

    fun setCustomerName(name: String) {
        customerName.value = name
    }

    fun validateField() {
        amountDbl = (
            amount.value!!.toDoubleOrNull() ?: kotlin.run {
                _message.value = Event("Enter a valid amount")
                return
            }
            ) * 100
        if (BuildConfig.FLAVOR == "konga" && (remark.value.isNullOrEmpty() || remark.value!!.length < 10)) {
            _message.value = Event("Remark too short")
            return
        }
        this.amountLong = amountDbl.toLong()
        _getCardData.value = Event(true)
    }

    private fun logTransactionBeforeConnectingToNibss(dataToLog: TransactionToLogBeforeConnectingToNibbs) {
        stormApiService!!.logTransactionBeforeConnectingToNibss(dataToLog)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe { t1, t2 ->
                t1?.let {
                    Log.d("SUCCESS_SV1", "SUCCESS SAVING 1")
                }
                t2?.let {
                    Log.d("ERROR_SV1", "ERROR SAVING 1")
                }
            }.disposeWith(compositeDisposable)
    }

    private fun logTransactionAfterConnectingToNibss(
        rrn: String,
        transactionResponse: TransactionResponseX,
        status: String
    ): Single<LogToBackendResponse> {
        val dataToLog = DataToLogAfterConnectingToNibss(status, transactionResponse, rrn)
        return stormApiService!!.updateLogAfterConnectingToNibss(rrn, dataToLog)
    }

    fun makePayment(context: Context, transactionType: TransactionType = TransactionType.PURCHASE) {
        Timber.e(cardData.toString())
        val configData: ConfigData = NetPosTerminalConfig.getConfigData() ?: kotlin.run {
            _message.value =
                Event("Terminal has not been configured, restart the application to configure")
            return
        }
        val keyHolder: KeyHolder = NetPosTerminalConfig.getKeyHolder()!!
        Timber.e("terminal id for transaction ${NetPosTerminalConfig.getTerminalId()}")
        val hostConfig = HostConfig(
            NetPosTerminalConfig.getTerminalId(),
            NetPosTerminalConfig.connectionData,
            keyHolder,
            configData
        )

        val customStan = generateRandomRrn(6)
        val customRrn = generateRandomRrn(12)
        val transTime = formattedTime.replace(":", "")
        val transDateTime = getCurrentDateTime()
        println("=========TTT+Time $transDateTime")

        // IsoAccountType.
        this.amountLong = amountDbl.toLong()
        val requestData =
            TransactionRequestData(
                transactionType,
                amountLong,
                0L,
                accountType = isoAccountType!!
            )

        val transactionToLog = cardData?.expiryDate?.let {
            customerName.value?.let { it1 ->
                user?.netplus_id?.let { it2 ->
                    val newAmount = amount.value!!.toDoubleOrNull()
                    TransactionToLogBeforeConnectingToNibbs(
                        status = "PENDING",
                        TransactionResponseX(
                            AID = "",
                            rrn = customRrn,
                            STAN = customStan,
                            TSI = "",
                            TVR = "",
                            accountType = isoAccountType!!.name,
                            acquiringInstCode = "",
                            additionalAmount_54 = "",
                            amount = newAmount?.toInt() ?: amount.value!!.toInt(),
                            appCryptogram = "",
                            authCode = "",
                            cardExpiry = it,
                            cardHolder = it1,
                            cardLabel = cardScheme.toString(),
                            id = 0,
                            localDate_13 = getDate(),
                            localTime_12 = transTime,
                            maskedPan = cardData!!.pan,
                            merchantId = it2,
                            originalForwardingInstCode = "",
                            otherAmount = requestData.otherAmount.toInt(),
                            otherId = "",
                            responseCode = "99",
                            responseDE55 = "",
                            terminalId = user!!.terminal_id!!,
                            transactionTimeInMillis = 0,
                            transactionType = requestData.transactionType.name,
                            transmissionDateTime = transDateTime
                        )
                    )
                }
            }
        }

        // Send to backend first
        logTransactionBeforeConnectingToNibss(transactionToLog!!)
        val processor = TransactionProcessor(hostConfig)
        transactionState.value = STATE_PAYMENT_STARTED
        processor.processTransaction(context, requestData, cardData!!)
            .onErrorResumeNext {
                processor.rollback(context, MessageReasonCode.Timeout)
            }
            .flatMap {
                transResp = it
                if (it.responseCode == "A3") {
                    Prefs.remove(PREF_CONFIG_DATA)
                    Prefs.remove(PREF_KEYHOLDER)
                    _shouldRefreshNibssKeys.postValue(Event(true))
                }

                it.cardHolder = customerName.value!!
                it.cardLabel = cardScheme!!
                it.amount = requestData.amount
                lastTransactionResponse.postValue(it)
                Timber.e(it.toString())
                Timber.e(it.responseCode)
                Timber.e(it.responseMessage)
                _message.postValue(Event(if (it.responseCode == "00") "Transaction Approved" else "Transaction Not approved"))
                transactionResponseDao
                    .insertNewTransaction(it)
            }.flatMap {
                val resp = lastTransactionResponse.value!!
                if (resp.responseCode == "00") {
                    Log.d("dataToSave", Singletons.gson.toJson(it))
                    logTransactionAfterConnectingToNibss(
                        customRrn,
                        mapDanbamitaleResponseToResponseX(resp),
                        "APPROVED"
                    )
                } else {
                    logTransactionAfterConnectingToNibss(
                        rrn = customRrn,
                        transactionResponse = mapDanbamitaleResponseToResponseX(resp),
                        status = resp.responseMessage
                    )
                }
            }
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .doFinally {
                transactionState.value = STATE_PAYMENT_STAND_BY
                printReceipt(context)
            }.subscribe { t1, throwable ->
                t1?.let {
                    // _finish.value = Event(true)
                }
                throwable?.let {
                    _message.value = Event("Error: ${it.localizedMessage}")
                    Timber.e(it)
                }
            }.disposeWith(compositeDisposable)
    }

    override fun onCleared() {
        super.onCleared()
        compositeDisposable.clear()
    }

    fun setAccountType(accountType: IsoAccountType) {
        this.isoAccountType = accountType
    }

    fun setCardScheme(cardScheme: String?) {
        this.cardScheme = if (cardScheme.equals("no match", true)) "VERVE" else cardScheme
    }

    fun showReceiptDialog() {
        _showPrintDialog.value = Event(
            lastTransactionResponse.value!!.buildSMSText(remark.value ?: "")
                .toString()
        )
    }

    private fun printReceipt(context: Context) {
        val transactionResponse = lastTransactionResponse.value ?: gatewayErrorTransactionResponse(
            amountLong,
            TransactionType.PURCHASE,
            isoAccountType!!
        ).apply {
            this.cardExpiry = ""
            this.cardHolder = customerName.value ?: ""
        }
        Log.d("MNAME2", Singletons.getCurrentlyLoggedInUser()!!.business_name ?: "Null")

        if (Build.MODEL.equals("Pro", true) || Build.MODEL.equals("P3", true)) {
            when (Prefs.getString(PREF_PRINTER_SETTINGS, PREF_VALUE_PRINT_CUSTOMER_COPY_ONLY)) {
                PREF_VALUE_PRINT_CUSTOMER_COPY_ONLY -> printReceipt(context, isMerchantCopy = false)
                PREF_VALUE_PRINT_CUSTOMER_AND_MERCHANT_COPY -> printReceipt(
                    context, printBoth = true
                )
                PREF_VALUE_PRINT_SMS -> _showPrintDialog.postValue(
                    Event(transactionResponse.buildSMSText(remark.value ?: "").toString())
                )
                PREF_VALUE_PRINT_ASK_BEFORE_PRINTING -> _showReceiptTypeMutableLiveData.postValue(
                    Event(true)
                )
            }
        } else
            _showPrintDialog.postValue(
                Event(transactionResponse.buildSMSText(remark.value ?: "").toString())
            )
    }

    fun printReceipt(
        context: Context,
        isMerchantCopy: Boolean = false,
        printBoth: Boolean = false,
        selected: Boolean = false
    ) {
        lastTransactionResponse.value?.print(
            context, remark = remark.value ?: "",
            isMerchantCopy = isMerchantCopy
        )
            ?.subscribeOn(Schedulers.io())?.observeOn(AndroidSchedulers.mainThread())
            ?.subscribe { t1, t2 ->
                t1?.let {
                    if (printBoth) {
                        if (isMerchantCopy) {
                            finish()
                        } else {
                            printReceipt(context, isMerchantCopy = true, printBoth = true)
                        }
                    } else {
                        if (selected.not())
                            finish()
                    }
                }
                t2?.let {
                    // MqttHelper.sendPayload(MqttTopics.PRINTING_RECEIPT, printerEvent)
                    _showPrinterError.value = Event(it.localizedMessage ?: "Unknown printer error")
                    _message.value = Event("Error: ${it.localizedMessage}")
                    Timber.e(it)
                }
            }?.disposeWith(compositeDisposable)
    }

    fun printReceipt2(context: Context) {
        val transactionResponse = lastTransactionResponse.value!!
        if (Build.MODEL == "P3" || Build.MODEL.equals(
                "PRO",
                true
            )
        ) transactionResponse.print(context, remark.value ?: "")
            .subscribeOn(Schedulers.io()) else {
            _showPrintDialog.postValue(
                Event(
                    transactionResponse.buildSMSText(remark.value ?: "").toString()
                )
            )
            Single.just(PrinterResponse())
        }.subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe { t1, t2 ->
                t1?.let {
                    //                    event.apply {
//                        this.event = MqttEvents.PRINTING_RECEIPT.event
//                        this.code = it.code.toString()
//                        this.timestamp = System.currentTimeMillis()
//                        this.data = PrinterEventData(
//                            lastTransactionResponse.value!!.RRN,
//                            "Printer code name"
//                        )
//                        this.status = it.message
//                    }
//                    MqttHelper.sendPayload(MqttTopics.PRINTING_RECEIPT, event)
                    // _message.value = Event("")
                }
                t2?.let {
                    _showPrinterError.value = Event(it.localizedMessage ?: "")
                }
            }.disposeWith(compositeDisposable)
    }

    fun finish() {
        _finish.value = Event(true)
    }

    fun sendSmS(number: String) {
        val map = JsonObject().apply {
            addProperty("from", "NetPlus")
            addProperty("to", "+234${number.substring(1)}")
            addProperty(
                "message",
                lastTransactionResponse.value!!.buildSMSText(remark.value ?: "").toString()
            )
        }
        Timber.e("payload: $map")
        val auth = "Bearer ${Prefs.getString(PREF_APP_TOKEN, "")}"
        val body: RequestBody = map.toString()
            .toRequestBody("application/json; charset=utf-8".toMediaTypeOrNull())

        StormApiClient.getSmsServiceInstance().sendSms(auth, body)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe { t1, t2 ->
                t1?.let {
                    _smsSent.value = Event(true)
                    Timber.e("Data $it")
                }
                t2?.let {
                    val httpException = it as? HttpException
                    httpException?.let { e ->
                    }
                    _smsSent.value = Event(false)
                    _toastMessage.value = Event("Error: ${it.localizedMessage}")
                }
            }.disposeWith(compositeDisposable)
    }

    fun isVend(vend: Boolean) {
        isVend = vend
    }

    private fun sendVendResponse(context: Context, out: String) {
        Single.fromCallable {
            Socket().run {
                soTimeout = 120_000
                connect(InetSocketAddress(VEND_PROD_IP, VEND_PROD_PORT))
                val reader = BufferedReader(InputStreamReader(getInputStream()))
                Timber.e(reader.readLine())
                val printWriter = PrintWriter(getOutputStream(), true)
                printWriter.println(out)
                reader.readLine()
            }
        }.subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .doFinally {
            }
            .subscribe { t1, t2 ->
                t1?.let {
                    Timber.e(it)
                    // Toast.makeText(context, "received", Toast.LENGTH_SHORT).show()
                }
                t2?.let {
                    Toast.makeText(context, it.localizedMessage, Toast.LENGTH_SHORT).show()
                    Timber.e(it)
                }
            }.disposeWith(compositeDisposable)
    }

    private val _cashTransactionCompleted = MutableLiveData<Event<Boolean>>()
    val cashTransactionCompleted: LiveData<Event<Boolean>> = _cashTransactionCompleted

    fun beginCashPayment() {
        (
            amount.value!!.toDoubleOrNull() ?: kotlin.run {
                _message.value = Event("Enter a valid amount")
                return
            }
            )
        val reqBody = JsonObject().apply {
            addProperty("amount", amount.value!!.toDouble())
        }
        transactionState.value = STATE_PAYMENT_STARTED
        netPOSCashService.addCashTransaction(reqBody)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .doFinally {
                transactionState.value = STATE_PAYMENT_STAND_BY
            }
            .subscribe { t1, t2 ->
                t1?.let {
                    _message.value = Event("Payment completed")
                    _cashTransactionCompleted.value = Event(true)
                }
                t2?.let {
                    _message.value = Event("payment failed")
                    Timber.e(it)
                }
            }.disposeWith(compositeDisposable)
    }
}
