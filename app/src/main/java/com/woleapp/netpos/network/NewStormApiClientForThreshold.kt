package com.woleapp.netpos.network

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory
import retrofit2.converter.gson.GsonConverterFactory

class NewStormApiClientForThreshold {

    companion object {

        private fun getBaseOkhttpClientBuilder(): OkHttpClient.Builder {
            val okHttpClientBuilder = OkHttpClient.Builder()

            val loggingInterceptor = HttpLoggingInterceptor()
            loggingInterceptor.level = HttpLoggingInterceptor.Level.BODY
            okHttpClientBuilder.addInterceptor(loggingInterceptor)

            return okHttpClientBuilder
        }

        private var LOGGING_INSTANCE: NewStormApiService? = null
        fun getStormApiLoginInstance(): NewStormApiService =
            LOGGING_INSTANCE ?: synchronized(this) {
                LOGGING_INSTANCE ?: Retrofit.Builder()
                    .baseUrl("https://device.netpluspay.com/")
                    .client(getBaseOkhttpClientBuilder().build())
                    .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
                    .addConverterFactory(GsonConverterFactory.create())
                    .build()
                    .create(NewStormApiService::class.java)
                    .also {
                        LOGGING_INSTANCE = it
                    }
            }
    }
}
