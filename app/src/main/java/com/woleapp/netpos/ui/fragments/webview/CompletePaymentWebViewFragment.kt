package com.woleapp.netpos.ui.fragments.webview

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import androidx.activity.OnBackPressedCallback
import com.pixplicity.easyprefs.library.Prefs
import com.woleapp.netpos.R
import com.woleapp.netpos.databinding.FragmentCompletePaymentWebViewBinding
import com.woleapp.netpos.model.User
import com.woleapp.netpos.util.*
import com.woleapp.netpos.util.RandomNumUtil.getBankName
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class CompletePaymentWebViewFragment : Fragment() {

    private lateinit var binding: FragmentCompletePaymentWebViewBinding
    private lateinit var webView: WebView
    private lateinit var webSettings: WebSettings
    private var netposID: String? = null
    private var userTID: String? = null
    private var email: String? = null
    private var name: String? = null
    private var amount: String? = null
    private var bank: String? = null
    private var netplusPayMid: String? = null
    private var merchantID: String? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        binding = FragmentCompletePaymentWebViewBinding.inflate(inflater, container, false)
        // Handle Back Press
        activity?.onBackPressedDispatcher?.addCallback(
            viewLifecycleOwner,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    if (webView.canGoBack()) {
                        webView.goBack()
                    } else {
                        requireActivity().supportFragmentManager.popBackStack()
                    }
                }
            }
        )
        return  binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        webView = binding.loadWebView
        bank = getBankName() ?: ""
        val user = Singletons.gson.fromJson(Prefs.getString(PREF_USER, ""), User::class.java)
        netposID = user.netplus_id
        userTID = user.terminal_id
        netplusPayMid = user.merchantId
        merchantID = user.netplusPayMid
        email = CONTACTLESS_TRANSACTION_DEFAULT_EMAIL

         amount = arguments?.getString(PAYMENT_KEY)
        Log.d("AMOUTNNT", amount.toString())
        setUpWebView(webView)
    }

    private fun setUpWebView(webView: WebView) {
        webSettings = webView.settings
        webSettings.apply {
            javaScriptEnabled = true
            loadWithOverviewMode = true
            useWideViewPort = true
        }
        webView.apply {
            webChromeClient = WebChromeClient()
            Log.d("PAYMENTURL", "https://qrpay.paysaddle.com/payment/#!/card?NPmerchantId=${merchantID}&terminalId=${userTID}&netposId=${netplusPayMid}&amount=${amount}&name=${CUSTOMER}&email=${CONTACTLESS_TRANSACTION_DEFAULT_EMAIL}&bank=${bank}&app=${CONTACT}&currency=NGN")
            loadUrl("https://qrpay.paysaddle.com/payment/#!/card?NPmerchantId=${merchantID}&terminalId=${userTID}&netposId=${netplusPayMid}&amount=${amount}&name=${CUSTOMER}&email=${CONTACTLESS_TRANSACTION_DEFAULT_EMAIL}&bank=${bank}&app=${CONTACT}&currency=NGN")
        }
    }
}