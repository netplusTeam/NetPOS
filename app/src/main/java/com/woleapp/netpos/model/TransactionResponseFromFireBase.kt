package com.woleapp.netpos.model

data class TransactionResponseFromFireBase(
    var amount: String,
    var depositorAccountName: String?,
    var depositorBankName: String?,
    var transactionDateTime: String?
)
