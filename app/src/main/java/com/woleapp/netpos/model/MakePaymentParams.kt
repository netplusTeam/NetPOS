package com.woleapp.netpos.model

import com.danbamitale.epmslib.entities.CardData
import com.danbamitale.epmslib.entities.OriginalDataElements
import com.danbamitale.epmslib.entities.TransactionType
import com.danbamitale.epmslib.utils.IsoAccountType
import com.woleapp.netpos.util.Singletons

data class MakePaymentParams(
    var action: String = "makePayment",
    var terminalId: String? = Singletons.getCurrentlyLoggedInUser()?.terminal_id,
    var amount: Long = 0,
    var otherAmount: Long = 0,
    var transactionType: TransactionType = TransactionType.PURCHASE,
    var accountType: IsoAccountType = IsoAccountType.DEFAULT_UNSPECIFIED,
    var cardData: CardData,
    var remark: String? = null,
) {
    var fundWallet: Boolean = true

    constructor(
        terminalId: String?,
        amount: Long,
        otherAmount: Long,
        cardData: CardData,
        transactionType: TransactionType,
        isoAccountType: IsoAccountType,
    ) : this(
        terminalId = terminalId,
        amount = amount,
        otherAmount = otherAmount,
        cardData = cardData,
        accountType = isoAccountType,
        transactionType = transactionType,
    )

    constructor(
        amount: Long,
        otherAmount: Long,
        cardData: CardData,
        transactionType: TransactionType,
        isoAccountType: IsoAccountType,
    ) : this(
        amount = amount,
        otherAmount = otherAmount,
        cardData = cardData,
        accountType = isoAccountType,
        transactionType = transactionType,
    )

    constructor(terminalId: String?, amount: Long, otherAmount: Long, cardData: CardData) : this(
        action = "makePayment",
        terminalId = terminalId,
        amount = amount,
        otherAmount = otherAmount,
        cardData = cardData,
    )

    constructor(amount: Long, otherAmount: Long, cardData: CardData) : this(
        action = "makePayment",
        terminalId = TerminalParams.TERMINAL_ID,
        amount = amount,
        otherAmount = otherAmount,
        cardData = cardData,
    )

    fun validate(): Boolean = !(terminalId == null || cardData == null)
    var originalDataElements: OriginalDataElements? = null
}
