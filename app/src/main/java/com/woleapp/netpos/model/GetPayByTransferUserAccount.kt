package com.woleapp.netpos.model

data class GetPayByTransferUserAccount(
    val user: GetPayByTransferUserAccountModel
)

data class GetPayByTransferUserAccountModel(
    val acctNumber: String,
    val businessName: String,
    val merchantId: String,
    val partnerId: String,
    val terminalId: String
)
