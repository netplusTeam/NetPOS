package com.woleapp.netpos.model

import com.danbamitale.epmslib.entities.TransactionResponse

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
    val transactionTimeInMillis: Int,
    val transactionType: String,
    val transmissionDateTime: String
)

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