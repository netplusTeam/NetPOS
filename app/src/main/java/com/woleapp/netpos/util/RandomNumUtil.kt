package com.woleapp.netpos.util

import android.annotation.SuppressLint
import android.os.Build
import android.text.Html
import android.text.Spanned
import com.danbamitale.epmslib.entities.TransactionRequestData
import com.danbamitale.epmslib.entities.TransactionResponse
import com.danbamitale.epmslib.utils.IsoTimeManager
import com.woleapp.netpos.model.MakePaymentParams
import com.woleapp.netpos.model.TransactionResponseX
import com.woleapp.netpos.model.TransactionToLogBeforeConnectingToNibbs
import timber.log.Timber
import java.text.DateFormat
import java.text.DecimalFormat
import java.text.ParseException
import java.text.SimpleDateFormat
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.*

object RandomNumUtil {

    @SuppressLint("SimpleDateFormat")
    fun getDateInMillis(dateTime: String): Long {
        val format = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'.000Z'")
        val date = format.parse(dateTime)
        return (date!!.time) + 3600000
    }

    fun Number.formatAmount(): String {
        return DecimalFormat("#.##.00").format(this)
    }

    fun Number.formatCurrencyAmountUsingCurrentModule(currencySymbol: String = "\u20A6"): String {
        val format = DecimalFormat("#,###.00")
        return "$currencySymbol${format.format(this)}"
    }

    @SuppressLint("SimpleDateFormat")
    fun getDateInTheFormatExpectedByTheNewService(
        date: String
    ): String {
        val initDate = SimpleDateFormat("dd:MM:yyyy hh:mm:ss").parse(date) ?: Date()
        val formatter = SimpleDateFormat("yyyy-MM-dd")
        return formatter.format(initDate) + " 00:00:00"
    }

    @SuppressLint("SimpleDateFormat")
    fun getDateInTheFormatExpectedByTheNewServiceForEnd(
        date: String
    ): String {
        val initDate = SimpleDateFormat("dd:MM:yyyy hh:mm:ss").parse(date) ?: Date()
        val formatter = SimpleDateFormat("yyyy-MM-dd")
        return formatter.format(initDate) + " 23:59:59"
    }
    @SuppressLint("SimpleDateFormat")
    fun getDateInMillis3(dateTime: String): Long {
        val format = SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
        val date = format.parse(dateTime)
        return date!!.time + 3600000
    }

    @SuppressLint("SimpleDateFormat")
    fun getDateInMillis2(dateTime: String): Long {
        val formatter: DateTimeFormatter =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss", Locale.ENGLISH)
        val localDate: LocalDateTime = LocalDateTime.parse(dateTime, formatter)
        return localDate.atOffset(ZoneOffset.UTC).toInstant().toEpochMilli() + 3600000
    }

    fun dateStr2Long(dateStr: String): Long {
        return try {
            val c = Calendar.getInstance()
            c.time = SimpleDateFormat("yyyy-MM-dd hh:mm")
                .parse(dateStr)!!
            c.timeInMillis
        } catch (e: ParseException) {
            e.printStackTrace()
            0
        }
    }

    @SuppressLint("SimpleDateFormat")
    fun getStartOfDayAsString(): String {
        val dateFormatter: DateFormat = SimpleDateFormat("yyyy-MM-dd")
        val today = Date()
        return dateFormatter.format(today) + " 00:00:00"
    }

    fun getDateInMillisFromZenithPbtTransDate(dateTime: String): Long {
        val format = SimpleDateFormat("yyyy-MM-dd hh:mm:ss")
        val date = format.parse(dateTime.replace("T", " ").removeSuffix(".000Z"))
        return date!!.time
    }

    fun getDateFromZenithPbtTransDate(dateTime: String): String {
        val format = SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
        return dateTime.replace("T", " ").removeSuffix(".000Z")
    }

    fun formatHtml(htmlText: String): Spanned? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            Html.fromHtml(htmlText, Html.FROM_HTML_MODE_COMPACT)
        } else {
            Html.fromHtml(htmlText)
        }
    }

    @SuppressLint("SimpleDateFormat")
    fun getDateInMilliSecsForLocal(date: String): Long {
        val dateFormatter = SimpleDateFormat("dd:MM:yyyy HH:mm:ss")
        val newDate = date.split(" ")[0] + " 00:00:00"
        Timber.d("DIS_TIME%s", newDate.toString())
        return dateFormatter.parse(newDate)!!.time
    }

    @SuppressLint("SimpleDateFormat")
    fun getDateInMilliSecsForLocalForEndOfDay(date: String): Long {
        val dateFormatter = SimpleDateFormat("dd:MM:yyyy HH:mm:ss")
        val newDate = date.split(" ")[0] + " 23:59:59"
        Timber.d("DIS_TIME_end%s", newDate)
        return dateFormatter.parse(newDate)!!.time
    }

    @SuppressLint("SimpleDateFormat")
    fun getEndOfDayAsString(): String {
        val dateFormatter: DateFormat = SimpleDateFormat("yyyy-MM-dd")
        val today = Date()
        return dateFormatter.format(today) + " 23:59:59"
    }

    fun generateRandomRrn(length: Int): String {
        val random = Random()
        var digits = ""
        digits += (random.nextInt(9) + 1).toString()
        for (i in 1 until length) {
            digits += (random.nextInt(10) + 0).toString()
        }
        return digits
    }

    val formattedTime =
        SimpleDateFormat("hh:mm:ss", Locale.getDefault()).format(System.currentTimeMillis())
            .format(Date())

    @SuppressLint("SimpleDateFormat")
    fun getDate(): String {
        val dateFormatter: DateFormat = SimpleDateFormat("ddMM")
        val today = Date()
        return dateFormatter.format(today)
    }

    @SuppressLint("SimpleDateFormat")
    fun getCurrentDateTime(): String {
        val dateFormatter: DateFormat = SimpleDateFormat("yyyy-MM-dd hh:mm:ss")
        val today = Date()
        return dateFormatter.format(today)
    }

    @SuppressLint("SimpleDateFormat")
    fun getCurrentDate(): String {
        val dateFormatter: DateFormat = SimpleDateFormat("yyyy-MM-dd")
        val today = Date()
        return dateFormatter.format(today)
    }

    fun mapTransactionResponse(transResp: TransactionResponse) =
        "CardHolder: ${transResp.cardHolder} " +
            "\nCardType: ${transResp.cardLabel}" +
            "\nAmount: ${transResp.amount} " +
            "\nRRN: ${transResp.RRN} \n" +
            "ResponseCode: ${transResp.responseCode} \n" +
            "TransmissionDateTime: ${transResp.transmissionDateTime} \n" +
            "Time in Millis: ${transResp.transactionTimeInMillis}"

    @SuppressLint("SimpleDateFormat")
    fun getDate(milliSeconds: Long): String {
        val formatter = SimpleDateFormat("yyyy-MM-dd hh:mm:ss")

        val calendar = Calendar.getInstance()
        calendar.timeInMillis = milliSeconds
        return formatter.format(calendar.time)
    }

    fun mapDanbamitaleResponseToResponseX(input: TransactionResponse): TransactionResponseX {
        with(input) {
            return TransactionResponseX(
                AID = AID,
                rrn = RRN,
                STAN = STAN,
                TSI = TSI,
                TVR = TVR,
                accountType = accountType.name,
                acquiringInstCode = acquiringInstCode,
                additionalAmount_54 = additionalAmount_54,
                amount = amount.toInt(),
                appCryptogram = appCryptogram,
                authCode = authCode,
                cardExpiry = cardExpiry,
                cardHolder = cardHolder,
                cardLabel = cardLabel,
                id = id.toInt(),
                localDate_13 = localDate_13,
                localTime_12 = localTime_12,
                maskedPan = maskedPan,
                merchantId = merchantId,
                originalForwardingInstCode = originalForwardingInstCode,
                otherAmount = otherAmount.toInt(),
                otherId = otherId,
                responseCode = responseCode,
                responseDE55 = responseDE55 ?: "",
                terminalId = terminalId,
                transactionTimeInMillis = transactionTimeInMillis,
                transactionType = transactionType.name,
                transmissionDateTime = getCurrentDateTime()
            )
        }
    }

//    fun mapDanbamitaleResponseToResponseWithRrn(
//        input: TransactionResponse,
//        remark: String
//    ): TransactionWithRemark {
//        with(input) {
//            return TransactionWithRemark(
//                AID = AID,
//                rrn = RRN,
//                STAN = STAN,
//                TSI = TSI,
//                TVR = TVR,
//                accountType = accountType.name,
//                acquiringInstCode = acquiringInstCode,
//                additionalAmount_54 = additionalAmount_54,
//                amount = amount.toInt(),
//                appCryptogram = appCryptogram,
//                authCode = authCode,
//                cardExpiry = cardExpiry,
//                cardHolder = cardHolder,
//                cardLabel = cardLabel,
//                id = id.toInt(),
//                localDate_13 = localDate_13,
//                localTime_12 = localTime_12,
//                maskedPan = maskedPan,
//                merchantId = merchantId,
//                originalForwardingInstCode = originalForwardingInstCode,
//                otherAmount = otherAmount.toInt(),
//                otherId = otherId,
//                responseCode = responseCode,
//                responseDE55 = responseDE55 ?: "",
//                responseMessage = responseMessage,
//                terminalId = terminalId,
//                transactionTimeInMillis = transactionTimeInMillis,
//                transactionType = transactionType.name,
//                transmissionDateTime = RandomNumUtil.getCurrentDateTime(),
//                remark = remark
//            )
//        }
//    }
//
//    fun toTransactionResponse(input: TransactionWithRemark) =
//        TransactionResponse().apply {
//            AID = input.AID
//            this.RRN = input.rrn
//            STAN = input.STAN
//            TSI = input.TSI
//            TVR = input.TVR
//            accountType = IsoAccountType.valueOf(input.accountType)
//            acquiringInstCode = input.acquiringInstCode
//            additionalAmount_54 = input.additionalAmount_54
//            amount = input.amount.toLong()
//            appCryptogram = input.appCryptogram
//            authCode = input.authCode
//            cardExpiry = input.cardExpiry
//            cardHolder = input.cardHolder
//            cardLabel = input.cardLabel
//            id = input.id.toLong()
//            localDate_13 = input.localDate_13
//            localTime_12 = input.localTime_12
//            maskedPan = input.maskedPan
//            merchantId = input.merchantId
//            originalForwardingInstCode = input.originalForwardingInstCode
//            otherAmount = input.otherAmount.toLong()
//            otherId = input.otherId
//            responseCode = input.responseCode
//            responseDE55 = input.responseDE55 ?: ""
//            terminalId = input.terminalId
//            transactionTimeInMillis = input.transactionTimeInMillis
//            transactionType = TransactionType.valueOf(input.transactionType)
//            transmissionDateTime = RandomNumUtil.getCurrentDateTime()
//        }

    fun getCustomRrn() = IsoTimeManager().fullDate.substring(2, 14)

    fun MakePaymentParams.getTransactionResponseToLog(
        cardScheme: String,
        requestData: TransactionRequestData,
        customerName: String,
        terminal_id: String,
        partnerId: String
    ) =
        this.cardData.expiryDate.let {
            TransactionToLogBeforeConnectingToNibbs(
                status = "PENDING",
                TransactionResponseX(
                    AID = "",
                    rrn = getCustomRrn(),
                    STAN = generateRandomRrn(6),
                    TSI = "",
                    TVR = "",
                    accountType = accountType.name,
                    acquiringInstCode = "",
                    additionalAmount_54 = "",
                    amount = amount.toInt(),
                    appCryptogram = "",
                    authCode = "",
                    cardExpiry = it,
                    cardHolder = customerName,
                    cardLabel = cardScheme.toString(),
                    id = 0,
                    localDate_13 = getDate(),
                    localTime_12 = formattedTime.replace(":", ""),
                    maskedPan = cardData.pan,
                    merchantId = partnerId,
                    originalForwardingInstCode = "",
                    otherAmount = requestData.otherAmount.toInt(),
                    otherId = "",
                    responseCode = "99",
                    responseDE55 = "",
                    terminalId = terminal_id,
                    transactionTimeInMillis = 0,
                    transactionType = requestData.transactionType.name,
                    transmissionDateTime = getCurrentDateTime()
                )
            )
        }
}
