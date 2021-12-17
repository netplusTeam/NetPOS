package com.woleapp.netpos.network

import com.netpluspay.nibssclient.models.TransactionResponse
import com.woleapp.netpos.model.GateWayTransactionResponse
import io.reactivex.Single
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Headers
import retrofit2.http.QueryMap

object NetPOSGatewayApi {

    private val client = OkHttpClient.Builder()
        .addInterceptor(HttpLoggingInterceptor().apply {
            setLevel(HttpLoggingInterceptor.Level.HEADERS)
        }).addInterceptor(HttpLoggingInterceptor().apply {
            setLevel(HttpLoggingInterceptor.Level.BODY)
        }).build()

    private const val BASE_URL = "https://netpos.netpluspay.com/"
    private var INSTANCE: GatewayService? = null
    fun getInstance(): GatewayService = INSTANCE ?: synchronized(this) {
        INSTANCE ?: Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(client)
            .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(GatewayService::class.java)
            .also {
                INSTANCE = it
            }
    }

}

interface GatewayService {
    @GET("partner-api/operator/pos-transactions")
    @Headers(
        "X-CLIENT-ID: b7c4fc42-4e1a-4493-bb4e-bf798d9ce8a1",
        "X-ACCESS-CODE: 9837a93abecc10faf7a36145401c1d9aabdc60d7d88cfba411a5b2dcd4423709"
    )
    fun getTransactions(@QueryMap queryParams: Map<String, String>): Single<GateWayTransactionResponse>
}