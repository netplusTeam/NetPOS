package com.woleapp.netpos.di

import android.content.Context
import androidx.room.Room
import com.woleapp.netpos.BuildConfig
import com.woleapp.netpos.database.AppDatabase
import com.woleapp.netpos.model.AppConstants.APP_DB_NAME
import com.woleapp.netpos.model.AppConstants.BASE_URL_FOR_LOGGING_TO_BACKEND
import com.woleapp.netpos.network.StormApiService
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.Cache
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object Module {

    @Provides
    @Singleton
    fun providesBaseUrl() = BASE_URL_FOR_LOGGING_TO_BACKEND

    fun providesRetrofit() {}

    @Singleton
    @Provides
    fun providesOKHTTPClient(
        @ApplicationContext context: Context
    ): OkHttpClient {
        val cacheSize = (5 * 1024 * 1024).toLong()
        val mCache = Cache(context.cacheDir, cacheSize)
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            setLevel(HttpLoggingInterceptor.Level.BODY)
        }
        return if (BuildConfig.DEBUG) {
            OkHttpClient().newBuilder()
                .cache(mCache)
                .retryOnConnectionFailure(true)
                .connectTimeout(200, TimeUnit.SECONDS)
                .readTimeout(200, TimeUnit.SECONDS)
                .addInterceptor(loggingInterceptor)
                .followRedirects(true)
                .followSslRedirects(true)
                .build()
        } else {
            OkHttpClient().newBuilder()
                .connectTimeout(200, TimeUnit.SECONDS)
                .readTimeout(200, TimeUnit.SECONDS)
                .retryOnConnectionFailure(true)
                .followRedirects(true)
                .followSslRedirects(true)
                .build()
        }
    }

    @Singleton
    @Provides
    fun providesStormApiService(retrofit: Retrofit): StormApiService =
        retrofit.create(StormApiService::class.java)

    @Singleton
    @Provides
    fun providesTransactionResponseDao(appDatabase: AppDatabase) =
        appDatabase.transactionResponseDao()

    @Provides
    @Singleton
    fun providesLocalDataBase(@ApplicationContext context: Context) =
        Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            APP_DB_NAME
        ).build()
}
