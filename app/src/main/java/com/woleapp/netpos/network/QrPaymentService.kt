package com.woleapp.netpos.network

import com.google.gson.JsonObject
import com.woleapp.netpos.model.PayWithQrRequest
import io.reactivex.Single
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST

interface QrPaymentService {

    @POST("contactlessQr")
    fun payWithQr(
        @Body payWithQrRequest: PayWithQrRequest,
    ): Single<Response<String>>
    
}
