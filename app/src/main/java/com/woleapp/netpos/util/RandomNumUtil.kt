package com.woleapp.netpos.util

import android.annotation.SuppressLint
import com.danbamitale.epmslib.entities.TransactionResponse
import com.woleapp.netpos.model.TransactionResponseX
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.* // ktlint-disable no-wildcard-imports

object RandomNumUtil {

    @SuppressLint("SimpleDateFormat")
    fun getDateInMillis(dateTime: String): Long {
        val format = SimpleDateFormat("dd-MM-yyyy hh:mm:ss")
        val date = format.parse(dateTime)
        return date!!.time
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
                transactionTimeInMillis = transactionTimeInMillis.toInt(),
                transactionType = transactionType.name,
                transmissionDateTime = getCurrentDateTime()
            )
        }
    }
}
