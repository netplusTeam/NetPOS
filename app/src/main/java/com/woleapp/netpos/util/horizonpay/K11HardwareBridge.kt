package com.woleapp.netpos.util.horizonpay

import android.content.Context
import android.os.Handler
import android.os.Looper
import com.woleapp.netpos.app.DeviceHelper
import com.woleapp.netpos.app.NetPosApp

object K11HardwareBridge {
    private var isInitialized = false

    // Initialize this in your Splash Activity or Login Screen
    fun connectService(context: Context, onReady: (Boolean) -> Unit) {
        if (isInitialized) { onReady(true); return }

        // Start the binding process
        DeviceHelper.initDevices(context.applicationContext as NetPosApp)

        // Polling check for service readiness
        val handler = Handler(Looper.getMainLooper())
        val checkStatus = object : Runnable {
            var attempts = 0
            override fun run() {
                if (DeviceHelper.getDevice() != null) {
                    isInitialized = true
                    onReady(true)
                } else if (attempts < 10) {
                    attempts++
                    handler.postDelayed(this, 300) // Check every 300ms
                } else {
                    onReady(false) // Service failed to bind
                }
            }
        }
        handler.post(checkStatus)
    }

    // SAFE GETTERS
    fun getSecureSN(): String {
        return try {
            // This is the "Horizon Way" that avoids the SecurityException
            val system = DeviceHelper.getSysHandle()
            system?.getSn() ?: "UNKNOWN_SN"
        } catch (e: Exception) {
            "ERROR_SN"
        }
    }
}