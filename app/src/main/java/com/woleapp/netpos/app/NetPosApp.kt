package com.woleapp.netpos.app

import android.annotation.SuppressLint
import android.app.Application
import android.content.ContextWrapper
import android.os.RemoteException
import com.google.firebase.FirebaseApp
import com.google.firebase.ktx.Firebase
import com.google.firebase.messaging.ktx.messaging
import com.horizonpay.smartpossdk.PosAidlDeviceServiceUtil
import com.horizonpay.smartpossdk.aidl.IAidlDevice
import com.horizonpay.utils.BaseUtils
import com.netpluspay.netpossdk.NetPosSdk
import com.netpluspay.netpossdk.utils.TerminalParameters
import com.pixplicity.easyprefs.library.Prefs
import com.woleapp.netpos.BuildConfig
import com.woleapp.netpos.network.StormApiClient
import com.woleapp.netpos.util.*
import dagger.hilt.android.HiltAndroidApp
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.plugins.RxJavaPlugins
import io.reactivex.schedulers.Schedulers
import timber.log.Timber



@HiltAndroidApp
class NetPosApp : Application() {

    // 1. Hold a reference to the device (optional, but good for debugging)
    var device: IAidlDevice? = null

    @SuppressLint("BinaryOperationInTimber")
    override fun onCreate() {
        super.onCreate()
        Timber.plant(Timber.DebugTree())
        FirebaseApp.initializeApp(this)

        // ... (Your existing Prefs and RxJava init)
        Prefs.Builder()
            .setContext(this)
            .setMode(ContextWrapper.MODE_PRIVATE)
            .setPrefsName(packageName)
            .setUseDefaultSharedPreference(true)
            .build()

        RxJavaPlugins.setErrorHandler {
            Timber.e("Error: ${it.localizedMessage}")
        }



        if (Prefs.contains("notification_campaign").not() && BuildConfig.FLAVOR.equals(
                "netpos",
                true
            )
        ) {
            Firebase.messaging.subscribeToTopic("netpos_campaign")
                .addOnCompleteListener { task ->
                    var msg = "subscribed"
                    if (!task.isSuccessful) {
                        msg = "subscription failed"
                    } else {
                        Prefs.putBoolean("notification_campaign", true)
                    }
                    Timber.e(msg)
                }
        }
        if (checkBillsPaymentToken().not()) {
            getBillsToken(StormApiClient.getBillsInstance())
        }
        if (checkAppToken().not()) {
            getAppToken(StormApiClient.getBillsInstance()).subscribeOn(Schedulers.io())
                .retry(2)
                .observeOn(AndroidSchedulers.mainThread()).subscribe { _, _ ->
                }.disposeWith(CompositeDisposable())
        }



        if (TerminalDetector.isHorizonPay()) {
            bindHorizonDriverService()
        } else {
            // Default for Kozen or other terminals
            setupStandardNetPos()
        }

    }

    public fun bindHorizonDriverService() {
        PosAidlDeviceServiceUtil.connectDeviceService(this, object : PosAidlDeviceServiceUtil.DeviceServiceListen {
            override fun onConnected(device: IAidlDevice) {
                this@NetPosApp.device = device
                try {
                    // Reset and Init devices for K11
                    DeviceHelper.reset()
                    DeviceHelper.initDevices(this@NetPosApp)

                    // Now that hardware is ready, initialize NetPos SDK

                } catch (e: RemoteException) {
                    Timber.e("Horizon Driver Error: ${e.localizedMessage}")
                }
            }

            override fun error(errorcode: Int) {
                Timber.e("Horizon Driver Connection Failed: $errorcode")
            }

            override fun onDisconnected() {}
            override fun onUnCompatibleDevice() {}
        })
    }


    private fun setupStandardNetPos() {
        // Kozen usually initializes synchronously because the SDK is bundled differently
        initializeNetPosSdk()
    }

    private fun initializeNetPosSdk() {
        NetPosSdk.init()
        if (Prefs.contains("load_provided").not()) {
            try {
                NetPosSdk.loadProvidedCapksAndAids()
                NetPosSdk.loadEmvParams(TerminalParameters().apply { terminalCapability = "E068C8" })
                Prefs.putBoolean("load_provided", true)
            } catch (e: Exception) {
                Timber.e("EMV error: ${e.localizedMessage}")
            }
        }
    }
}