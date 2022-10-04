package com.woleapp.netpos.model

import com.danbamitale.epmslib.entities.TransactionResponse
import com.danbamitale.epmslib.entities.TransactionType
import com.danbamitale.epmslib.utils.IsoAccountType
import com.woleapp.netpos.util.RandomNumUtil.getDateInMillis

data class Row2(
    val accountType: String?,
    val acquiringInstCode: String?,
    val amount: Double?,
    val appCryptogram: String?,
    val authCode: String?,
    val cardExpiry: String?,
    val cardHolder: String?,
    val cardLabel: String?,
    val dateCreated: String?,
    val maskedPan: String?,
    val merchantId: String?,
    val originalForwardingInstCode: String?,
    val otherAmount: Int?,
    val otherId: String?,
    val partnerId: String?,
    val provider: String?,
    val remark: String?,
    val responseCode: String?,
    val responseDE55: String?,
    val responseMessage: String?,
    val rrn: String?,
    val source: String?,
    val terminalId: String?,
    val transactionTime: String?,
    val transactionTimeInMillis: String?,
    val transactionType: String?,
    val transmissionDateTime: String?
)

fun List<RowX>.mapRowToTransactionResponse() =
    map {
        TransactionResponse().apply {
            RRN = it.rrn ?: ""
            accountType = it.accountType?.let { it1 ->
                return@let IsoAccountType.valueOf(it1)
            } ?: IsoAccountType.DEFAULT_UNSPECIFIED
            acquiringInstCode = it.acquiringInstCode ?: ""
            additionalAmount_54 = ""
            amount = it.amount?.toLong() ?: 0L
            authCode = it.authCode ?: ""
            cardExpiry = it.cardExpiry ?: ""
            cardHolder = it.cardHolder ?: ""
            cardLabel = it.cardLabel ?: ""
            errorMessage = it.responseMessage
            maskedPan = it.maskedPan ?: ""
            originalForwardingInstCode = it.originalForwardingInstCode ?: ""
            responseCode = it.responseCode ?: ""
            terminalId = it.terminalId ?: ""
            transactionTimeInMillis =
                if (it.transactionTime?.contains("-") == true) getDateInMillis(it.transactionTime) else it.transactionTime?.toLong()
                    ?: 0L
            transactionType = it.transactionType?.let { it1 ->
                TransactionType.valueOf(it1)
            } ?: TransactionType.PURCHASE
            transmissionDateTime = it.transactionTime ?: ""
        }
    }
