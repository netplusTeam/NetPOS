package com.woleapp.netpos.network

import com.google.gson.JsonObject
import io.reactivex.Single
import retrofit2.http.Body
import retrofit2.http.POST

interface NetPOSCashService {
    @POST("addTransactions")
    fun addCashTransaction(@Body body: JsonObject): Single<Any>
}

