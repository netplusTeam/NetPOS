package com.woleapp.netpos.network

import com.google.gson.JsonObject
import com.woleapp.netpos.model.BlueCodeQrResponse
import com.woleapp.netpos.model.BlueCodeResponse
import com.woleapp.netpos.model.BlueCodeTransactionStatus
import com.woleapp.netpos.model.CreateBlueCodeMerchant
import io.reactivex.Single
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

interface BlueCodeService {
    @POST("registerMerchant")
    fun createBlueCodeMerchant(@Body payload: CreateBlueCodeMerchant): Single<BlueCodeResponse>

    @GET("getMCC/{page}")
    fun getMCC(@Path("page") page: String): Single<JsonObject>

    @GET("getMCC/category/{category}")
    fun getMCCWithCategory(@Path("category") category: String): Single<JsonObject>

    @GET("getPostal/{state}")
    fun getPostalWithState(@Path("state") state: String): Single<String>

    @POST("getQr")
    fun generateQr(@Body payload: JsonObject): Single<BlueCodeQrResponse>

    @GET("getTransaction/{transactionId}")
    fun getTransactionStatus(@Path("transactionId") transactionId: String): Single<BlueCodeTransactionStatus>

    @GET("getPostal")
    fun getAllPostal(): Single<JsonObject>
}
