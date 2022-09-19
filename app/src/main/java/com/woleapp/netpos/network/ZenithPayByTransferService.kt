package com.woleapp.netpos.network

import com.woleapp.netpos.model.GetPayByTransferUserAccount
import com.woleapp.netpos.model.GetZenithPayByTransferUserTransactions
import io.reactivex.Single
import retrofit2.Response
import retrofit2.http.GET
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
}
