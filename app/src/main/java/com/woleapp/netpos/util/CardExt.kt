@file:Suppress("DEPRECATION")

package com.woleapp.netpos.util

import android.app.Activity
import android.app.AlertDialog
import android.app.ProgressDialog
import android.content.*
import android.os.Bundle
import android.os.RemoteException
import android.util.Log
import android.view.LayoutInflater
import android.widget.Toast
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import com.danbamitale.epmslib.entities.CardData
import com.danbamitale.epmslib.utils.IsoAccountType
import com.horizonpay.smartpossdk.aidl.emv.AidlCheckCardListener
import com.horizonpay.smartpossdk.aidl.emv.AidlEmvStartListener
import com.horizonpay.smartpossdk.aidl.emv.CandidateAID
import com.horizonpay.smartpossdk.aidl.emv.EmvFinalSelectData
import com.horizonpay.smartpossdk.aidl.emv.EmvTransData
import com.horizonpay.smartpossdk.aidl.emv.EmvTransOutputData
import com.horizonpay.smartpossdk.aidl.magcard.TrackData
import com.horizonpay.smartpossdk.aidl.pinpad.AidlPinPadInputListener
import com.horizonpay.smartpossdk.data.EmvConstant
import com.horizonpay.smartpossdk.data.PinpadConst
import com.horizonpay.smartpossdk.data.PinpadConst.PinAlgorithmMode.ISO9564FMT1
import com.horizonpay.utils.ConvertUtils
import com.netpluspay.netpossdk.emv.CardReadResult
import com.netpluspay.netpossdk.emv.CardReaderEvent
import com.netpluspay.netpossdk.emv.CardReaderService
import com.pos.sdk.emvcore.POIEmvCoreManager.DEV_ICC
import com.pos.sdk.emvcore.POIEmvCoreManager.DEV_PICC
import com.pos.sdk.security.POIHsmManage
import com.woleapp.netpos.R
import com.woleapp.netpos.app.DeviceHelper
import com.woleapp.netpos.databinding.DialogSelectAccountTypeBinding
import com.woleapp.netpos.nibss.NetPosTerminalConfig
import com.woleapp.netpos.util.horizonpay.Hex
import com.woleapp.netpos.util.horizonpay.HexUtil
import com.woleapp.netpos.util.horizonpay.SoftwareDukpt
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import timber.log.Timber
import java.util.Locale

data class ICCCardHelper(
    val cardReadResult: CardReadResult? = null,
    val customerName: String? = null,
    val cardScheme: String? = null,
    var accountType: IsoAccountType? = null,
    val cardData: CardData? = null,
    val error: Throwable? = null
)

private var k11ActiveDialog: ProgressDialog? = null
private var k11IsRunning = false

private fun panForIso9564PinBlockFromEmv(emvL2: Any?): String? {
    // We keep this defensive because K11 tag reads can be flaky across kernels/cards.
    // Preferred: PAN from tag 5A (Application PAN). Fallback: from tag 57 (Track 2 Equivalent).
    val emv = emvL2 as? com.horizonpay.smartpossdk.aidl.emv.IAidlEmvL2 ?: return null
    val panFrom5A = emv.getTagValue("5A")?.replace("F", "")?.trim().orEmpty()
    val pan =
        when {
            panFrom5A.length >= 13 -> panFrom5A
            else -> {
                val t2 = emv.getTagValue("57")?.replace("F", "")?.trim().orEmpty()
                val sepIdx = t2.indexOfAny(charArrayOf('D', '='))
                if (sepIdx >= 13) t2.substring(0, sepIdx) else ""
            }
        }

    // For HorizonPay K11, use the full PAN for PIN block encryption
    if (pan.length < 12) return null
    return pan
}

//fun showCardDialog(
//    context: Activity,
//    lifecycleOwner: LifecycleOwner,
//    amount: Long,
//    cashBackAmount: Long,
//    compositeDisposable: CompositeDisposable? = null
//): LiveData<Event<ICCCardHelper>> {
//    var configurationFinished = false
//    val liveData: MutableLiveData<Event<ICCCardHelper>> = MutableLiveData()
//    if (NetPosTerminalConfig.liveData.hasActiveObservers()) {
//        NetPosTerminalConfig.liveData.removeObservers(lifecycleOwner)
//    }
//    val progressDialog = ProgressDialog(context)
//    progressDialog.setMessage("connecting, please wait...")
//    var observer: Observer<Event<Int>>? = null
//    observer = Observer<Event<Int>> {
//        it.getContentIfNotHandled()?.let { int ->
//            Timber.e("picked up: $int")
//            when (int) {
//                0 -> progressDialog.show()
//                1 -> {
//                    configurationFinished = true
//                    if (progressDialog.isShowing) {
//                        progressDialog.dismiss()
//                    }
//                    getCardLiveData(context, amount, cashBackAmount, liveData, compositeDisposable)
//                    NetPosTerminalConfig.liveData.removeObserver(observer!!)
//                }
//                -1 -> {
//                    configurationFinished = true
//                    if (progressDialog.isShowing) {
//                        progressDialog.dismiss()
//                    }
//                    if (NetPosTerminalConfig.getTerminalId().isEmpty()) {
//                        Toast.makeText(context, "No TID found on account", Toast.LENGTH_SHORT)
//                            .show()
//                    } else {
//                        Toast.makeText(context, "Connection Failed", Toast.LENGTH_SHORT).show()
//                    }
//                }
//                else -> {
//                }
//            }
//        }
//    }
//    NetPosTerminalConfig.liveData.observe(lifecycleOwner, observer)
//    when {
//        NetPosTerminalConfig.configurationStatus != 1 && NetPosTerminalConfig.isConfigurationInProcess.not() -> NetPosTerminalConfig.init(
//            context.applicationContext
//        )
//        NetPosTerminalConfig.isConfigurationInProcess -> if (configurationFinished.not()) progressDialog.show()
//        NetPosTerminalConfig.configurationStatus == 1 -> getCardLiveData(
//            context,
//            amount,
//            cashBackAmount,
//            liveData
//        )
//    }
//    return liveData
//}
//
//fun getCardLiveData(
//    context: Activity,
//    amount: Long,
//    cashBackAmount: Long,
//    liveData: MutableLiveData<Event<ICCCardHelper>>,
//    compositeDisposable: CompositeDisposable? = null
//) {
//    val dialog = ProgressDialog(context)
//        .apply {
//            setMessage("Waiting for card")
//            // setCancelable(false)
//        }
//    var iccCardHelper: ICCCardHelper? = null
//    val cardService = CardReaderService(
//        context,
//        listOf(DEV_ICC, DEV_PICC),
//        keyMode = POIHsmManage.PED_PINBLOCK_FETCH_MODE_TPK
//    )
//    val c = cardService.initiateICCCardPayment(
//        amount,
//        cashBackAmount
//    )
//        .subscribeOn(Schedulers.io())
//        .observeOn(AndroidSchedulers.mainThread())
//        .subscribe({
//            when (it) {
//                is CardReaderEvent.CardRead -> {
//                    val cardResult: CardReadResult = it.data
//                    val card = CardData(
//                        track2Data = cardResult.track2Data!!,
//                        nibssIccSubset = cardResult.nibssIccSubset,
//                        panSequenceNumber = cardResult.applicationPANSequenceNumber!!,
//                        posEntryMode = "051"
//                    )
//                    cardResult.cardScheme
//                    if (cardResult.encryptedPinBlock.isNullOrEmpty().not()) {
//                        card.apply {
//                            pinBlock = cardResult.encryptedPinBlock
//                        }
//                    }
//                    Timber.e(card.toString())
//                    iccCardHelper = ICCCardHelper(
//                        cardReadResult = cardResult,
//                        customerName = cardResult.cardHolderName,
//                        cardScheme = cardResult.cardScheme,
//                        cardData = card
//                    )
//                    val cardReaderMqttEvent = CardReaderMqttEvent(
//                        cardExpiry = cardResult.expirationDate,
//                        cardHolder = cardResult.cardHolderName,
//                        maskedPan = StringUtils.overlay(
//                            cardResult.applicationPANSequenceNumber,
//                            "xxxxxx",
//                            6,
//                            12
//                        )
//                    )
//                    sendCardEvent("SUCCESS", "00", cardReaderMqttEvent)
//                }
//                is CardReaderEvent.CardDetected -> {
//                    val mode = when (it.mode) {
//                        DEV_ICC -> {
//                            "EMV"
//                        }
//                        DEV_PICC -> {
//                            "EMV Contactless"
//                        }
//                        else -> "MAGNETIC STRIPE"
//                    }
//                    dialog.setMessage("Reading Card with $mode Please Wait")
//                }
//                else -> {
//                }
//            }
//        }, {
//            it?.let {
//                dialog.dismiss()
//                // sendCardEvent("ERROR", "99", CardReaderMqttEvent(readerError = it.localizedMessage))
//                Timber.e("error: ${it.localizedMessage}")
//                liveData.value = Event(ICCCardHelper(error = it))
//            }
//        }, {
//            dialog.dismiss()
//            iccCardHelper?.apply {
//                this.accountType = IsoAccountType.SAVINGS
//            }
//            liveData.value = Event(iccCardHelper!!)
////            showSelectAccountTypeDialog(context, iccCardHelper!!, liveData)
////            liveData.value = Event(iccCardHelper!!)
//        })
//
//    dialog.setButton(DialogInterface.BUTTON_POSITIVE, "Stop") { d, _ ->
//        cardService.transEnd(message = "Stopped")
//        d.dismiss()
//    }
//    dialog.show()
//    compositeDisposable?.add(c)
//}
//
//fun sendCardEvent(s: String, s1: String, cardReaderMqttEvent: CardReaderMqttEvent) {
//    val event = MqttEvent<CardReaderMqttEvent>()
//    event.apply {
//        this.event = MqttEvents.CARD_READER_EVENTS.event
//        data = cardReaderMqttEvent
//        this.status = s
//        timestamp = System.currentTimeMillis()
//        this.code = s1
//    }
//    MqttHelper.sendPayload(MqttTopics.CARD_READER_EVENTS, event)
//}
//
private fun showSelectAccountTypeDialog(
    context: Activity,
    iccCardHelper: ICCCardHelper,
    liveData: MutableLiveData<Event<ICCCardHelper>>
) {
    Timber.e("show select account dialog")
    var dialogSelectAccountTypeBinding: DialogSelectAccountTypeBinding
    val dialog = AlertDialog.Builder(context)
        .apply {
            dialogSelectAccountTypeBinding =
                DialogSelectAccountTypeBinding.inflate(LayoutInflater.from(context), null, false)
                    .apply {
                        executePendingBindings()
                    }
            setView(dialogSelectAccountTypeBinding.root)
            setCancelable(false)
        }.create()
    dialogSelectAccountTypeBinding.accountTypes.setOnCheckedChangeListener { _, checkedId ->
        val accountType = when (checkedId) {
            R.id.savings_account -> IsoAccountType.SAVINGS
            R.id.current_account -> IsoAccountType.CURRENT
            R.id.credit_account -> IsoAccountType.CREDIT
            R.id.bonus_account -> IsoAccountType.BONUS_ACCOUNT
            R.id.investment_account -> IsoAccountType.INVESTMENT_ACCOUNT
            R.id.universal_account -> IsoAccountType.UNIVERSAL_ACCOUNT
            else -> IsoAccountType.DEFAULT_UNSPECIFIED
        }
        dialog.dismiss()
        Timber.e("$checkedId")
        if (accountType != IsoAccountType.DEFAULT_UNSPECIFIED) {
            iccCardHelper.apply {
                this.accountType = accountType
            }
            liveData.value = Event(iccCardHelper)
        }
    }
    dialogSelectAccountTypeBinding.cancelButton.setOnClickListener {
        dialog.dismiss()
        liveData.value = Event(ICCCardHelper(error = Throwable("Operation was canceled")))
    }
    dialog.show()
}


// --- Entry Point ---
fun showCardDialog(
    context: Activity,
    lifecycleOwner: LifecycleOwner,
    amount: Long,
    cashBackAmount: Long,
    compositeDisposable: CompositeDisposable? = null
): LiveData<Event<ICCCardHelper>> {

    if (k11IsRunning) {
        Log.d("K11_DEBUG", "Gatekeeper blocked duplicate call")
        return MutableLiveData()
    }

    // 2. Kill any old dialog that might be hanging around
    k11ActiveDialog?.dismiss()
    k11ActiveDialog = null

    var configurationFinished = false
    val liveData: MutableLiveData<Event<ICCCardHelper>> = MutableLiveData()

    if (NetPosTerminalConfig.liveData.hasActiveObservers()) {
        NetPosTerminalConfig.liveData.removeObservers(lifecycleOwner)
    }

    val progressDialog = ProgressDialog(context)
    progressDialog.setMessage("Connecting, please wait...")

    // Listen for configuration status updates from NetPosTerminalConfig
    val observer = Observer<Event<Int>> { event ->
        event.getContentIfNotHandled()?.let { status ->
            Log.d("CONFIG_DEBUG", "Observer picked status: $status")
            when (status) {
                0 -> {
                    configurationFinished = false
                    if (!progressDialog.isShowing) progressDialog.show()
                }

                1 -> {
                    configurationFinished = true
                    if (progressDialog.isShowing) progressDialog.dismiss()
                    k11IsRunning = true
                    Log.d("CONFIG_DEBUG", "Config complete via observer. Starting reader...")
                    getCardLiveData(context, amount, cashBackAmount, liveData, compositeDisposable)
//                    NetPosTerminalConfig.liveData.removeObserver(observer)
                }

                -1 -> {
                    configurationFinished = true
                    if (progressDialog.isShowing) progressDialog.dismiss()
                    val msg =
                        if (NetPosTerminalConfig.getTerminalId().isEmpty()) "No TID found on account"
                        else "Connection Failed"
                    Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
//                    NetPosTerminalConfig.liveData.removeObserver(observer)
                }
            }
        }
    }
    NetPosTerminalConfig.liveData.observe(lifecycleOwner, observer)

    // Inside showCardDialog – handle current state immediately
    val currentStatus = NetPosTerminalConfig.configurationStatus
    Log.d("CONFIG_DEBUG", "Current Internal Status: $currentStatus")

    when {
        currentStatus == 1 -> {
            // Already configured – no need to wait for events
            k11IsRunning = true
            if (progressDialog.isShowing) progressDialog.dismiss()
            Log.d("CONFIG_DEBUG", "Config already OK. Starting reader immediately...")
            getCardLiveData(context, amount, cashBackAmount, liveData, compositeDisposable)
        }

        !NetPosTerminalConfig.isConfigurationInProcess -> {
            Log.d("CONFIG_DEBUG", "Status is $currentStatus. Triggering Fresh Init...")
            progressDialog.show()
            NetPosTerminalConfig.init(context.applicationContext)
        }

        NetPosTerminalConfig.isConfigurationInProcess -> {
            Log.d("CONFIG_DEBUG", "Waiting for existing process...")
            if (!progressDialog.isShowing) progressDialog.show()
        }
    }
//    when {
//        NetPosTerminalConfig.configurationStatus != 1 && !NetPosTerminalConfig.isConfigurationInProcess -> {
//            NetPosTerminalConfig.init(context.applicationContext)
//        }
//        NetPosTerminalConfig.isConfigurationInProcess -> if (!configurationFinished) progressDialog.show()
//        NetPosTerminalConfig.configurationStatus == 1 -> {
//            getCardLiveData(context, amount, cashBackAmount, liveData, compositeDisposable)
//        }
//    }

    return liveData
}

fun getCardLiveData(
    context: Activity,
    amount: Long,
    cashBackAmount: Long,
    liveData: MutableLiveData<Event<ICCCardHelper>>,
    compositeDisposable: CompositeDisposable? = null
) {
    // 1. BRANCHING LOGIC FOR HORIZON K11
    if (android.os.Build.MODEL.contains("K11", ignoreCase = true)) {
        startK11CardFlow(context, amount, liveData)
        return
    }

    // 2. ORIGINAL KOZEN FLOW
    val dialog = ProgressDialog(context).apply { setMessage("Waiting for card") }
    val cardService = CardReaderService(
        context, listOf(DEV_ICC, DEV_PICC), keyMode = POIHsmManage.PED_PINBLOCK_FETCH_MODE_TPK
    )

    val c = cardService.initiateICCCardPayment(amount, cashBackAmount).subscribeOn(Schedulers.io())
        .observeOn(AndroidSchedulers.mainThread()).subscribe({ event ->
            if (event is CardReaderEvent.CardRead) {
                val res = event.data
                val encryptedPinBlock = res.encryptedPinBlock
                val card = CardData(
                    res.track2Data!!, res.nibssIccSubset, res.applicationPANSequenceNumber!!, "051"
                ).apply { pinBlock = encryptedPinBlock }
                liveData.value = Event(ICCCardHelper(cardReadResult = res, cardData = card))
            }
        }, { liveData.value = Event(ICCCardHelper(error = it)) }, { dialog.dismiss() })

    dialog.show()
    compositeDisposable?.add(c)
}


private fun startK11CardFlow(
    context: Activity, amount: Long, liveData: MutableLiveData<Event<ICCCardHelper>>
) {


    k11ActiveDialog?.dismiss()
//    val dialog = ProgressDialog(context).apply {
//        setMessage("K11: Please Insert Card [ID: ${System.currentTimeMillis() % 1000}]")
//        setCancelable(false)
//        show()
//    }
    val dialog = ProgressDialog(context).apply {
        setMessage("K11: Please Insert or Tap Card")
        setCancelable(false)
        show()
    }
    k11ActiveDialog = dialog // Track this specific dialog globally

    try {
        val reader = DeviceHelper.getCardReader()

        if (reader == null) {
            Log.e("K11_HARDWARE", "FATAL: DeviceHelper.getCardReader() returned NULL")
            context.runOnUiThread { dialog.dismiss() }
            Toast.makeText(context, "Hardware Reader Not Initialized", Toast.LENGTH_LONG).show()
            return
        }
        Log.d("K11_HARDWARE", "Reader found. Calling searchCard...")

        // searchCard(mag, ic, rf, timeout, listener)
        reader.searchCard(false, true, true, 30, object : AidlCheckCardListener.Stub() {
            override fun onFindMagCard(data: TrackData?) { /* Handle Mag */
                context.runOnUiThread { dialog.dismiss() }
                Log.d("K11_HARDWARE", "Magnetic Card Found")
            }

            override fun onFindICCard() {
                context.runOnUiThread {
                    dialog.setMessage("Reading Chip...")
                    try {
                        // FIX: The method name is stopSearch()
//                        DeviceHelper.getCardReader()?.cancelSearch()
                    } catch (e: RemoteException) {
                        e.printStackTrace()
                    }

                    startK11Emv(amount, liveData, context, dialog, posEntryMode = "051")
                }
                Log.d("K11_HARDWARE", "IC Chip Card Found")
//                context.runOnUiThread { startK11Emv(amount, liveData, dialog, context)
//                }
            }

            override fun onFindRFCard(cardType: Int) {
                dialog.dismiss()
                Log.d("K11_HARDWARE", "Contactless Card Found: $cardType")
                context.runOnUiThread {
                    dialog.setMessage("Reading Contactless...")
                    startK11Emv(amount, liveData, context, dialog, posEntryMode = "071")
                }
            }

            override fun onSwipeCardFail() {
                context.runOnUiThread { dialog.dismiss() }
            }

            override fun onTimeout() {
                context.runOnUiThread { dialog.dismiss() }
            }

            override fun onCancelled() {
                context.runOnUiThread { dialog.dismiss() }
            }

            override fun onError(code: Int) {
                Log.e("K11_HARDWARE", "SearchCard Error Code: $code")
                context.runOnUiThread {
                    dialog.dismiss()
                    liveData.postValue(Event(ICCCardHelper(error = Throwable("Error $code"))))
                }
            }
        })
    } catch (e: Exception) {
        context.runOnUiThread { dialog.dismiss() }
    }
}

private fun setK11TerminalConfig(emvL2: com.horizonpay.smartpossdk.aidl.emv.IAidlEmvL2) {
    try {
        // Use the SDK-provided Entity instead of a Bundle
        val termConfig = com.horizonpay.smartpossdk.aidl.emv.EmvTermConfig().apply {
            // 9F33: Terminal Capabilities (E0F8C8 supports PIN & DUKPT)
            capability = "E0F8C8"

            // 9F1A: Terminal Country Code (Nigeria is 0566)
            countryCode = "0566"

            // 5F2A: Transaction Currency Code (Naira is 0566)
            transCurrCode = "0566"

            // 9F35: Terminal Type (22 is Attended Online POS)
            termType = 22

            // 9F40: Additional Terminal Capabilities
            capability = "F000F0A001"

            // Set the Exponent (usually 02 for Naira)
            transCurrExp = 2
        }

        // Pass the EmvTermConfig object to the kernel
        val success = emvL2.setTermConfig(termConfig)
        Log.d("K11_CONFIG", "Terminal Config Applied: $success")
    } catch (e: RemoteException) {
        Log.e("K11_CONFIG", "Failed to set terminal config", e)
    }
}

private fun startK11Emv(
    amount: Long,
    liveData: MutableLiveData<Event<ICCCardHelper>>,
    context: Activity,
    dialog: ProgressDialog,
    posEntryMode: String
) {
    Log.d("K11_EMV", "Starting EMV Kernel for amount: $amount")
//    var iccCardHelper: ICCCardHelper? = null
    var encryptedPinBlock: String? = null
    try {
        val emvL2 = DeviceHelper.getEmvHandler()
        if (emvL2 == null) {
            Log.e("K11_EMV", "EMV Handler is NULL")
            return
        }
//        setK11TerminalConfig(emvL2)

        val emvTransData = EmvTransData().apply {
            setAmount(amount)
            setOtherAmount(0)
            setForceOnline(true)
            setEmvFlowType(EmvConstant.EmvTransFlow.FULL)
            setTransType(0x00.toByte())
            setTransTime("60000")
        }
        Log.d("K11_EMV", "Calling emvL2.startEmvProcess...")


        emvL2.startEmvProcess(emvTransData, object : AidlEmvStartListener.Stub() {
            override fun onRequestAmount() {
                emvL2.requestAmountResp(amount.toString())
                Log.d("K11_EMV", "Kernel requested amount")
            }

            override fun onRequestAidSelect(times: Int, aids: MutableList<CandidateAID>?) {
                emvL2.requestAidSelectResp(0) // Select first AID
                Log.d("K11_EMV", "Kernel requested AID select. Found ${aids?.size} AIDs")
            }

            override fun onFinalSelectAid(data: EmvFinalSelectData?) {
                emvL2.requestFinalSelectAidResp(data?.aid)
                Log.d("K11_EMV", "Final AID Selected: ${data?.aid}")
            }

            override fun onConfirmCardNo(cardNo: String?) {
                // In a real app, show a dialog to confirm. For now, auto-confirm:
                emvL2.confirmCardNoResp(true)
                Log.d("K11_EMV", "Card Number: $cardNo")
            }

            override fun onRequestPin(isOnlinePIN: Boolean, leftTimes: Int) {
                Log.d("K11_EMV", "PIN Requested. Online: $isOnlinePIN")

                // 1. Get the PAN for PIN block encryption (12 right-most digits excluding check digit)
                val panForPinBlock = panForIso9564PinBlockFromEmv(emvL2)
                Log.d("K11_DEBUG_SECURITY", "PIN_DEBUG: derived panForPinBlock=%s $panForPinBlock")
                if (panForPinBlock.isNullOrBlank()) {
                    Log.e("K11_EMV", "Unable to derive PAN for PIN block. Tag 5A/57 missing.")
                    emvL2.requestPinResp(null, false)
                    return
                }
//                if (pan.isEmpty()) {
//                    // Fallback to the card number read during confirmCardNo if 5A is empty
//                    pan = cardNum
//                }

                // 2. Set up the UI Bundle (Matches your setPinpadUI method)
                val bundle = Bundle().apply {
                    putBoolean(PinpadConst.PinpadShow.COMMON_NEW_LAYOUT, true)
                    putString(PinpadConst.PinpadShow.COMMON_OK_TEXT, "Enter")
                    putBoolean(PinpadConst.PinpadShow.COMMON_SUPPORT_BYPASS, false)
                    putBoolean(PinpadConst.PinpadShow.COMMON_IS_RANDOM, true)
                    putBoolean(PinpadConst.PinpadShow.COMMON_SUPPORT_KEYVOICE, true)
                    putString(
                        PinpadConst.PinpadShow.TITLE_HEAD_CONTENT,
                        if (isOnlinePIN) "Please Enter PIN" else "Please Enter Offline PIN"
                    )
                }

                try {
                    val pinpad = DeviceHelper.getPinpad()

                    if (isOnlinePIN) {
                        // Use inputOnlinePin for EMV Online PIN
                        pinpad.inputOnlinePin(
                            bundle,
                            intArrayOf(4, 4), // Allowed lengths
                            60,               // Timeout in seconds
                            panForPinBlock,
                            0, // Key index for DUKPT
                            ISO9564FMT1, // PinpadConst.PinAlgorithmMode.ISO9564FMT1
                            object : AidlPinPadInputListener.Stub() {
                                override fun onConfirm(
                                    data: ByteArray?, noPin: Boolean, ksn: String?
                                ) {
                                    // The pinpad returns DUKPT-encrypted PIN block in 'data' parameter
                                    // Simply convert it to hex format - don't re-encrypt
                                    val hexPin: String = ConvertUtils.bytes2HexString(data) ?: ""
                                    val dataLen = data?.size ?: 0

                                    encryptedPinBlock = hexPin

                                    Log.d("K11_DEBUG_SECURITY", "PIN BLOCK (DUKPT from pinpad): $hexPin")
                                    Log.d("K11_DEBUG_SECURITY", "PIN BLOCK LENGTH: ${hexPin.length} chars (${dataLen} bytes)")
                                    Log.d("K11_DEBUG_SECURITY", "PIN BLOCK HEX BYTES: ${hexPin.chunked(2).joinToString(" ")}")
                                    Log.d("K11_DEBUG_SECURITY", "KSN: $ksn")
                                    Log.d("K11_DEBUG_SECURITY_PANFORPINBLOCK", "PAN for PIN: $panForPinBlock")


                                    Log.d(
                                        "K11_DEBUG_SECURITY",
                                        "Encrypted PIN Block (Field 52): %s $hexPin"
                                    )
                                    Log.d(
                                        "K11_DEBUG_SECURITY",
                                        "PIN_DEBUG: panForPinBlock=$panForPinBlock, keyIndex=0, ksn=${ksn ?: "<null>"}, noPin=$noPin, pinBytes="
                                    )
//                                    if (hexPin.length == 16 && dataLen == 8) {
//                                        encryptedPinBlock = hexPin
//                                    } else {
//                                        Log.w(
//                                            "K11_DEBUG_SECURITY",
//                                            "PIN_DEBUG: Unexpected PIN block format: hexLen=${hexPin.length}, bytes=$dataLen, value=$hexPin"
//                                        )
//                                    }

                                    Log.d(
                                        "K11_DEBUG_SECURITY",
                                        "Encrypted PIN Block LAST ONE: $hexPin"
                                    )

                                    emvL2.requestPinResp(data, noPin)

                                }

                                override fun onSendKey(keyCode: Int) {}
                                override fun onCancel() {
                                    emvL2.requestPinResp(null, false)
                                }

                                override fun onError(errorCode: Int) {
                                    emvL2.requestPinResp(null, false)

                                }

                                override fun getPinPadKey(keys: ByteArray?) {}
                                override fun getPinNum(num: String?) {}
                            })
                    } else {
                        // Use inputOfflinePin for Plaintext/Ciphered Offline PIN
                        pinpad.inputOfflinePin(
                            bundle, intArrayOf(4, 6), 60, object : AidlPinPadInputListener.Stub() {
                                override fun onConfirm(
                                    data: ByteArray?, noPin: Boolean, ksn: String?
                                ) {
                                    emvL2.requestPinResp(data, noPin)
                                }

                                override fun onSendKey(keyCode: Int) {}
                                override fun onCancel() {
                                    emvL2.requestPinResp(null, false)
                                }

                                override fun onError(errorCode: Int) {
                                    emvL2.requestPinResp(null, false)
                                }

                                override fun getPinPadKey(keys: ByteArray?) {}
                                override fun getPinNum(num: String?) {}
                            })
                    }
                } catch (e: Exception) {
                    Log.e("K11_EMV", "Pinpad call failed", e)
                    emvL2.requestPinResp(null, false)
                }
            }

            override fun onResquestOfflinePinDisp(times: Int) {}

            override fun onRequestOnline(output: EmvTransOutputData?) {

                val cvmResults = emvL2.getTagValue("9F34") ?: ""
                Log.d("K11_EMV", "CVM Results (9F34): $cvmResults")

                val tvr = emvL2.getTagValue("95") ?: "0000000000"
                Log.d("K11_EMV", "TVR during Online Request: $tvr")

                // Check if PIN verification failed
                // CVM Results format: 1 byte method + 1 byte status + 1 byte IAD
                // Status byte bits:
                // Bit 5 (0x20): CVM Failed (explicit failure)
                // Only decline if we have EXPLICIT failure indicator
                // Values like 0x03 mean "not performed" (K11 defers PIN verification to host)
                var pinVerificationFailed = false
                
                if (cvmResults.length >= 4) {
                    val cvmStatusByte = cvmResults.substring(2, 4).toInt(16)
                    val cvmFailedBit5 = (cvmStatusByte and 0x20) != 0  // Bit 5: CVM Failed (as assumed)
                    val cvmFailedBit6 = (cvmStatusByte and 0x40) != 0
                    val cvmFailedBit7 = (cvmStatusByte and 0x80) != 0
                    val cvmFailed = cvmFailedBit5
                    Log.d("K11_EMV", "CVM Status byte (0x${cvmResults.substring(2, 4)}): cvmFailed=$cvmFailed (status byte: $cvmStatusByte)")
                    
                    // Only fail if bit 5 (0x20) is explicitly set
                    // If not set, either CVM was not performed (deferred) or was successful
                    pinVerificationFailed = cvmFailed
                    Log.d("K11_EMV", "CVM Result: PIN Verification Failed=$pinVerificationFailed")

                } else {
                    // Fallback to TVR check if CVM Results unavailable
                    val byte3 = if (tvr.length >= 6) tvr.substring(4, 6).toInt(16) else 0
                    pinVerificationFailed = (byte3 and 0x80) != 0
                    Log.d("K11_EMV", "Using TVR fallback - Byte 3 is 0x${String.format("%02X", byte3)}. PIN Failure: $pinVerificationFailed")
                }

                Log.d("K11_EMV", "Final PIN Verification Result: pinVerificationFailed=$pinVerificationFailed")

                if (pinVerificationFailed) {
                    Log.e("K11_EMV", "PIN verification failed (explicit CVM failure). Declining transaction.")
                    context.runOnUiThread {
                        k11IsRunning = false
                        k11ActiveDialog?.dismiss()
                        Toast.makeText(
                            context, "PIN Verification Failed - Transaction Declined", Toast.LENGTH_LONG
                        ).show()
                    }
                    emvL2.requestOnlineResp("01", "")
                    return
                }


                // 1. Get the Raw Track 2 (Tag 57)
                val rawTrack2 = emvL2.getTagValue("57") ?: ""

                // 2. Clean/Format Track 2 for the NIBSS library
                val formattedTrack2 =
                    rawTrack2.uppercase(Locale.ROOT).replace("F", "").replace("=", "D")

                // 3. Extract Card Scheme and Name (Tags 50 and 5F20)
                // We convert these from Hex to readable ASCII strings
                val rawAppLabel = emvL2.getTagValue("50") ?: ""
                val rawHolderName = emvL2.getTagValue("5F20") ?: ""

                val cardScheme = if (rawAppLabel.isNotEmpty()) hexToAscii(rawAppLabel) else "CARD"
                val customerName =
                    if (rawHolderName.isNotEmpty()) hexToAscii(rawHolderName) else "CUSTOMER"

                // 4. Get ICC Data (Tag 55) for NIBSS
                val iccData = emvL2.getTlvByTags(
                    arrayOf(
                        "9F26",
                        "9F27",
                        "9F10",
                        "9F37",
                        "9F36",
                        "95",   // <--- Terminal Verification Results (Must be here!)
                        "9A",
                        "9C",
                        "9F02",
                        "5F2A",
                        "82",
                        "9F1A",
                        "9F03",
                        "9F33",
                        "9F34", // <--- CVM Results (Must be here!)
                        "9F35",
                        "9F1E",
                        "84",
                        "9F09",
                        "9F41",
                        "9F63"
                    )
                ) ?: ""

                Log.d("K11_EMV", "Constructed Tag 55: $iccData")


                val panSeq = emvL2.getTagValue("5F34") ?: "00"

//                try {
//                    Log.d("K11_EMV", "Final Track2 for Lib: $formattedTrack2")
//
//                    // 5. Create the CardData object required by epmslib
////                    val card = CardData(formattedTrack2, iccData, panSeq, "051")
//
//                    Log.d("K11_EMV", "Final Track2 for CARD: $encryptedPinBlock")
//
//                    val card = CardData(
//                        track2Data = formattedTrack2,
//                        nibssIccSubset = iccData,
//                        panSequenceNumber = panSeq,
//                        posEntryMode = "051"
//                    ).apply {
//                        // NIBSS often requires the PIN block to be exactly 16 characters Uppercase
//                        this.pinBlock = encryptedPinBlock
//                    }
//                    // 6. Create the Helper and populate it fully to avoid NullPointer in DashboardFragment
//                    val iccCardHelper = ICCCardHelper(
//                        cardScheme = cardScheme,
//                        accountType = IsoAccountType.SAVINGS,
//                        cardData = card
//                    )
//
//
//                    context.runOnUiThread {
//                        // 7. Post the fully populated object
//                        k11IsRunning = false // OPEN THE GATE
//                        if (k11ActiveDialog != null && k11ActiveDialog!!.isShowing) {
//                            k11ActiveDialog!!.dismiss()
//                            k11ActiveDialog = null
//                        }
//                        liveData.value = Event(iccCardHelper)
//                    }
//                } catch (e: Exception) {
//                    Log.e("K11_EMV", "CardData Creation Failed: ${e.message}")
//                    // ADD THIS:
//                    k11IsRunning = false
//                    k11ActiveDialog?.dismiss()
//                }
                try {
                    // 1. Create the CardData object
                    val card = CardData(
                        track2Data = formattedTrack2,
                        nibssIccSubset = iccData,
                        panSequenceNumber = "001",
                        posEntryMode = "051"
                    )
                        .apply {
                            pinBlock = encryptedPinBlock
                            Log.d("K11_DEBUG_SECURITY", "PIN block generated: ${encryptedPinBlock?.take(8)}... but NOT sending in Field 52")
                            Log.d("K11_DEBUG_SECURITY", "Reason: epmslib serialization issue with PIN block changes message prefix")
                            Log.d("K11_DEBUG_SECURITY", "PIN verification will use CVM results from Field 55 (ICC data) instead")
                            
                            // Leave pinBlock null/unset - don't send it to NIBSS
                    }

                    // 2. Create the Helper (Initial state)
                    val iccCardHelper = ICCCardHelper(
                        cardScheme = cardScheme,
                        customerName = customerName, // Added this for completeness
                        cardData = card
                    )

                    context.runOnUiThread {
                        // 3. Close the "Reading Card" dialog
                        if (k11ActiveDialog != null && k11ActiveDialog!!.isShowing) {
                            k11ActiveDialog!!.dismiss()
                            k11ActiveDialog = null
                        }

                        // 4. RESET the gatekeeper so the UI can proceed
                        k11IsRunning = false

                        // 5. INSTEAD of posting to liveData, show the Account Selection Dialog
                        // This dialog will handle posting to liveData once the user picks an account
                        showSelectAccountTypeDialog(context, iccCardHelper, liveData)
                    }

                } catch (e: Exception) {
                    Log.e("K11_EMV", "CardData Creation Failed: ${e.message}")
                    k11IsRunning = false
                    k11ActiveDialog?.dismiss()
                }
                // Tell kernel to wait for online response (00 means success/proceed)
                emvL2.requestOnlineResp("00", "")
            }

            /**
             * Helper function to convert EMV Hex Tags (like 50 or 5F20) to human-readable ASCII
             */
            override fun onFinish(result: Int, output: EmvTransOutputData?) {
                Log.d("K11_EMV", "EMV Finished. Result Code: $result")
                context.runOnUiThread { if (dialog.isShowing) dialog.dismiss() }
            }
            override fun onError(code: Int) {
                Log.e("K11_EMV", "Kernel ERROR: $code")
                context.runOnUiThread {
                    if (dialog.isShowing) dialog.dismiss()
                    liveData.postValue(Event(ICCCardHelper(error = Throwable("EMV Error $code"))))
                }
            }
        })
    } catch (e: Exception) {
        context.runOnUiThread { if (dialog.isShowing) dialog.dismiss() }
    }
}


private fun hexToAscii(hexStr: String): String {
    val output = StringBuilder()
    try {
        var i = 0
        while (i < hexStr.length) {
            val str = hexStr.substring(i, i + 2)
            val charCode = str.toInt(16)
            if (charCode in 32..126) { // Only add printable characters
                output.append(charCode.toChar())
            }
            i += 2
        }
    } catch (e: Exception) {
        return hexStr
    }
    return output.toString().trim()
}
