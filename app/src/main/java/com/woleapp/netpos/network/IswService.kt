package com.woleapp.netpos.network

import io.reactivex.Single
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.converter.simplexml.SimpleXmlConverterFactory
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.Headers
import retrofit2.http.POST
import java.util.concurrent.TimeUnit

interface IswService {
    @Headers("content-type:application/xml")
    @POST("kimonotms/requesttoken/perform-process")
    fun getToken(@Body tokenPassportRequest: TokenPassportRequest): Single<TokenPassportResponse>

    @Headers("content-type: application/xml", "Accept: application/xml")
    @POST("amex")
    fun performTransaction(
        @Header("Authorization") token: String,
        @Body transferRequest: TransferRequest
    ): Single<CashOutResponnse>
}

val baseBuilder: Retrofit.Builder = Retrofit.Builder()
    .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
    .addConverterFactory(GsonConverterFactory.create())
    .client(
        getBaseOkhttpClientBuilder()
            .build()
    )

private fun getBaseOkhttpClientBuilder(): OkHttpClient.Builder {
    val okHttpClientBuilder = OkHttpClient.Builder()
    val loggingInterceptor = HttpLoggingInterceptor()
    loggingInterceptor.level = HttpLoggingInterceptor.Level.BODY
    okHttpClientBuilder.addInterceptor(loggingInterceptor)
        .callTimeout(1, TimeUnit.MINUTES)
        .readTimeout(1, TimeUnit.MINUTES)
        .connectTimeout(1, TimeUnit.MINUTES)
    return okHttpClientBuilder
}

val getTokenClient: IswService = Retrofit.Builder()
    .client(getBaseOkhttpClientBuilder().build())
    .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
    .addConverterFactory(SimpleXmlConverterFactory.create())
    .baseUrl("https://saturn.interswitchng.com:443/")
    .build().create(IswService::class.java)

const val live = "https://kimono.interswitchng.com/kmw/kimonoservice/"
const val test = "https://qa.interswitchng.com/kmw/kimonoservice/"
const val KSN_TEST = ""
const val IPEK_TEST = ""
const val KSN_LIVE = ""
const val IPEK_LIVE = ""

val iswTransactionClient: IswService = Retrofit.Builder()
    .client(getBaseOkhttpClientBuilder().build())
    .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
    .addConverterFactory(SimpleXmlConverterFactory.create())
    .baseUrl(live)
    .build().create(IswService::class.java)

val iswTransactionClientTest: IswService = baseBuilder
    .baseUrl(test)
    .build().create(IswService::class.java)
