package com.woleapp.netpos.di.customDependencies

import android.util.Log
import android.webkit.JavascriptInterface
import androidx.core.os.bundleOf
import androidx.fragment.app.FragmentManager
import com.google.gson.Gson
import com.woleapp.netpos.model.pay.QrTransactionResponseModel
import com.woleapp.netpos.ui.fragments.dialog.ResponseModal
import com.woleapp.netpos.util.MPGS_TRANSACTION_RESULT_BUNDLE_KEY
import com.woleapp.netpos.util.MPGS_TRANSACTION_RESULT_REQUEST_KEY
import com.woleapp.netpos.util.RandomNumUtil.alertDialog
import com.woleapp.netpos.util.STRING_MPGS_RESPONSE_MODAL_DIALOG_TAG
import com.woleapp.netpos.util.UtilityParams


class JavaScriptInterface(
    private val fragmentManager: FragmentManager,
    private val termUrl: String?,
    private val md: String?,
    private val cReq: String?,
    private val acsUrl: String?,
    private val transId: String,
    private val redirectHtml: String
) {
    private val context = fragmentManager.fragments.first().requireContext()

    // private val loader = RandomUtils.alertDialog(context, R.layout.layout_loading_dialog)
    private val loader = alertDialog(context)
    var isResponseHandled = false
    private val webViewBaseUrl =
        UtilityParams.STRING_WEB_VIEW_BASE_URL + UtilityParams.STRING_CHECKOUT_MERCHANT_ID + "/"

    @JavascriptInterface
    fun sendValueToWebView() =
        "$termUrl<======>$md<======>$cReq<======>$acsUrl<======>$transId<======>$webViewBaseUrl<======>$redirectHtml"

    @JavascriptInterface
    fun webViewCallback(webViewResponse: String) {
        val responseFromWebView = Gson().fromJson(
            webViewResponse,
            QrTransactionResponseModel::class.java
        )
        Log.d("NEW_DATA_JA", webViewResponse)
        if (responseFromWebView.code == "00" || responseFromWebView.code == "90" || responseFromWebView.code == "80") {
            if (loader.isShowing) {
                fragmentManager.fragments.first().requireActivity().runOnUiThread {
                    loader.dismiss()
                }
            }
            if (!isResponseHandled) {
                isResponseHandled = true
                loader.dismiss()
                fragmentManager.setFragmentResult(
                    MPGS_TRANSACTION_RESULT_REQUEST_KEY,
                    bundleOf(MPGS_TRANSACTION_RESULT_BUNDLE_KEY to responseFromWebView)
                )
                fragmentManager.popBackStack()
                val responseModal = ResponseModal()
                responseModal.show(fragmentManager, STRING_MPGS_RESPONSE_MODAL_DIALOG_TAG)
            }
        } else {
            fragmentManager.fragments.first().requireActivity().runOnUiThread {
                loader.dismiss()
            }
        }
    }
}
