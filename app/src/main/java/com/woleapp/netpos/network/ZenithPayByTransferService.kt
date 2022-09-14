package com.woleapp.netpos.network

import io.reactivex.Single
import retrofit2.http.GET
import retrofit2.http.Path

interface ZenithPayByTransferService {
    @GET("/getUserAccount/{merchant_id}")
    fun getUserAccount(
        @Path("merchant_id") merchantId: String
    ): Single<String>
}
