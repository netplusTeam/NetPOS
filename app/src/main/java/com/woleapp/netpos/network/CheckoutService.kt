package com.woleapp.netpos.network

import com.google.gson.JsonObject
import com.woleapp.netpos.model.checkout.CheckOutResponse
import com.woleapp.netpos.model.pay.PayModel
import io.reactivex.Single
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

interface CheckoutService {

    @GET("v2/checkout")
    fun checkOut(
        @Query("merchantId") merchantId : String,
        @Query("name") name : String,
        @Query("email") email : String,
        @Query("amount") amount : Double,
        @Query("currency") currency : String,
        @Query("orderId") orderId : String,
    ): Single<CheckOutResponse>

    @POST("v2/pay")
    fun pay(
        @Body payModel: PayModel
    ): Single<JsonObject>

}