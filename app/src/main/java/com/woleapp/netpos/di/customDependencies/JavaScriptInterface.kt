package com.woleapp.netpos.di.customDependencies

import android.util.Log
import android.webkit.JavascriptInterface
import androidx.core.os.bundleOf
import androidx.fragment.app.FragmentManager
import com.google.gson.Gson
import com.pixplicity.easyprefs.library.Prefs
import com.woleapp.netpos.model.User
import com.woleapp.netpos.model.pay.QrTransactionResponseModel
import com.woleapp.netpos.ui.fragments.dialog.ResponseModal
import com.woleapp.netpos.util.*
import com.woleapp.netpos.util.RandomNumUtil.alertDialog

class JavaScriptInterface(
    private val fragmentManager: FragmentManager,
    private val termUrl: String?,
    private val md: String?,
    private val cReq: String?,
    private val acsUrl: String?,
    private val transId: String,
    private val redirectHtml: String,
) {
    private val context = fragmentManager.fragments.first().requireContext()

    // private val loader = RandomUtils.alertDialog(context, R.layout.layout_loading_dialog)
    private val loader = alertDialog(context)
    var isResponseHandled = false
    private val webViewBaseUrl =
        UtilityParams.STRING_WEB_VIEW_BASE_URL + UtilityParams.STRING_CHECKOUT_MERCHANT_ID + "/"

    @JavascriptInterface
    fun sendValueToWebView() = "$termUrl<======>$md<======>$cReq<======>$acsUrl<======>$transId<======>$webViewBaseUrl<======>$redirectHtml"

    @JavascriptInterface
    fun getUserData(): String {
        // Get the user object from Prefs
        val user = Singletons.gson.fromJson(Prefs.getString(PREF_USER, ""), User::class.java)
        // Return the data as a JSON string to the JavaScript code
        Log.d("WEB_USER", user.netplusPayMid.toString())
        return user.netplusPayMid.toString()
    }

    @JavascriptInterface
    fun webViewCallback(webViewResponse: String) {
        val responseFromWebView =
            Gson().fromJson(
                webViewResponse,
                QrTransactionResponseModel::class.java,
            )
        Log.d("NEW_DATA_JA", webViewResponse)
        Log.d("NEWVIEW", sendValueToWebView())
        Log.d("NEWVNETPLUSMID", getUserData())
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
                    bundleOf(MPGS_TRANSACTION_RESULT_BUNDLE_KEY to responseFromWebView),
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
