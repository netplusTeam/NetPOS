package com.woleapp.netpos.network


import android.util.Log
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.woleapp.netpos.model.checkout.CheckOutModel
import com.woleapp.netpos.model.pay.PayModel
import io.reactivex.Single
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PayByTransferRepository @Inject constructor(
    private val payByTransferService: PayByTransferService,
    private val checkoutService: CheckoutService,
    private val providusMerchantsAccountService: ProvidusMerchantsAccountService,
    private val fcmbMerchantsAccountService: FcmbMerchantsAccountService
) {

    fun getMerchantDetails(token: String, netPlusPayMid: String) = payByTransferService.getMerchantDetails(token, netPlusPayMid)
    fun getProvidusMerchantDetails(token: String, netPlusPayMid: String) = providusMerchantsAccountService.getMerchantDetails(token, netPlusPayMid)
    fun getFcmbMerchantDetails(token: String, netPlusPayMid: String) = fcmbMerchantsAccountService.getMerchantDetails(token, netPlusPayMid)
    fun checkOut(checkOutModel: CheckOutModel) = checkoutService.checkOut(
        checkOutModel.merchantId,
        checkOutModel.name,
        checkOutModel.email,
        checkOutModel.amount,
        checkOutModel.currency,
        checkOutModel.orderId
    )

    fun pay(clientData: String): Single<JsonObject> = checkoutService.pay(PayModel(clientData,"PAY"))


}