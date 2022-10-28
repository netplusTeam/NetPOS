package com.woleapp.netpos.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.danbamitale.epmslib.entities.TransactionResponse
import com.danbamitale.epmslib.entities.TransactionType
import com.danbamitale.epmslib.utils.IsoAccountType
import com.woleapp.netpos.util.RandomNumUtil.formatCurrencyAmountUsingCurrentModule
import com.woleapp.netpos.util.RandomNumUtil.getDateFromZenithPbtTransDate
import com.woleapp.netpos.util.RandomNumUtil.getDateInMillisFromZenithPbtTransDate

data class GetZenithPayByTransferUserTransactions(
    val transactions: List<GetZenithPayByTransferUserTransactionsModel>
)

@Entity(tableName = "pbtTransaction")
data class GetZenithPayByTransferUserTransactionsModel(
    val amount: Double,
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
) {
    override fun toString(): String {
        return "<b>Amount: &nbsp;&nbsp;</b>${amount.formatCurrencyAmountUsingCurrentModule()} <br /><b>Payer's Name:</b><br /> $payer_account_name <br /><br /><b>Payer' Account Number: &nbsp;&nbsp;</b>$payer_account_number <br /><b>Paid at: &nbsp;&nbsp;</b> ${
        getDateFromZenithPbtTransDate(paid_at)
        }<br /><br /> <b>Transaction Details: </b><br />$details <br /><br /><b>Transaction Reference:  </b><br />$transaction_reference"
    }
}

fun GetZenithPayByTransferUserTransactionsModel.mapZenithPayByTransferToNormalTransaction(): TransactionResponse =
    TransactionResponse().apply {
        responseCode = "00"
        transactionType = TransactionType.TRANSFER
        AID = ""
        STAN = "N/A"
        TSI = payer_account_number
        TVR = type
        accountType = IsoAccountType.SAVINGS
        acquiringInstCode = ""
        additionalAmount_54 = recipient_account_number
        amount = this@mapZenithPayByTransferToNormalTransaction.amount.toLong()
        appCryptogram = this@mapZenithPayByTransferToNormalTransaction.details
        authCode = ""
        cardExpiry = "Not Applicable"
        cardHolder = this@mapZenithPayByTransferToNormalTransaction.payer_account_name
        echoData = channel
        errorMessage = ""
        id = 0L
        interSwitchThreshold = 0L
        localDate_13 = ""
        localTime_12 = paid_at
        maskedPan = "Not applicable"
        merchantId = this@mapZenithPayByTransferToNormalTransaction.merchantId
        originalForwardingInstCode = this@mapZenithPayByTransferToNormalTransaction.payer_bank_code
        otherAmount = 0
        otherId = partnerId
        terminalId = this@mapZenithPayByTransferToNormalTransaction.terminalId
        RRN = this@mapZenithPayByTransferToNormalTransaction.transaction_reference
        transactionTimeInMillis =
            getDateInMillisFromZenithPbtTransDate(this@mapZenithPayByTransferToNormalTransaction.paid_at)
        transmissionDateTime =
            this@mapZenithPayByTransferToNormalTransaction.paid_at.replace("T", " ")
                .removeSuffix(".000Z")
    }

fun TransactionResponse.mapToZenithPbtTransactionModel() =
    GetZenithPayByTransferUserTransactionsModel(
        amount.toDouble(),
        echoData ?: "",
        appCryptogram,
        merchantId,
        localTime_12,
        otherId,
        cardHolder,
        TSI,
        originalForwardingInstCode,
        additionalAmount_54,
        terminalId,
        RRN,
        TVR
    )
