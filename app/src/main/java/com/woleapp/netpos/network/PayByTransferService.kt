package com.woleapp.netpos.network


import com.woleapp.netpos.model.MerchantDetailsResponse
import io.reactivex.Single
import retrofit2.Response
import retrofit2.http.*


interface PayByTransferService {

    @GET("getUserAccount/{NetpluspayMid}")
    fun getMerchantDetails(
        @Header("Authorization") token: String,
        @Path("NetpluspayMid") partnerId: String
    ): Single<Response<MerchantDetailsResponse>>

}
