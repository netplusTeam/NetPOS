package com.woleapp.netpos.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.danbamitale.epmslib.entities.TransactionResponse
import com.danbamitale.epmslib.entities.TransactionType
import com.danbamitale.epmslib.utils.IsoAccountType

data class DataToLogAfterConnectingToNibss(
    val status: String,
    val transactionResponse: TransactionResponseX,
    val rrn: String
)

data class LogToBackendResponse(
    val `data`: List<Int>,
    val message: String,
    val status: String
)

data class ResponseBodyAfterLoginToBackend(
    val message: String
)

data class TransactionResponseX(
    val AID: String,
    val rrn: String,
    val STAN: String,
    val TSI: String,
    val TVR: String,
    val accountType: String,
    val acquiringInstCode: String,
    val additionalAmount_54: String,
    val amount: Int,
    val appCryptogram: String,
    val authCode: String,
    val cardExpiry: String,
    val cardHolder: String,
    val cardLabel: String,
    val id: Int,
    val localDate_13: String,
    val localTime_12: String,
    val maskedPan: String,
    val merchantId: String,
    val originalForwardingInstCode: String,
    val otherAmount: Int,
    val otherId: String,
    val responseCode: String,
    val responseDE55: String,
    val terminalId: String,
    val transactionTimeInMillis: Long,
    val transactionType: String,
    val transmissionDateTime: String
)

@Entity(tableName = "transactionTrackingTable")
data class TransactionResponseXForTracking(
    @PrimaryKey(autoGenerate = false)
    val temporalRRN: String,
    val transRespX: TransactionResponseX,
    val status: String
)

fun mapToTransactionResponse(transRespX: TransactionResponseX): TransactionResponse =
    TransactionResponse().apply {
        transactionType = TransactionType.PURCHASE
        maskedPan = transRespX.maskedPan
        amount = transRespX.amount.toLong()

        transmissionDateTime = transRespX.transmissionDateTime
        STAN = transRespX.STAN
        RRN = transRespX.rrn
        localTime_12 = transRespX.localTime_12
        localDate_13 = transRespX.localDate_13
        otherAmount = transRespX.otherAmount.toLong()
        acquiringInstCode = transRespX.acquiringInstCode
        originalForwardingInstCode = transRespX.originalForwardingInstCode
        authCode = transRespX.authCode
        responseCode = transRespX.responseCode
        additionalAmount_54 = transRespX.additionalAmount_54

        cardLabel = transRespX.cardLabel
        cardExpiry = transRespX.cardExpiry
        cardHolder = transRespX.cardHolder
        TVR = transRespX.TVR
        TSI = transRespX.TSI
        AID = transRespX.AID
        appCryptogram = transRespX.appCryptogram
        transactionTimeInMillis = transRespX.transactionTimeInMillis.toLong()
        accountType = IsoAccountType.valueOf(transRespX.accountType)

        terminalId = transRespX.terminalId
        merchantId = transRespX.merchantId
        otherId = transRespX.otherId
        responseDE55 = transRespX.responseDE55
    }

data class TransactionToLogAfterSuccessfulTransaction(
    val rrn: String,
    val status: String,
    val transactionResponse: TransactionResponse
)

data class TransactionToLogBeforeConnectInToNibss(
    val status: String,
    val trasnactionResponse: TransactionResponse
)

data class TransactionToLogBeforeConnectingToNibbs(
    val status: String,
    val transactionResponse: TransactionResponseX
)

data class TransactionToLogToBackEnd(
    val status: String,
    val trasnactionResponse: TransactionResponse
)
