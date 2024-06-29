package com.woleapp.netpos.webview


import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import androidx.activity.OnBackPressedCallback
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.activityViewModels
import com.woleapp.netpos.R
import com.woleapp.netpos.databinding.FragmentWebviewBinding
import com.woleapp.netpos.di.customDependencies.JavaScriptInterface
import com.woleapp.netpos.di.customDependencies.WebViewCallBack
import com.woleapp.netpos.ui.fragments.BaseFragment
import com.woleapp.netpos.ui.fragments.DashboardFragment
import com.woleapp.netpos.util.STRING_TAG_JAVASCRIPT_INTERFACE_TAG
import com.woleapp.netpos.viewmodels.PayByZenithViewModel
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class WebViewFragment : BaseFragment() {

    private lateinit var binding: FragmentWebviewBinding
    private lateinit var webView: WebView
    private lateinit var webSettings: WebSettings
    private val qrViewModel by activityViewModels<PayByZenithViewModel>()
    private lateinit var javaScriptInterface: JavaScriptInterface

    @Inject
    lateinit var customWebViewClient: WebViewCallBack


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_webview, container, false)

        // Handle Back Press
        activity?.onBackPressedDispatcher?.addCallback(
            viewLifecycleOwner,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    if (webView.canGoBack()) {
                        webView.goBack()
                    } else {
                        showFragment(
                            targetFragment = DashboardFragment(),
                            className = "Dashboard Fragment"
                        )
                    }
                }
            }
        )
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        webView = binding.webView
        qrViewModel.payResponse.observe(viewLifecycleOwner) { response ->
            Log.d("NEW_DATA_WEB", response?.data.toString())
            response?.data?.let {
                javaScriptInterface = JavaScriptInterface(
                    parentFragmentManager,
                    null,
                    null,
                    null,
                    null,
                    it.transId,
                    it.redirectHtml
                )
            }
            setUpWebView(webView)
        } }

    private fun setUpWebView(webView: WebView) {
        webSettings = webView.settings
        webSettings.apply {
            javaScriptEnabled = true
            loadWithOverviewMode = true
            useWideViewPort = true
        }
        webView.apply {
            webViewClient = customWebViewClient
            webChromeClient = WebChromeClient()
            addJavascriptInterface(javaScriptInterface, STRING_TAG_JAVASCRIPT_INTERFACE_TAG)
            loadUrl("file:///android_asset/3ds_pay.html")
        }
    }
}