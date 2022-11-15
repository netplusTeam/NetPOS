package com.woleapp.netpos.network

import com.woleapp.netpos.model.GetPayByTransferUserAccount
import com.woleapp.netpos.model.GetZenithPayByTransferUserTransactions
import com.woleapp.netpos.model.ZenithPayByTransferRegisterDeviceTokenModel
import io.reactivex.Single
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

interface ZenithPayByTransferService {
    @GET("api/getUserAccount/{terminalId}")
    fun getUserAccount(
        @Path("terminalId") terminalId: String
    ): Single<GetPayByTransferUserAccount>

    @GET("api/queryTransactions/{requestParameters}")
    fun getTransactions(
        @Path("requestParameters") requestParameters: String
    ): Single<GetZenithPayByTransferUserTransactions>

    @POST("api/addFirebaseToken")
    fun registerDeviceToken(@Body req: ZenithPayByTransferRegisterDeviceTokenModel): Single<String>
}
