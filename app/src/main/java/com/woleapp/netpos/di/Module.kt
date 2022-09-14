package com.woleapp.netpos.di

import android.content.Context
import com.woleapp.netpos.BuildConfig
import com.woleapp.netpos.database.AppDatabase
import com.woleapp.netpos.network.StormApiService
import com.woleapp.netpos.network.ZenithPayByTransferService
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.Cache
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Named
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object Module {

    @Provides
    @Singleton
    @Named("defaultBaseUrl")
    fun providesDefaultBaseUrl(): String = BuildConfig.STRING_DEFAULT_BASE_URL

    @Provides
    @Singleton
    @Named("payByTransferBaseUrl")
    fun providesZenithPayByTransferBaseUrl(): String = BuildConfig.STRING_ZENITH_BASE_URL

    @Provides
    @Singleton
    fun providesLoginInterceptor(): Interceptor = HttpLoggingInterceptor().apply {
        setLevel(HttpLoggingInterceptor.Level.BODY)
    }

    @Singleton
    @Provides
    fun providesOKHTTPClient(
        @ApplicationContext context: Context,
        loggingInterceptor: Interceptor
    ): OkHttpClient {
        val cacheSize = (5 * 1024 * 1024).toLong()
        val mCache = Cache(context.cacheDir, cacheSize)
        return if (BuildConfig.DEBUG) {
            OkHttpClient().newBuilder()
                .cache(mCache)
                .retryOnConnectionFailure(true)
                .connectTimeout(20, TimeUnit.SECONDS)
                .readTimeout(20, TimeUnit.SECONDS)
                .addInterceptor(loggingInterceptor)
                .followRedirects(true)
                .followSslRedirects(true)
                .build()
        } else {
            OkHttpClient().newBuilder()
                .connectTimeout(20, TimeUnit.SECONDS)
                .readTimeout(20, TimeUnit.SECONDS)
                .retryOnConnectionFailure(true)
                .followRedirects(true)
                .followSslRedirects(true)
                .build()
        }
    }

    @Provides
    @Singleton
    @Named("defaultRetrofit")
    fun providesDefaultRetrofit(
        okhttp: OkHttpClient,
        @Named("defaultBaseUrl") baseUrl: String
    ): Retrofit =
        Retrofit.Builder()
            .addConverterFactory(GsonConverterFactory.create())
            .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
            .baseUrl(baseUrl)
            .client(okhttp)
            .build()

    @Provides
    @Singleton
    @Named("zenithPayByTransferRetrofit")
    fun providesPayByTransferRetrofit(
        okhttp: OkHttpClient,
        @Named("payByTransferBaseUrl") baseUrl: String
    ): Retrofit =
        Retrofit.Builder()
            .addConverterFactory(GsonConverterFactory.create())
            .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
            .baseUrl(baseUrl)
            .client(okhttp)
            .build()

    @Singleton
    @Provides
    fun providesStormApiService(
        @Named("defaultRetrofit") retrofit: Retrofit
    ): StormApiService =
        retrofit.create(StormApiService::class.java)

    @Singleton
    @Provides
    fun providesZenithPayByTransferService(
        @Named("zenithPayByTransferRetrofit") retrofit: Retrofit
    ): ZenithPayByTransferService =
        retrofit.create(ZenithPayByTransferService::class.java)

    @Provides
    @Singleton
    fun providesLocalDataBase(
        @ApplicationContext context: Context
    ): AppDatabase =
        AppDatabase.getDatabaseInstance(context)

    @Singleton
    @Provides
    fun providesTransactionResponseDao(
        appDatabase: AppDatabase
    ) =
        appDatabase.transactionResponseDao()
}
