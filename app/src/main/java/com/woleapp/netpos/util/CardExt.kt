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

//    val observer = Observer<Event<Int>> { event ->
//        event.getContentIfNotHandled()?.let { status ->
//            when (status) {
//                0 -> progressDialog.show()
//                1 -> {
//                    configurationFinished = true
//                    if (progressDialog.isShowing) progressDialog.dismiss()
//                    getCardLiveData(context, amount, cashBackAmount, liveData, compositeDisposable)
//                }
//                -1 -> {
//                    configurationFinished = true
//                    if (progressDialog.isShowing) progressDialog.dismiss()
//                    val msg = if (NetPosTerminalConfig.getTerminalId().isEmpty()) "No TID found" else "Connection Failed"
//                    Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
//                }
//            }
//
//        }
//    }


    // Inside showCardDialog
    val currentStatus = NetPosTerminalConfig.configurationStatus
    Log.d("CONFIG_DEBUG", "Current Internal Status: $currentStatus")

    when {
        // ONLY proceed if status is exactly 1 (Success)
        currentStatus == 1 -> {
            k11IsRunning = true
            Log.d("CONFIG_DEBUG", "Success status confirmed. Starting reader...")
            getCardLiveData(context, amount, cashBackAmount, liveData, compositeDisposable)
        }

        // If status is -99 or anything else, we MUST initialize
        !NetPosTerminalConfig.isConfigurationInProcess -> {
            Log.d("CONFIG_DEBUG", "Status is $currentStatus. Triggering Fresh Init...")
            progressDialog.show()
            NetPosTerminalConfig.init(context.applicationContext)
        }

        NetPosTerminalConfig.isConfigurationInProcess -> {
            Log.d("CONFIG_DEBUG", "Waiting for existing process...")
            progressDialog.show()
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
                val card = CardData(
                    res.track2Data!!, res.nibssIccSubset, res.applicationPANSequenceNumber!!, "051"
                ).apply { pinBlock = res.encryptedPinBlock }
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

                    startK11Emv(amount, liveData, context, dialog)
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
                    startK11Emv(amount, liveData, context, dialog)
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

private fun startK11Emv(
    amount: Long,
    liveData: MutableLiveData<Event<ICCCardHelper>>,
    context: Activity,
    dialog: ProgressDialog
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

                // 1. Get the PAN for PIN block encryption
                var pan = emvL2.getTagValue("5A")?.replace("F", "") ?: ""
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
                            intArrayOf(4, 6), // Allowed lengths
                            60,               // Timeout in seconds
                            pan,
                            0,                // Key Index (usually 0 for TPK)
                            PinpadConst.PinAlgorithmMode.ISO9564FMT1,
                            object : AidlPinPadInputListener.Stub() {
                                override fun onConfirm(
                                    data: ByteArray?, noPin: Boolean, ksn: String?
                                ) {
//                                    Log.d("K11_EMV", "PIN Input Success. Block: ${data}")
//                                    emvL2.requestPinResp(data, noPin)

                                    val hexPin =
                                        data?.joinToString("") { "%02x".format(it) } ?: "NULL"
                                    Log.d(
                                        "K11_DEBUG_SECURITY",
                                        "Encrypted PIN Block (Field 52): $hexPin"
                                    )
                                    encryptedPinBlock = hexPin
                                    // Store it in the variable we created above
//                                    iccCardHelper?.cardData?.apply { pinBlock = hexPin }
//                                    Log.d("K11_DEBUG_SECURITY", "Encrypted PIN Block (Field 52): ${iccCardHelper?.cardData}")

//                                    showSelectAccountTypeDialog(context, iccCardHelper!!, liveData)
//                                    val card = ICCCardHelper().cardData
//                                    val cardRes = ICCCardHelper().cardReadResult
//                                    if (cardRes?.encryptedPinBlock.isNullOrEmpty().not()) {
//                                        Log.d(
//                                            "K11_DEBUG_SECURITY",
//                                            "Encrypted PIN Block GOT HERE: $hexPin"
//                                        )
//                                        card?.apply {
//                                            Log.d(
//                                                "K11_DEBUG_SECURITY",
//                                                "Encrypted PIN Block GOT HERE 2222: $hexPin"
//                                            )
//                                            pinBlock = cardRes?.encryptedPinBlock
//                                        }
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

                // Check if Offline PIN was tried and failed
                val byte3 = if (tvr.length >= 6) tvr.substring(4, 6).toInt(16) else 0
                val pinVerificationFailed = (byte3 and 0x80 != 0)

                Log.d("K11_EMV", "TVR Byte 3 is $byte3. Decline triggered: $pinVerificationFailed")

                if (pinVerificationFailed) {
                    Log.e("K11_EMV", "Hard PIN Failure detected. Declining.")
                    context.runOnUiThread {
                        k11IsRunning = false
                        k11ActiveDialog?.dismiss()
                        Toast.makeText(
                            context, "PIN Error - Transaction Declined", Toast.LENGTH_LONG
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

                try {
                    Log.d("K11_EMV", "Final Track2 for Lib: $formattedTrack2")

                    // 5. Create the CardData object required by epmslib
//                    val card = CardData(formattedTrack2, iccData, panSeq, "051")

                    Log.d("K11_EMV", "Final Track2 for CARD: $encryptedPinBlock")

                    val card = CardData(
                        track2Data = formattedTrack2,
                        nibssIccSubset = iccData,
                        panSequenceNumber = panSeq,
                        posEntryMode = "051"
                    ).apply {
                        // NIBSS often requires the PIN block to be exactly 16 characters Uppercase
                        this.pinBlock = encryptedPinBlock
                    }
                    // 6. Create the Helper and populate it fully to avoid NullPointer in DashboardFragment
                    val iccCardHelper = ICCCardHelper(
                        cardScheme = cardScheme,
                        accountType = IsoAccountType.SAVINGS,
                        cardData = card
                    )


                    context.runOnUiThread {
                        // 7. Post the fully populated object
                        k11IsRunning = false // OPEN THE GATE
                        if (k11ActiveDialog != null && k11ActiveDialog!!.isShowing) {
                            k11ActiveDialog!!.dismiss()
                            k11ActiveDialog = null
                        }
                        liveData.value = Event(iccCardHelper)
                    }
                } catch (e: Exception) {
                    Log.e("K11_EMV", "CardData Creation Failed: ${e.message}")
                    // ADD THIS:
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
