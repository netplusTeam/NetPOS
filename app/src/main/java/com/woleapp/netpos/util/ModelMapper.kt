package com.woleapp.netpos.util

import android.util.Log
import com.danbamitale.epmslib.entities.*
import com.danbamitale.epmslib.utils.IsoAccountType
import com.woleapp.netpos.model.Row
import com.woleapp.netpos.model.TransactionResponseModelFromGateWay
import com.woleapp.netpos.util.RandomNumUtil.formattedTime
import com.woleapp.netpos.util.RandomNumUtil.getDateInMillis

object ModelMapper {
    fun mapTransFromGateWayToEntity(trans: List<TransactionResponse>) =
        trans.map {
            TransactionResponseModelFromGateWay(
                RRN = it.RRN,
                accountType = it.accountType.name,
                acquiringInstCode = it.acquiringInstCode,
                additionalAmount = it.additionalAmount.toString().toLong(),
                agentName = "",
                amount = it.amount.toDouble(),
                authCode = it.authCode,
                cardExpiry = it.cardExpiry,
                cardHolder = it.cardHolder,
                cardLabel = it.cardLabel,
                maskedPan = it.maskedPan,
                merchantId = it.merchantId,
                merchantName = "",
                originalForwardingInstCode = it.originalForwardingInstCode,
                remark = "",
                responseCode = it.responseCode,
                responseMessage = it.responseMessage,
                source = "",
                terminalId = it.terminalId,
                total = 0,
                transactionTime = it.transmissionDateTime,
                transactionType = it.transactionType.name
            )
        }

    fun mapEntityToTransFromGateWay(trans: List<TransactionResponseModelFromGateWay>) =
        trans.map {
            Log.d("TRANS_MILLIS", it.transactionTime)
            Log.d("TRANS_MILLISD", it.transactionTime)
            println("TRANS_MILLIS" + it.transactionTime)
            TransactionResponse().apply {
                RRN = it.RRN
                accountType = IsoAccountType.parseStringAccountType(it.accountType)
                acquiringInstCode = it.acquiringInstCode
                additionalAmount_54 =
                    if (it.additionalAmount != null) it.additionalAmount.toString() else ""
                amount = it.amount.toLong()
                authCode = it.authCode
                cardExpiry = it.cardExpiry
                cardHolder = it.cardHolder
                cardLabel = it.cardLabel
                errorMessage = it.responseMessage
                maskedPan = it.maskedPan
                originalForwardingInstCode = it.originalForwardingInstCode
                responseCode = it.responseCode
                terminalId = it.terminalId
                transactionTimeInMillis =
                    if (it.transactionTime.contains("-")) getDateInMillis(it.transactionTime) else it.transactionTime.toLong()
                transactionType = TransactionType.valueOf(it.transactionType)
                transmissionDateTime = it.transactionTime
            }
        }

    fun List<Row>.mapRowToTransactionResponse() =
        map {
            TransactionResponse().apply {
                RRN = it.rrn ?: ""
                accountType =
                    it.accountType?.let { it1 -> IsoAccountType.parseStringAccountType(it1) }
                        ?: IsoAccountType.DEFAULT_UNSPECIFIED
                acquiringInstCode = it.acquiringInstCode ?: ""
                additionalAmount_54 =
                    if (it.additionalAmount != null) it.additionalAmount.toString() else ""
                amount =
                    if (it.amount is Int) (it.amount as Int).toLong() else (it.amount as Double).toLong()
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
                    if (it.transactionTime?.contains("-") == true) getDateInMillis(it.transactionTime!!) else it.transactionTime?.toLong()
                        ?: 0L
                transactionType = it.transactionType?.let { it1 -> TransactionType.valueOf(it1) }
                    ?: TransactionType.PURCHASE
                transmissionDateTime = it.transactionTime ?: ""
            }
        }

    fun TransactionRequestData.mapRequestDataToTransactionResponse(
        cardData: CardData,
        cardHolderName: String,
        cardScheme: String,
        errorMessage: String,
        transTimeInMillis: Long,
        responseCode: String?
    ): TransactionResponse =
        TransactionResponse().apply {
            this.RRN = this@mapRequestDataToTransactionResponse.RRN ?: ""
            accountType =
                this@mapRequestDataToTransactionResponse.accountType
            acquiringInstCode =
                this@mapRequestDataToTransactionResponse.originalDataElements?.originalAcquiringInstCode
                    ?: ""
            additionalAmount_54 = ""
            amount = this@mapRequestDataToTransactionResponse.amount
            authCode =
                this@mapRequestDataToTransactionResponse.originalDataElements?.originalAuthorizationCode
                    ?: ""
            cardExpiry = cardData.expiryDate
            cardHolder = cardHolderName
            cardLabel = cardScheme
            this.errorMessage = errorMessage
            this.maskedPan = cardData.pan
            originalForwardingInstCode =
                this@mapRequestDataToTransactionResponse.originalDataElements?.originalForwardingInstCode
                    ?: ""
            this.responseCode = responseCode
                ?: this@mapRequestDataToTransactionResponse.originalDataElements?.reversalReasonCode?.code
                ?: ""
            terminalId = Singletons.getCurrentlyLoggedInUser()?.terminal_id ?: ""
            transactionTimeInMillis = transTimeInMillis
            transactionType = this@mapRequestDataToTransactionResponse.transactionType
            transmissionDateTime = formattedTime
        }
}
