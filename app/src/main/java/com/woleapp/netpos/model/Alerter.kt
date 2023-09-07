package com.woleapp.netpos.model

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.widget.Toast

object Alerter {
    fun showToast(message: String, context: Context) {
        Handler(Looper.getMainLooper()).post {
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        }
    }
}
