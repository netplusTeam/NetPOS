package com.woleapp.netpos.model

data class MerchantDetailsResponse(
    val user: MerchantDetail
)

data class MerchantDetail(
    val acctNumber: String,
    val bank: String,
    val businessName: String,
    val merchantId: String,
    val partnerId: String,
    val terminalId: String
)
