package com.woleapp.netpos.network

import com.danbamitale.epmslib.entities.CardData
import com.danbamitale.epmslib.entities.PosMode
import com.danbamitale.epmslib.entities.TransactionResponse
import com.danbamitale.epmslib.entities.TransactionType
import com.danbamitale.epmslib.utils.IsoAccountType
import com.isw.iswclient.request.IswParameters
import org.apache.commons.lang.StringUtils
import org.simpleframework.xml.Element
import org.simpleframework.xml.Path
import org.simpleframework.xml.Root
import java.text.SimpleDateFormat
import java.util.*

@Root(strict = false, name = "transferResponse")
data class CashOutResponnse @JvmOverloads constructor(
    @field:Element(name = "description")
    var description: String = "",
    @field:Element(name = "field39")
    var field39: String = "A3",
    @field:Element(name = "authId", required = false)
    var authId: String = "",
    @field:Path("hostEmvData")
    @field:Element(name = "AmountAuthorized", required = false)
    var amountAuthorized: Long = 0L,
    @field:Path("hostEmvData")
    @field:Element(name = "AmountOther", required = false)
    var AmountOther: Long = 0L,
    @field:Path("hostEmvData")
    @field:Element(name = "atc", required = false)
    var atc: String = "",
    @field:Path("hostEmvData")
    @field:Element(name = "iad", required = false)
    var iad: String = "",
    @field:Path("hostEmvData")
    @field:Element(name = "rc", required = false)
    var rc: Int = 0,
    @field:Element(name = "referenceNumber", required = false)
    var referenceNumber: String = "",
    @field:Element(name = "transactionChannelName", required = false)
    var transactionChannelName: String = "",
    @field:Element(name = "wasReceived", required = false)
    var wasReceived: Boolean = false,
    @field:Element(name = "wasSent", required = false)
    var wasSent: Boolean = false
)

fun CashOutResponnse.toTransactionResponse(
    stan: String,
    cardData: CardData,
    transactionTime: Long,
    iswParameters: IswParameters,
    amount: Long,
    interswitchThreshold: Long = 0L,
    errorMessage: String? = null,
    reqRRN: String = ""
) = TransactionResponse().also {
    it.amount = amount
    it.RRN = if (this.referenceNumber.isEmpty()) reqRRN else this.referenceNumber
    it.responseCode = this.field39
    it.authCode = this.authId
    it.STAN = stan
    it.echoData = iswParameters.remark
    it.maskedPan = StringUtils.overlay(cardData.pan, "xxxxxx", 6, 12)
    it.cardExpiry = cardData.expiryDate
    it.cardLabel = ""
    it.transactionTimeInMillis = transactionTime
    it.transactionType = TransactionType.PURCHASE
    it.terminalId = iswParameters.terminalId
    it.merchantId = iswParameters.merchantId
    it.transmissionDateTime = SimpleDateFormat("MMddhhmmss", Locale.getDefault()).format(transactionTime)
    it.localTime_12 = SimpleDateFormat("hhmmss", Locale.getDefault()).format(transactionTime)
    it.localDate_13 = SimpleDateFormat("MMdd", Locale.getDefault()).format(transactionTime)
    it.accountType = IsoAccountType.DEFAULT_UNSPECIFIED
    it.source = PosMode.ISW
    it.errorMessage = errorMessage
    it.interSwitchThreshold = interswitchThreshold
}

