package com.woleapp.netpos.model.checkout

import com.woleapp.netpos.util.RandomNumUtil.getGUID


data class CheckOutModel(
    val merchantId: String,
    val name: String,
    val email: String,
    val amount: Double,
    val currency: String,
    val orderId: String = getGUID()
)
