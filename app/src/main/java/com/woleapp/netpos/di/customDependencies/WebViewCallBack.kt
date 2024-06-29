package com.woleapp.netpos.di.customDependencies

import android.util.Log
import android.webkit.WebView
import android.webkit.WebViewClient
import javax.inject.Inject
import javax.inject.Singleton


@Singleton
class WebViewCallBack @Inject constructor() : WebViewClient() {

    @Deprecated("Deprecated in Java", ReplaceWith("true"))
    override fun shouldOverrideUrlLoading(view: WebView?, url: String?): Boolean = true

    @Deprecated("Deprecated in Java", ReplaceWith(
        "super.onReceivedError(view, errorCode, description, failingUrl)",
        "android.webkit.WebViewClient"
    )
    )
    override fun onReceivedError(
        view: WebView?,
        errorCode: Int,
        description: String?,
        failingUrl: String?
    ) {
        super.onReceivedError(view, errorCode, description, failingUrl)
        Log.d("WEBVIEWERROR", errorCode.toString())
    }

}
