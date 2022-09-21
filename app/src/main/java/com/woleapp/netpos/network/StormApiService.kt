package com.woleapp.netpos.network

import com.google.gson.JsonObject
import com.woleapp.netpos.model.* // ktlint-disable no-wildcard-imports
import io.reactivex.Single
import retrofit2.Response
import retrofit2.http.* // ktlint-disable no-wildcard-imports

interface StormApiService {
    @POST("api/token")
    fun appToken(@Body credentials: JsonObject?): Single<TokenResp>

    @POST("api/auth")
    fun userToken(
        @Body credentials: JsonObject?
    ): Single<TokenResp>

    @POST("api/auth")
    fun userToken(
        @Header("Authorization") appToken: String?,
        @Body credentials: JsonObject?
    ): Single<TokenResp>

    @GET("api/agents/{stormId}")
    fun getAgentDetails(@Path("stormId") stormId: String?): Single<User>

    @POST("api/passwordReset")
    fun passwordReset(
        @Body payload: JsonObject?
    ): Single<Response<Any?>?>

    @GET("/api/nip-notifications")
    fun getNotificationByReference(
        @Query("referenceNo") reference: String,
        @Header("X-CLIENT-ID") clientId: String,
        @Header("X-ACCESSCODE") accessCode: String
    ): Single<NipNotification>

    @GET("/api/nip-notifications")
    fun getNotifications(
        @Query("terminalId") terminalId: String,
        @Query("from") from: String? = null,
        @Query("to") to: String? = null,
        @Header("X-CLIENT-ID") clientId: String,
        @Header("X-ACCESSCODE") accessCode: String
    ): Single<List<NipNotification>>

    @POST("/pos_transaction")
    fun logTransactionBeforeConnectingToNibss(
        @Body dataToLog: TransactionToLogBeforeConnectingToNibbs
    ): Single<ResponseBodyAfterLoginToBackend>

    @PUT("/pos_transaction/{rrn}")
    fun updateLogAfterConnectingToNibss(
        @Path("rrn") rrn: String,
        @Body data: DataToLogAfterConnectingToNibss
    ): Single<Response<LogToBackendResponse>>

    @GET("/pos_transactions/terminal/{terminalId}/btw/{from}/{to}/{page}/{pageSize}")
    fun getTransactionsFromNewService(
        @Path("terminalId") terminalId: String,
        @Path("from") from: String,
        @Path("to") to: String,
        @Path("page") page: Int,
        @Path("pageSize") pageSize: Int
    ): Single<GetEndOfDayModelFromNewServer>

    @GET("/pos_transactions/terminal/{terminalId}/{page}/{pageSize}")
    fun getTransactionsFromNewServiceByTerminalId(
        @Path("terminalId") terminalId: String,
        @Path("page") page: Int,
        @Path("pageSize") pageSize: Int
    ): Single<GetEndOfDayModelFromNewServer>
    // http://localhost:8800/pos_transactions/terminal/2101JJ91/btw/2021-09-24 00:00:00/2021-09-24 23:59:59/1/2
// https://device.netpluspay.com/pos_transactions/terminal/:terminalId?/:page?/:pageSize
}
