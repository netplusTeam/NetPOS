package com.woleapp.netpos.app

import android.app.Application
import android.content.ContextWrapper
import android.content.pm.FeatureInfo
import android.util.Log
import android.widget.Toast
import com.google.firebase.FirebaseApp
import com.google.firebase.ktx.Firebase
import com.google.firebase.ktx.app
import com.google.firebase.messaging.ktx.messaging
import com.netpluspay.netpossdk.NetPosSdk
import com.netpluspay.netpossdk.utils.TerminalParameters
import com.pixplicity.easyprefs.library.Prefs
import com.woleapp.netpos.BuildConfig
import com.woleapp.netpos.R
import com.woleapp.netpos.network.StormApiClient
import com.woleapp.netpos.util.*
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.plugins.RxJavaPlugins
import io.reactivex.schedulers.Schedulers
import timber.log.Timber

class NetPosApp : Application() {


    override fun onCreate() {
        super.onCreate()
        Timber.plant(Timber.DebugTree())
        FirebaseApp.initializeApp(this)
        Prefs.Builder()
            .setContext(this)
            .setMode(ContextWrapper.MODE_PRIVATE)
            .setPrefsName(packageName)
            .setUseDefaultSharedPreference(true)
            .build()
        //TimeZone.setDefault(TimeZone.getTimeZone("GMT+8"));
        RxJavaPlugins.setErrorHandler {
            Timber.e("Error: ${it.localizedMessage}")
        }
        /*Thread.setDefaultUncaughtExceptionHandler { _, e ->
            Timber.e("LMAOOOOO, e wan crash")
            Timber.e(e)
            throw e
        }*/
//        ProcessLifecycleOwner.get().lifecycle
//            .addObserver(AppLifeCycleObserver())

        NetPosSdk.init()
        if (Prefs.contains("load_provided").not()) {
            NetPosSdk.loadProvidedCapksAndAids()
            NetPosSdk.loadEmvParams(
                TerminalParameters()
                    .apply {
                        //online E068C8
                        terminalCapability = "E0F8C8"
                    }
            )
            Prefs.putBoolean("load_provided", true)
        }

        if (Prefs.contains("notification_campaign").not() && BuildConfig.FLAVOR.equals("netpos", true)) {
            Firebase.messaging.subscribeToTopic("netpos_campaign")
                .addOnCompleteListener { task ->
                    var msg = "subscribed"
                    if (!task.isSuccessful) {
                        msg = "subscription failed"
                    } else {
                        Prefs.putBoolean("notification_campaign", true)
                    }
                    Timber.e(msg)
                    Toast.makeText(baseContext, msg, Toast.LENGTH_SHORT).show()
                }
        }
        if (checkBillsPaymentToken().not())
            getBillsToken(StormApiClient.getBillsInstance())
        if (checkAppToken().not())
            getAppToken(StormApiClient.getBillsInstance()).subscribeOn(Schedulers.io())
                .retry(2)
                .observeOn(AndroidSchedulers.mainThread()).subscribe { _, _ ->

                }.disposeWith(CompositeDisposable())
    }


}