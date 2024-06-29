package com.woleapp.netpos.model.pay

data class PayResponseErrorModel(
    val amount: String,
    val code: String,
    val message: String,
    val orderId: String,
    val result: String,
    val review_status: String,
    val status: String,
    val transId: String
)
