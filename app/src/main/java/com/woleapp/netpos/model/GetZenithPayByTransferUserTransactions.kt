package com.woleapp.netpos.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.danbamitale.epmslib.entities.TransactionResponse
import com.danbamitale.epmslib.entities.TransactionType
import com.danbamitale.epmslib.utils.IsoAccountType
import com.woleapp.netpos.util.RandomNumUtil.getDateInMillisFromZenithPbtTransDate

data class GetZenithPayByTransferUserTransactions(
    val transactions: List<GetZenithPayByTransferUserTransactionsModel>
)

@Entity(tableName = "pbtTransaction")
data class GetZenithPayByTransferUserTransactionsModel(
    val amount: Int,
    val channel: String,
    val details: String,
    val merchantId: String,
    val paid_at: String,
    val partnerId: String,
    val payer_account_name: String,
    val payer_account_number: String,
    val payer_bank_code: String,
    val recipient_account_number: String,
    val terminalId: String,
    @PrimaryKey(autoGenerate = false)
    val transaction_reference: String,
    val type: String
)

fun GetZenithPayByTransferUserTransactionsModel.mapZenithPayByTransferToNormalTransaction(): TransactionResponse =
    TransactionResponse().apply {
        responseCode = "00"
        transactionType = TransactionType.TRANSFER
        AID = ""
        STAN = "N/A"
        TSI = ""
        TVR = ""
        accountType = IsoAccountType.SAVINGS
        acquiringInstCode = ""
        additionalAmount_54 = ""
        amount = this@mapZenithPayByTransferToNormalTransaction.amount.toLong()
        appCryptogram = "Not Applicable"
        authCode = "Not Applicable"
        cardExpiry = "Not Applicable"
        cardHolder = this@mapZenithPayByTransferToNormalTransaction.payer_account_name
        echoData = ""
        errorMessage = ""
        id = 0L
        interSwitchThreshold = 0L
        localDate_13 = ""
        localTime_12 = ""
        maskedPan = "Not applicable"
        merchantId = this@mapZenithPayByTransferToNormalTransaction.merchantId
        originalForwardingInstCode = this@mapZenithPayByTransferToNormalTransaction.payer_bank_code
        otherAmount = 0
        otherId = ""
        terminalId = this@mapZenithPayByTransferToNormalTransaction.terminalId
        RRN = this@mapZenithPayByTransferToNormalTransaction.transaction_reference
        transactionTimeInMillis =
            getDateInMillisFromZenithPbtTransDate(this@mapZenithPayByTransferToNormalTransaction.paid_at)
        transmissionDateTime =
            this@mapZenithPayByTransferToNormalTransaction.paid_at.replace("T", " ")
                .removeSuffix(".000Z")
    }
