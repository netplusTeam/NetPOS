package com.woleapp.netpos.model

import com.google.gson.annotations.SerializedName

data class UtilitiesPayload(
    var destinationAccount: String,
    @Transient
    var stringAmount: String,
    var amount: Float,
    var service: String,
    var provider: String,
    @Transient
    var billType: String,
    @Transient
    var billPackage: String,
    var serviceType: String
) {
    constructor() : this("", "", 0f, "", "", "", "", "")
}

data class ServiceFeeResponse(
    var provider: String,
    var service: String,
    var fees: Int
)

data class ValidateBillResponse(
    @SerializedName("Bill")
    var bill: String? = null,
    @SerializedName("BillStatus")
    var billStatus: String? = null,
    @SerializedName("BillAccountId")
    var billAccountId: String,
    @SerializedName("BillVerifiedOnline")
    var billVerifiedOnline: String? = null,
    @SerializedName("BillAccountIdDetails")
    var billAccountIdDetails: String? = null,
    @Transient
    var billPackage: String? = null,
    @Transient
    var provider: String? = null,
    @Transient
    var fees: String? = null,
    @Transient
    var amount: String? = null,
    var message: String? = null
)

data class PayBillResponse(
    var reference: String,
    var otherReference: String,
    var amount: String,
    var action: String,
    var token: String?

)

sealed class NetworkResponse

data class ErrorNetworkResponse(var message: String) : NetworkResponse()
data class SuccessNetworkResponse(var message: String, var data: PayBillResponse?) :
    NetworkResponse()

data class Vend(val amount: Double)

data class AuthError(val success: Boolean, val message: String?)

data class GateWayTransactionResponse(
    var result: List<TransactionResponseModelFromGateWay>,
    val count: Int,
    val page: Int,
    val total: Int
)

data class CreateBlueCodeMerchant(
    var city: String,
    var addr: String,
    var zip: String,
    var mcc: String,
    var bankCode: String,
    var acctNumber: String,
    var acctName: String
) {
    constructor() : this("", "", "", "", "", "", "")
}

data class BlueCodeResponse(val message: String, val errors: BlueCodeError?, val data: Any?)
data class BlueCodeError(val property: String, val reason: String)
data class BlueCodeQrResponse(val qr: String, val transactionId: String)
data class BlueCodeTransactionStatus(
    val state: String,
    @SerializedName("merchant_tx_id") val merchantTxId: String,
    val amount: String,
    val terminalId: String,
    val merchant_uniqueId: String,
    val partnerId: String,
    @SerializedName("acquirer_tx_id") val acquirerTxId: String
)
