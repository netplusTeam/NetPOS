package com.woleapp.netpos.model

data class PayWithQrRequest(
    val terminalId: String,
    val netposId: String,
    val amount: String,
    val name: String,
    val email: String,
    val bank: String,
    val NPmerchantId: String
)
