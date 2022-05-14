package com.woleapp.netpos.model

data class TransactionResponseModelFromGateWay(
    val RRN: String,
    val accountType: String,
    val acquiringInstCode: String,
    val additionalAmount: Any,
    val agentName: String,
    var amount: Double,
    val authCode: String,
    val cardExpiry: String,
    val cardHolder: String,
    val cardLabel: String,
    val maskedPan: String,
    val merchantId: String,
    val merchantName: String,
    val originalForwardingInstCode: String,
    val remark: String,
    val responseCode: String,
    val responseMessage: String,
    val source: String,
    val terminalId: String,
    val total: Int,
    val transactionTime: String,
    val transactionType: String
)
