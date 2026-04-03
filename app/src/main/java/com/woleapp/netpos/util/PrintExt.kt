package com.woleapp.netpos.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Paint
import android.os.Build
import android.os.Bundle
import com.danbamitale.epmslib.entities.TransactionResponse
import com.danbamitale.epmslib.entities.responseMessage
import com.danbamitale.epmslib.extensions.formatCurrencyAmount
import com.horizonpay.smartpossdk.aidl.printer.AidlPrinterListener
import com.horizonpay.smartpossdk.aidl.printer.IAidlPrinter
import com.netpluspay.netpossdk.NetPosSdk
import com.netpluspay.netpossdk.printer.PrinterResponse
import com.netpluspay.netpossdk.printer.ReceiptBuilder
import com.netpluspay.netpossdk.utils.DeviceConfig
import com.pos.sdk.printer.POIPrinterManage
import com.pos.sdk.printer.models.BitmapPrintLine
import com.pos.sdk.printer.models.PrintLine
import com.pos.sdk.printer.models.TextPrintLine
import com.woleapp.netpos.BuildConfig
import com.woleapp.netpos.R
import com.woleapp.netpos.app.DeviceHelper
import com.woleapp.netpos.model.NipNotification
import com.woleapp.netpos.util.DateTimeUtil.getDateFromMilliseconds
import com.woleapp.netpos.util.RandomNumUtil.formatCurrencyAmountUsingCurrentModule
import com.woleapp.netpos.util.horizonpay.K11ReceiptPrinter
import io.reactivex.Observable
import io.reactivex.ObservableEmitter
import io.reactivex.Single
import io.reactivex.SingleEmitter
import timber.log.Timber
import java.text.SimpleDateFormat
import java.util.* // ktlint-disable no-wildcard-imports

fun newEndOfDayPrintImplementation(
    index: Int,
    it: TransactionResponse,
    printerManager: POIPrinterManage,
    textPrintLine: TextPrintLine
) {
    val status = if (it.responseCode == "00") "A" else "D"
    val formattedAmount = it.amount.div(100).formatCurrencyAmountUsingCurrentModule()
    val formattedTime =
        getDateFromMilliseconds(it.transactionTimeInMillis).split("T").last()
    textPrintLine.apply {
        content = if (it.RRN.startsWith(
                "F",
                true
            )
        ) "${it.RRN}  $status  $formattedTime  $formattedAmount" else "${it.RRN}  $status  $formattedTime  $formattedAmount"
    }
    printerManager.appendTextEntity(textPrintLine)

    if (index >= 9) {
        if (index == 9) {
            drawStraightLine(printerManager, textPrintLine)
        } else {
            if (index > 10 && index % 10 == 0) {
                drawStraightLine(printerManager, textPrintLine)
            }
        }
    }
}

private fun drawStraightLine(
    printerManager: POIPrinterManage,
    textPrintLine: TextPrintLine
) {
    textPrintLine.apply {
        content = "-----------------------------------------------"
    }
    printerManager.appendTextEntity(textPrintLine)
}

fun previousEndOfDayPrintImplementation(
    index: Int,
    it: TransactionResponse,
    printerManager: POIPrinterManage,
    textPrintLine: TextPrintLine
) {
    textPrintLine.apply {
        isBold = true
        content = if (it.responseCode == "00") "Approved" else "Declined"
    }
    printerManager.appendTextEntity(textPrintLine)
    textPrintLine.apply {
        isBold = false
        content = "Amount: ${it.amount.formatCurrencyAmount("\u20A6")}"
    }
    printerManager.appendTextEntity(textPrintLine)
    textPrintLine.apply {
        content = "RRN: ${it.RRN}"
    }
    printerManager.appendTextEntity(textPrintLine)
    textPrintLine.apply {
        content = "Date: ${it.transactionTimeInMillis.formatDate()}"
    }
    printerManager.appendTextEntity(textPrintLine)
    drawStraightLine(printerManager, textPrintLine)
}

fun List<TransactionResponse>.printEndOfDay(
    context: Context
): Single<PrinterResponse> {

    if (Build.MODEL.equals("mini", true) || Build.MODEL.equals("p5", true)) {
        return Single.error(Throwable("Device cannot print"))
    }

    // 2. BRANCHING LOGIC FOR K11
    if (Build.MODEL.contains("K11", ignoreCase = true)) {
        return printEndOfDayK11(context, this)
    }

//    if (Build.MODEL.equals("mini", true) || Build.MODEL.equals("p5", true)) {
//        return Single.error(Throwable("Device cannot print"))
//    }

    val printerManager = NetPosSdk.getPrinterManager(context).apply {
        if (DeviceConfig.Device == DeviceConfig.DEVICE_PRO) {
            try {
                setPrintGray(Integer.valueOf("5000"))
                setLineSpace(Integer.valueOf("1"))
                cleanCache()
            } catch (e: java.lang.Exception) {
                e.printStackTrace()
                Timber.e("Error: ${e.localizedMessage}")
            }
        }
    }
    val textPrintLine = TextPrintLine().apply {
        type = TextPrintLine.TEXT
    }
    var amountApproved: Long = 0
    var amountDeclined: Long = 0

    val bitmapPrintLine = BitmapPrintLine()
    bitmapPrintLine.type = PrintLine.BITMAP
    bitmapPrintLine.position = PrintLine.CENTER
    val bitmap: Bitmap =
        BitmapFactory.decodeResource(context.resources, R.drawable.ic_print_logo)
    bitmapPrintLine.bitmap = Bitmap.createScaledBitmap(bitmap, 180, 120, false)
    printerManager.addPrintLine(bitmapPrintLine)

    var emitter: SingleEmitter<PrinterResponse>? = null

    textPrintLine.apply {
        isBold = false
        content = "EOD FOR: ${
        SimpleDateFormat(
            "yyyy-MM-dd",
            Locale.getDefault()
        ).format(this@printEndOfDay.first().transactionTimeInMillis)
        }"
    }
    printerManager.appendTextEntity(textPrintLine)

    textPrintLine.apply {
        isBold = false
        content = "Terminal ID: ${this@printEndOfDay.first().terminalId}"
    }
    printerManager.appendTextEntity(textPrintLine)

    textPrintLine.apply {
        isBold = false
        content = "MID: ${this@printEndOfDay.first().merchantId}"
    }
    printerManager.appendTextEntity(textPrintLine)

    // New EOD implementation starts here
    drawStraightLine(printerManager, textPrintLine)

    textPrintLine.apply {
        content = STRING_EOD_TITLE_HEADER
    }
    printerManager.appendTextEntity(textPrintLine)
    textPrintLine.apply {
        position = PrintLine.LEFT
        content = "-----------------------------------------------"
    }
    printerManager.appendTextEntity(textPrintLine)

    forEachIndexed { index, it ->
        if (it.responseCode == "00") amountApproved =
            amountApproved.plus(it.amount) else amountDeclined =
            amountDeclined.plus(it.amount)
        newEndOfDayPrintImplementation(
            index,
            it,
            printerManager,
            textPrintLine
        )
    }
    // New EOD implementation ends here

    textPrintLine.apply {
        position = PrintLine.CENTER
        content = "\nSUMMARY"
    }
    printerManager.appendTextEntity(textPrintLine)
    textPrintLine.apply {
        position = PrintLine.LEFT
        content = "-----------------------------------------------"
    }
    printerManager.appendTextEntity(textPrintLine)

    textPrintLine.apply {
        content = "Approved: ${amountApproved.div(100).formatCurrencyAmount("\u20A6")}"
    }
    printerManager.appendTextEntity(textPrintLine)
    textPrintLine.apply {
        content = "Declined: ${amountDeclined.div(100).formatCurrencyAmount("\u20A6")}"
    }
    printerManager.appendTextEntity(textPrintLine)

    textPrintLine.apply {
        content = "\n\n\n\n"
    }
    printerManager.appendTextEntity(textPrintLine)

    Timber.e("Approved: ${amountApproved.div(100).formatCurrencyAmount("\u20A6")}")
    Timber.e("Declined: ${amountDeclined.div(100).formatCurrencyAmount("\u20A6")}")

    val printerListener = object : POIPrinterManage.IPrinterListener {
        override fun onError(p0: Int, p1: String?) {
            emitter?.let {
                if (it.isDisposed.not()) {
                    it.onError(Throwable("message: $p1 - code: $p0"))
                }
            }
        }

        override fun onFinish() {
            emitter?.let {
                if (it.isDisposed.not()) {
                    it.onSuccess(PrinterResponse())
                }
            }
        }

        override fun onStart() {
            Timber.e("Printing started")
        }
    }
    // printerManager.addPrintLine(listOfTextPrintLine)
    return Single.create {
        emitter = it
        printerManager.beginPrint(printerListener)
    }
}

private fun printEndOfDayK11(
    context: Context,
    transactions: List<TransactionResponse>
): Single<PrinterResponse> {
    return Single.create { emitter ->
        try {
            // Use the IAidlPrinter interface as defined in your DeviceHelper
            val printer: IAidlPrinter? = DeviceHelper.getPrinter()

            if (printer == null) {
                emitter.onError(Throwable("K11 Printer Service not initialized"))
                return@create
            }

            // 1. Initialize
//            printer.init()

            // 2. Add Image (Logo)
            val bitmap = BitmapFactory.decodeResource(context.resources, R.drawable.ic_print_logo)
            val scaledBitmap = Bitmap.createScaledBitmap(bitmap, 180, 120, false)
            // IAidlPrinter usually takes (align, bitmap)
//            printer.addImage(1, scaledBitmap) // 1 is usually CENTER

            // 3. Add Text (Header)
            // IAidlPrinter.addText takes a Bundle for formatting and the String
            val format = Bundle().apply {
                putInt("font", 2) // 0: small, 1: normal, 2: large
                putBoolean("bold", true)
                putInt("align", 1) // 0: left, 1: center, 2: right
            }

//            printer.addText(format, "END OF DAY\n")

            format.putInt("font", 1)
            format.putBoolean("bold", false)
            format.putInt("align", 0)

//            printer.addText(format, "--------------------------------\n")

            // 4. Loop Transactions
            transactions.forEach { trans ->
                val status = if (trans.responseCode == "00") "A" else "D"
                val line = "${trans.RRN}  $status  ${trans.amount}\n"
//                printer.addText(format, line)
            }

            // 5. Feed paper (pixels)
//            printer.paperSkip(100)

            // 6. Execute Print
//            printer.print(object : AidlPrinterListener.Stub() {
//                override fun onPrintFinish() {
//                    emitter.onSuccess(PrinterResponse())
//                }
//
//                override fun onError(code: Int) {
//                    emitter.onError(Throwable("Print Error: $code"))
//                }
//            })

        } catch (e: Exception) {
            emitter.onError(e)
        }
    }
}

fun POIPrinterManage.appendTextEntity(printLine: TextPrintLine) {
    addPrintLine(printLine)
}

fun List<TransactionResponse>.printAll(
    context: Context,
    isMerchantCopy: Boolean
): Observable<PrinterResponse> {
    var emitter: ObservableEmitter<PrinterResponse>? = null
    val printerListener = object : POIPrinterManage.IPrinterListener {
        override fun onError(p0: Int, p1: String?) {
            emitter?.let {
                if (it.isDisposed.not()) {
                    it.onError(Throwable("message: $p1 - code: $p0"))
                }
            }
        }

        override fun onFinish() {
            emitter?.let {
                if (it.isDisposed.not()) {
                    it.onNext(PrinterResponse())
                }
            }
        }

        override fun onStart() {
            Timber.e("Printing started")
        }
    }
    return Observable.create {
        emitter = it
        forEach { transactionResponse ->
            if (it.isDisposed.not()) {
                transactionResponse.print(printerListener, context, isMerchantCopy)
            }
        }
        it.onComplete()
    }
}

//fun TransactionResponse.print(
//    printerListener: POIPrinterManage.IPrinterListener,
//    context: Context,
//    isMerchantCopy: Boolean = true
//) {
//    buildReceipt(context, isMerchantCopy).print(printerListener)
//}

//fun TransactionResponse.print(
//    context: Context,
//    remark: String? = null,
//    isMerchantCopy: Boolean = false,
//    isReprint: Boolean = false
//): Single<PrinterResponse> =
//    buildReceipt(
//        remark = remark,
//        context = context,
//        isMerchantCopy = isMerchantCopy,
//        isReprint = isReprint
//    ).print()

fun TransactionResponse.print(
    printerListener: POIPrinterManage.IPrinterListener,
    context: Context,
    isMerchantCopy: Boolean = true
) {
    // 1. Intercept K11 devices
    if (Build.MODEL.contains("K11", ignoreCase = true)) {
        printerListener.onStart()
        K11ReceiptPrinter.printReceipt(context, this, isMerchantCopy) { success, message ->
            if (success) {
                printerListener.onFinish()
            } else {
                printerListener.onError(-1, message)
            }
        }
        return
    }

    // 2. Fallback for old devices
    buildReceipt(context, isMerchantCopy).print(printerListener)
}

fun TransactionResponse.print(
    context: Context,
    remark: String? = null,
    isMerchantCopy: Boolean = false,
    isReprint: Boolean = false
): Single<PrinterResponse> {

    // 1. Intercept K11 devices and wrap the callback in RxJava Single
    if (Build.MODEL.contains("K11", ignoreCase = true)) {
        return Single.create { emitter ->
            K11ReceiptPrinter.printReceipt(context, this, isMerchantCopy) { success, message ->
                if (success) {
                    if (!emitter.isDisposed) emitter.onSuccess(PrinterResponse())
                } else {
                    if (!emitter.isDisposed) emitter.onError(Throwable(message))
                }
            }
        }
    }

    // 2. Fallback for old devices
    return buildReceipt(
        remark = remark,
        context = context,
        isMerchantCopy = isMerchantCopy,
        isReprint = isReprint
    ).print()
}

fun TransactionResponse.builder() = StringBuilder().apply {
    append("Merchant Name: ").append(Singletons.getCurrentlyLoggedInUser()!!.business_name)
    append("\nTERMINAL ID: ").append(terminalId).append("\n")
    append("Account Type: ${accountType.name}\n")
    append("Transaction Type: $transactionType").append("\n")
    append("DATE/TIME: ").append("\n${transactionTimeInMillis.formatDate()}").append("\n")
    append("AMOUNT: ").append(amount.div(100).formatCurrencyAmount("\u20A6")).append("\n")
    append(cardLabel).append(" Ending with ").append(maskedPan.substring(maskedPan.length - 4))
        .append("\n")
    append("RESPONSE CODE: ").append(responseCode).append("\nResponse Message").append(
        " : ${
        try {
            responseMessage
        } catch (ex: Exception) {
            "Error"
        }
        }"
    )
}

fun TransactionResponse.buildSMSText(s: String? = null): StringBuilder = StringBuilder().apply {
    append("POS $transactionType ${if (responseCode == "00") "Approved" else "Declined"}\n\n")
    if (!Singletons.getCurrentlyLoggedInUser()?.business_address.isNullOrEmpty()) {
        append("Merchant Address: ${Singletons.getCurrentlyLoggedInUser()?.business_address}\n")
    }
    if (!Singletons.getCurrentlyLoggedInUser()?.business_phone_number.isNullOrEmpty()) {
        append("Merchant Phone Number: ${Singletons.getCurrentlyLoggedInUser()?.business_phone_number}\n")
    }
    append("Response Code: $responseCode\n")
    append(
        "Message: ${
        try {
            responseMessage
        } catch (e: java.lang.Exception) {
            ""
        }
        }\n"
    )
    append("Amount: ${amount.div(100).formatCurrencyAmount("\u20A6")}\n")
    append("Date/Time: \n${transactionTimeInMillis.formatDate()}\n")
    s?.let {
        append("Remark: $it\n")
    }
    append("Auth Code: $authCode\n")
    append("RRN: $RRN\n")
    append("STAN: $STAN\n")
    append("Card: $cardLabel - $maskedPan\n")
    append("Card Owner: $cardHolder\n")
    append("Merchant: ${Singletons.getCurrentlyLoggedInUser()?.business_name}\n")
    append("Terminal ID: $terminalId\n")
}
fun TransactionResponse.buildMpgsText(transactionResponse: TransactionResponse, s: String? = null): StringBuilder = StringBuilder().apply {
    append("POS ${transactionResponse.transactionType} ${if (transactionResponse.responseCode == "00") "Approved" else "Declined"}\n\n")
    if (!Singletons.getCurrentlyLoggedInUser()?.business_address.isNullOrEmpty()) {
        append("Merchant Address: ${Singletons.getCurrentlyLoggedInUser()?.business_address}\n")
    }
    if (!Singletons.getCurrentlyLoggedInUser()?.business_phone_number.isNullOrEmpty()) {
        append("Merchant Phone Number: ${Singletons.getCurrentlyLoggedInUser()?.business_phone_number}\n")
    }
    append("Response Code: ${transactionResponse.responseCode} \n")
    append("Amount: ${transactionResponse.amount.div(100).formatCurrencyAmount("\u20A6")}\n")
    append("Date/Time: \n${transactionResponse.transactionTimeInMillis.formatDate()}\n")
    s?.let {
        append("Remark: $it\n")
    }
    append("RRN: ${transactionResponse.RRN}\n")
    append("Card: ${transactionResponse.cardLabel} - ${maskPan(transactionResponse.maskedPan)}\n")
    append("Merchant: ${Singletons.getCurrentlyLoggedInUser()?.business_name}\n")
    append("Terminal ID: ${transactionResponse.terminalId}\n")
}

fun maskPan(pan: String, maskChar: Char = '*'): String {
    val visibleDigits = 4
    val maskedLength = pan.length - visibleDigits
    val maskedPart = maskChar.toString().repeat(maskedLength)
    val visiblePart = pan.takeLast(visibleDigits)
    return "$maskedPart$visiblePart"
}

fun TransactionResponse.buildReceipt(
    context: Context,
    isMerchantCopy: Boolean = false,
    remark: String? = null,
    isReprint: Boolean = false
) =
    ReceiptBuilder(
        NetPosSdk.getPrinterManager(context).apply {
            cleanCache()
            setPrintGray(2000)
            setLineSpace(1)
        }
    ).also { builder ->
        builder.appendLogo(
            BitmapFactory.decodeResource(
                context.resources,
                R.drawable.ic_print_logo
            )
        )
        if (AID.isNotEmpty()) builder.appendAID(AID)

        Timber.d("DATA_ADDRESS_IN_PRINT_1=====>${Singletons.getCurrentlyLoggedInUser()?.business_address ?: "IRO NI O"}")
        Singletons.getCurrentlyLoggedInUser()?.business_address?.let {
            Timber.d("DATA_ADDRESS_IN_PRINT=====>$it")
            builder.appendMerchantAddress(it)
        }
        builder.appendMerchantName(Singletons.getCurrentlyLoggedInUser()!!.business_name)
        builder.appendAmount(
            amount.div(100).formatCurrencyAmount("\u20A6")
        )
        remark?.let {
            if (it.isNotEmpty()) builder.appendRemark(it)
        }
        builder.appendAppName("App Version: ${BuildConfig.FLAVOR.uppercase()}")
        builder.appendAppVersion(BuildConfig.VERSION_NAME)
        builder.appendAuthorizationCode("$authCode \nPhone Number: ${Singletons.getCurrentlyLoggedInUser()?.business_phone_number}")
        builder.appendCardHolderName("Card Holder: $cardHolder")
        builder.appendCardNumber("Masked Pan: $maskedPan")
        builder.appendCardScheme("Card Type: $cardLabel")
        builder.appendDateTime("\n${transactionTimeInMillis.formatDate()}")
        builder.appendRRN("$RRN \nAccount Type: ${accountType.name}")
        if (STAN.isNotEmpty()) builder.appendStan(STAN)
        builder.appendTerminalId(terminalId)
        builder.appendTransactionType(transactionType.name)
        builder.appendTransactionStatus(if (responseCode == "00") "Approved" else "Declined")
        builder.appendResponseCode(
            "${responseCode}\nMessage: ${
            try {
                responseMessage
            } catch (ex: Exception) {
                "Error"
            }
            }"
        )
        if (!Singletons.getCurrentlyLoggedInUser()?.business_address.isNullOrEmpty()) {
            builder.appendMerchantAddress("Merchant Address: ${Singletons.getCurrentlyLoggedInUser()?.business_address}")
        }
        builder.isReprint = isReprint
        if (isMerchantCopy) {
            builder.isMerchantCopy
        } else builder.isCustomerCopy
    }

fun NipNotification.print(context: Context, printerListener: POIPrinterManage.IPrinterListener) {
    buildNipReceipt(context).print(printerListener)
}

fun NipNotification.print(context: Context): Single<PrinterResponse> {
    return buildNipReceipt(context).print()
}

fun NipNotification.buildNipReceipt(context: Context): ReceiptBuilder =
    ReceiptBuilder(
        NetPosSdk.getPrinterManager(context).apply {
            cleanCache()
            setPrintGray(2000)
            setLineSpace(1)
        }
    ).apply {
        appendLogo(BitmapFactory.decodeResource(context.resources, R.drawable.ic_print_logo))
        appendTextEntityFontSixteenCenter("BANK TRANSFER")
        appendTextEntity("\nBeneficiary Account Number: $beneficiaryAccountNumber")
        appendTextEntity("Source Name: $sourceName")
        appendTextEntity("Source Account Number: $sourceAccountNumber")
        appendTextEntity(
            "Amount: \u20A6${this@buildNipReceipt.amount}"
        )
        appendTextEntity("Date: $createdAt")
    }

fun List<NipNotification>.printAllNotifications(context: Context): Observable<PrinterResponse> {
    var emitter: ObservableEmitter<PrinterResponse>? = null
    val printerListener = object : POIPrinterManage.IPrinterListener {
        override fun onError(p0: Int, p1: String?) {
            emitter?.let {
                if (it.isDisposed.not()) {
                    it.onError(Throwable("message:$p1 - code:$p0"))
                }
            }
        }

        override fun onFinish() {
            emitter?.let {
                if (it.isDisposed.not()) {
                    it.onNext(PrinterResponse())
                }
            }
        }

        override fun onStart() {
            Timber.e("Printing started")
        }
    }
    return Observable.create {
        emitter = it
        forEach { nipNotification ->
            if (it.isDisposed.not()) {
                nipNotification.print(context, printerListener)
            }
        }
        it.onComplete()
    }
}
