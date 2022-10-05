package com.woleapp.netpos.di

import android.content.Context
import com.google.gson.Gson
import com.pixplicity.easyprefs.library.Prefs
import com.woleapp.netpos.BuildConfig
import com.woleapp.netpos.database.AppDatabase
import com.woleapp.netpos.database.dao.ZenithPayByTransferUserTransactionsDao
import com.woleapp.netpos.network.StormApiService
import com.woleapp.netpos.network.ZenithPayByTransferService
import com.woleapp.netpos.util.PREF_USER_TOKEN
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
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
    @Named("loginInterceptor")
    fun providesLoginInterceptor(): Interceptor = HttpLoggingInterceptor().apply {
        setLevel(HttpLoggingInterceptor.Level.BODY)
    }

    @Provides
    @Singleton
    @Named("zenithPayByTransferHeaderInterceptor")
    fun providesZenithPayByTransferHeaderInterceptor(): Interceptor = Interceptor { chain ->
        val originalRequest = chain.request()
        val requestHeaderInterceptor = originalRequest.newBuilder()
            .addHeader("Authorization", "Bearer ${Prefs.getString(PREF_USER_TOKEN, "")}")
            .build()
        chain.proceed(requestHeaderInterceptor)
    }

    @Singleton
    @Provides
    @Named("defaultOkHttpClient")
    fun providesDefaultOkHttpClient(
        @Named("loginInterceptor") loggingInterceptor: Interceptor
    ): OkHttpClient {
        return if (BuildConfig.DEBUG) {
            OkHttpClient().newBuilder()
                .connectTimeout(20, TimeUnit.SECONDS)
                .readTimeout(20, TimeUnit.SECONDS)
                .retryOnConnectionFailure(true)
                .addInterceptor(loggingInterceptor)
                .build()
        } else {
            OkHttpClient().newBuilder()
                .connectTimeout(20, TimeUnit.SECONDS)
                .readTimeout(20, TimeUnit.SECONDS)
                .retryOnConnectionFailure(true)
                .addInterceptor(loggingInterceptor)
                .build()
        }
    }

    @Singleton
    @Provides
    @Named("zenithPayByTransferOkHttp")
    fun providesZenithOkHttpClient(
        @ApplicationContext context: Context,
        @Named("loginInterceptor") loggingInterceptor: Interceptor,
        @Named("zenithPayByTransferHeaderInterceptor") zenithPayByTransferHeaderInterceptor: Interceptor
    ): OkHttpClient {
        return if (BuildConfig.DEBUG) {
            OkHttpClient().newBuilder()
                .connectTimeout(20, TimeUnit.SECONDS)
                .readTimeout(20, TimeUnit.SECONDS)
                .retryOnConnectionFailure(true)
                .addInterceptor(zenithPayByTransferHeaderInterceptor)
                .addInterceptor(loggingInterceptor)
                .build()
        } else {
            OkHttpClient().newBuilder()
                .connectTimeout(20, TimeUnit.SECONDS)
                .readTimeout(20, TimeUnit.SECONDS)
                .retryOnConnectionFailure(true)
                .addInterceptor(zenithPayByTransferHeaderInterceptor)
                .addInterceptor(loggingInterceptor)
                .build()
        }
    }

    @Provides
    @Singleton
    @Named("defaultRetrofit")
    fun providesDefaultRetrofit(
        @Named("defaultOkHttpClient") okhttp: OkHttpClient,
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
        @Named("zenithPayByTransferOkHttp") okhttp: OkHttpClient,
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

    @Singleton
    @Provides
    fun providesZenithPayByTransferLocalDao(
        appDatabase: AppDatabase
    ): ZenithPayByTransferUserTransactionsDao =
        appDatabase.getZenithPayByTransferDao()

    @Singleton
    @Provides
    fun providesGson() = Gson()
}
