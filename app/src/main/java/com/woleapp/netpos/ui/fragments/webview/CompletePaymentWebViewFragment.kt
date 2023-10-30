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
import com.woleapp.netpos.util.CONTACTLESS_TRANSACTION_DEFAULT_EMAIL
import com.woleapp.netpos.util.PREF_USER
import com.woleapp.netpos.util.Singletons


class CompletePaymentWebViewFragment : Fragment() {

    private lateinit var binding: FragmentCompletePaymentWebViewBinding
    private lateinit var webView: WebView
    private lateinit var webSettings: WebSettings
    private var netposID: String? = null
    private var userTID: String? = null
    private var email: String? = null
    private var name: String? = null
    private var amount: String? = null

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
        val user = Singletons.gson.fromJson(Prefs.getString(PREF_USER, ""), User::class.java)
        netposID = user.netplus_id
        userTID = user.terminal_id
        name = user.business_name
        email = CONTACTLESS_TRANSACTION_DEFAULT_EMAIL

         amount = arguments?.getString("key")
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
          //  loadUrl("https://qrpay.paysaddle.com/payment/#!/card?NPmerchantId=${MID635ff140365c5}&terminalId=${2033ALZP}&netposId=${2222SPTCM001048}&amount=${5}&name=${CUSTOMER}&email=${contact@gmail.com}&bank=${providus}&app=${contact}")
        }
    }
}