package com.woleapp.netpos.util

import android.app.Activity
import android.content.Context
import android.text.Html
import android.text.Spanned
import androidx.fragment.app.Fragment
import androidx.lifecycle.LifecycleOwner
import com.danbamitale.epmslib.entities.* // ktlint-disable no-wildcard-imports
import com.danbamitale.epmslib.utils.IsoAccountType
import com.woleapp.netpos.model.GetZenithPayByTransferUserTransactionsModel
import com.woleapp.netpos.model.PayWithCardNotificationModelResponse
import com.woleapp.netpos.model.Row
import com.woleapp.netpos.model.TransactionResponseModelFromGateWay
import com.woleapp.netpos.util.RandomNumUtil.dateStr2Long
import com.woleapp.netpos.util.RandomNumUtil.dateStrToLong
import com.woleapp.netpos.util.RandomNumUtil.formattedTime
import com.woleapp.netpos.util.RandomNumUtil.getCurrentDateTime
import com.woleapp.netpos.util.RandomNumUtil.getDateInMillis
import com.woleapp.netpos.util.RandomNumUtil.getLocaleCurrentDateTime
import pub.devrel.easypermissions.EasyPermissions

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
                merchantName = it.transactionTimeInMillis.toString(),
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
            TransactionResponse().apply {
                RRN = it.RRN
                accountType = IsoAccountType.parseStringAccountType(it.accountType)
                acquiringInstCode = it.acquiringInstCode
                additionalAmount_54 =
                    it.additionalAmount.toString()
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
                transactionTimeInMillis = it.merchantName.toLong()
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
                localDate_13 = it.localDate?.plus("<===>REPRINT") ?: "<===>REPRINT"
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

    private fun requestForPermission(
        host: LifecycleOwner,
        requestCode: Int,
        permissionRationale: String,
        permissionToRequest: String
    ) {
        if (host is Fragment) {
            EasyPermissions.requestPermissions(
                host,
                permissionRationale,
                requestCode,
                permissionToRequest
            )
        } else {
            host as Activity
            EasyPermissions.requestPermissions(
                host,
                permissionRationale,
                requestCode,
                permissionToRequest
            )
        }
    }

    private fun checkForPermission(context: Context, perms: String) =
        EasyPermissions.hasPermissions(
            context,
            perms
        )

    fun genericPermissionHandler(
        host: LifecycleOwner,
        context: Context,
        perm: String,
        permCode: Int,
        permRationale: String,
        fn: () -> Unit
    ) {
        if (checkForPermission(context, perm)) {
            fn()
        } else {
            requestForPermission(
                host,
                permCode,
                permRationale,
                perm
            )
        }
    }

    fun PayWithCardNotificationModelResponse.mapToTransactionResponse(): TransactionResponse {
        val currentDateTime = getLocaleCurrentDateTime()
        return TransactionResponse().apply {
            transactionType = TransactionType.PURCHASE
            maskedPan = this@mapToTransactionResponse.maskedPan
            amount = this@mapToTransactionResponse.amount.toDouble().toLong()
            transmissionDateTime = currentDateTime
            STAN = ""
            RRN = this@mapToTransactionResponse.rrn
            responseCode = this@mapToTransactionResponse.code
            cardLabel = ""
            cardHolder = this@mapToTransactionResponse.customerName
            transactionTimeInMillis = dateStrToLong(currentDateTime, "yyyy-MM-dd hh:mm a")
            accountType = IsoAccountType.DEFAULT_UNSPECIFIED
            terminalId = this@mapToTransactionResponse.terminalId
            merchantId = this@mapToTransactionResponse.merchantId
        }
    }

    fun GetZenithPayByTransferUserTransactionsModel.mapToTransactionResponse(): TransactionResponse {
        val currentDateTime = getCurrentDateTime()
        val boldText = "<b>PayByTransfer</b>"
        val spanned: Spanned = Html.fromHtml(boldText, Html.FROM_HTML_MODE_LEGACY)

        return TransactionResponse().apply {
            transactionType = TransactionType.PURCHASE
            maskedPan = ""
            amount = this@mapToTransactionResponse.amount.toDouble().toLong()
            transmissionDateTime = paid_at
            STAN = ""
            RRN = "$spanned ${this@mapToTransactionResponse.transaction_reference}"
            responseCode = "00"
            cardLabel = this@mapToTransactionResponse.payer_account_number
            cardHolder = this@mapToTransactionResponse.payer_account_name
            transactionTimeInMillis = dateStrToLong(paid_at, "yyyy-MM-dd hh:mm:ss")
            accountType = IsoAccountType.DEFAULT_UNSPECIFIED
            terminalId = this@mapToTransactionResponse.terminalId
            merchantId = this@mapToTransactionResponse.merchantId
        }
    }

}
