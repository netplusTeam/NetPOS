package com.woleapp.netpos.di

import android.content.Context
import com.google.gson.Gson
import com.pixplicity.easyprefs.library.Prefs
import com.woleapp.netpos.BuildConfig
import com.woleapp.netpos.database.AppDatabase
import com.woleapp.netpos.database.dao.ZenithPayByTransferUserTransactionsDao
import com.woleapp.netpos.network.*
import com.woleapp.netpos.util.PREF_USER_TOKEN
import com.woleapp.netpos.util.UtilityParams.COMPLAINTS_BASE_URL
import com.woleapp.netpos.util.UtilityParams.FCMB_MERCHANTS_ACCOUNT_BASE_URL
import com.woleapp.netpos.util.UtilityParams.PAY_BY_TRANSFER_BASE_URL
import com.woleapp.netpos.util.UtilityParams.PROVIDUS_MERCHANTS_ACCOUNT_BASE_URL
import com.woleapp.netpos.util.UtilityParams.RRN_BASE_URL
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import io.reactivex.Scheduler
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
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
    @Named("contactQrPaymentBaseUrl")
    fun providesBaseUrlForContactlessPaymentWithQr(): String =
        BuildConfig.STRING_CONTACTLESS_PAYMENT_WITH_QR_BASE_URL


    @Provides
    @Singleton
    @Named("zenithPayByTransferBaseUrl")
    fun payByTransferBaseUrl(): String = PAY_BY_TRANSFER_BASE_URL

    @Provides
    @Singleton
    @Named("complaintBaseUrl")
    fun providesBaseUrlForNotification(): String = COMPLAINTS_BASE_URL

    @Provides
    @Singleton
    @Named("providusMerchantsAccountBaseUrl")
    fun providusMerchantsAccountBaseUrl(): String = PROVIDUS_MERCHANTS_ACCOUNT_BASE_URL

    @Provides
    @Singleton
    @Named("fcmbMerchantsAccountBaseUrl")
    fun fcmbMerchantsAccountBaseUrl(): String = FCMB_MERCHANTS_ACCOUNT_BASE_URL


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
        @Named("loginInterceptor") loggingInterceptor: Interceptor,
    ): OkHttpClient =
        OkHttpClient().newBuilder()
            .connectTimeout(120, TimeUnit.SECONDS)
            .readTimeout(120, TimeUnit.SECONDS)
            .writeTimeout(120, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .addInterceptor(loggingInterceptor)
            .build()

    @Singleton
    @Provides
    @Named("zenithPayByTransferOkHttp")
    fun providesZenithOkHttpClient(
        @ApplicationContext context: Context,
        @Named("loginInterceptor") loggingInterceptor: Interceptor,
        @Named("zenithPayByTransferHeaderInterceptor") zenithPayByTransferHeaderInterceptor: Interceptor,
    ): OkHttpClient =
        OkHttpClient().newBuilder()
            .connectTimeout(120, TimeUnit.SECONDS)
            .writeTimeout(120, TimeUnit.SECONDS)
            .readTimeout(120, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .addInterceptor(zenithPayByTransferHeaderInterceptor)
            .addInterceptor(loggingInterceptor)
            .build()

    @Provides
    @Singleton
    @Named("payByTransferOkHttp")
    fun providesOKHTTPClientForPayByTransfer(
        @Named("loginInterceptor") loggingInterceptor: Interceptor,
    ): OkHttpClient = OkHttpClient().newBuilder().connectTimeout(70, TimeUnit.SECONDS)
        .readTimeout(70, TimeUnit.SECONDS).writeTimeout(70, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true).addInterceptor(loggingInterceptor).build()

    @Provides
    @Singleton
    @Named("fcmbMerchantsAccountRetrofit")
    fun fcmbMerchantsAccountService(
        @Named("payByTransferOkHttp") okhttp: OkHttpClient,
        @Named("fcmbMerchantsAccountBaseUrl") payByTransferBaseUrl: String,
    ): Retrofit = Retrofit.Builder().addConverterFactory(GsonConverterFactory.create())
        .addCallAdapterFactory(RxJava2CallAdapterFactory.create()).baseUrl(payByTransferBaseUrl)
        .client(okhttp).build()


    @Provides
    @Singleton
    @Named("providusMerchantsAccountRetrofit")
    fun providusMerchantsAccountService(
        @Named("payByTransferOkHttp") okhttp: OkHttpClient,
        @Named("providusMerchantsAccountBaseUrl") payByTransferBaseUrl: String,
    ): Retrofit = Retrofit.Builder().addConverterFactory(GsonConverterFactory.create())
        .addCallAdapterFactory(RxJava2CallAdapterFactory.create()).baseUrl(payByTransferBaseUrl)
        .client(okhttp).build()

    @Provides
    @Singleton
    @Named("notificationRetrofit")
    fun providesRetrofitForNotificationService(
        @Named("defaultOkHttpClient") okhttp: OkHttpClient,
        @Named("complaintBaseUrl") notificationBaseUrl: String,
    ): Retrofit = Retrofit.Builder().addConverterFactory(GsonConverterFactory.create())
        .addCallAdapterFactory(RxJava2CallAdapterFactory.create()).baseUrl(notificationBaseUrl)
        .client(okhttp).build()

    @Provides
    @Singleton
    @Named("defaultRetrofit")
    fun providesDefaultRetrofit(
        @Named("defaultOkHttpClient") okhttp: OkHttpClient,
        @Named("defaultBaseUrl") baseUrl: String,
    ): Retrofit =
        Retrofit.Builder()
            .addConverterFactory(GsonConverterFactory.create())
            .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
            .baseUrl(baseUrl)
            .client(okhttp)
            .build()

    @Provides
    @Singleton
    @Named("rrnRetrofit")
    fun providesRrnRetrofit(
        @Named("defaultOkHttpClient") okhttp: OkHttpClient,
    ): Retrofit =
        Retrofit.Builder()
            .addConverterFactory(GsonConverterFactory.create())
            .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
            .baseUrl(RRN_BASE_URL)
            .client(okhttp)
            .build()

    @Provides
    @Singleton
    @Named("zenithPayByTransferRetrofit")
    fun providesPayByTransferRetrofit(
        @Named("zenithPayByTransferOkHttp") okhttp: OkHttpClient,
        @Named("payByTransferBaseUrl") baseUrl: String,
    ): Retrofit =
        Retrofit.Builder()
            .addConverterFactory(GsonConverterFactory.create())
            .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
            .baseUrl(baseUrl)
            .client(okhttp)
            .build()


    @Provides
    @Singleton
    @Named("contactQrPaymentRetrofit")
    fun providesRetrofitForContactlessQrPayment(
        @Named("defaultOkHttpClient") okhttp: OkHttpClient,
        @Named("contactQrPaymentBaseUrl") contactQrPaymentBaseUrl: String,
    ): Retrofit =
        Retrofit.Builder()
            .addConverterFactory(GsonConverterFactory.create())
            .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
            .baseUrl(contactQrPaymentBaseUrl)
            .client(okhttp)
            .build()


    @Provides
    @Singleton
    @Named("payByTransferRetrofit")
    fun payByTransferService(
        @Named("payByTransferOkHttp") okhttp: OkHttpClient,
        @Named("zenithPayByTransferBaseUrl") payByTransferBaseUrl: String,
    ): Retrofit = Retrofit.Builder().addConverterFactory(GsonConverterFactory.create())
        .addCallAdapterFactory(RxJava2CallAdapterFactory.create()).baseUrl(payByTransferBaseUrl)
        .client(okhttp).build()

    @Provides
    @Singleton
    fun providusMerchantsAccountDetailsService(
        @Named("providusMerchantsAccountRetrofit") retrofit: Retrofit,
    ): ProvidusMerchantsAccountService = retrofit.create(ProvidusMerchantsAccountService::class.java)

    @Provides
    @Singleton
    fun fcmbMerchantsAccountDetailsService(
        @Named("fcmbMerchantsAccountRetrofit") retrofit: Retrofit,
    ): FcmbMerchantsAccountService = retrofit.create(FcmbMerchantsAccountService::class.java)



    @Singleton
    @Provides
    fun providesStormApiService(
        @Named("defaultRetrofit") retrofit: Retrofit,
    ): StormApiService =
        retrofit.create(StormApiService::class.java)

    @Singleton
    @Provides
    fun providesZenithPayByTransferService(
        @Named("zenithPayByTransferRetrofit") retrofit: Retrofit,
    ): ZenithPayByTransferService =
        retrofit.create(ZenithPayByTransferService::class.java)

    @Singleton
    @Provides
    fun providesRrnApiService(
        @Named("rrnRetrofit") rrnRetrofit: Retrofit,
    ): RrnApiService = rrnRetrofit.create(RrnApiService::class.java)

    @Provides
    @Singleton
    fun providesContactlessQrPaymentService(
        @Named("contactQrPaymentRetrofit") retrofit: Retrofit,
    ): QrPaymentService = retrofit.create(QrPaymentService::class.java)

    @Provides
    @Singleton
    fun payByTransferServiceService(
        @Named("payByTransferRetrofit") retrofit: Retrofit,
    ): PayByTransferService = retrofit.create(PayByTransferService::class.java)

    @Provides
    @Singleton
    fun providesNotificationService(
        @Named("notificationRetrofit") retrofit: Retrofit,
    ): SubmitComplaintsService = retrofit.create(SubmitComplaintsService::class.java)

    @Provides
    @Singleton
    fun providesLocalDataBase(
        @ApplicationContext context: Context,
    ): AppDatabase =
        AppDatabase.getDatabaseInstance(context)

    @Singleton
    @Provides
    fun providesTransactionResponseDao(
        appDatabase: AppDatabase,
    ) =
        appDatabase.transactionResponseDao()

    @Singleton
    @Provides
    fun providesZenithPayByTransferLocalDao(
        appDatabase: AppDatabase,
    ): ZenithPayByTransferUserTransactionsDao =
        appDatabase.getZenithPayByTransferDao()

    @Singleton
    @Provides
    fun providesGson() = Gson()

    @Provides
    @Singleton
    @Named("io-scheduler")
    fun providesIoScheduler(): Scheduler = Schedulers.io()

    @Provides
    @Singleton
    @Named("main-scheduler")
    fun providesMainThreadScheduler(): Scheduler = AndroidSchedulers.mainThread()

    @Provides
    @Singleton
    fun providesCompositeDisposable(): CompositeDisposable = CompositeDisposable()

}
