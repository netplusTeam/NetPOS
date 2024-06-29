package com.woleapp.netpos.ui.fragments.dialog

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.setFragmentResultListener
import com.airbnb.lottie.LottieAnimationView
import com.woleapp.netpos.R
import com.woleapp.netpos.databinding.TransactionStatusModalBinding
import com.woleapp.netpos.di.customDependencies.JavaScriptInterface
import com.woleapp.netpos.model.pay.ModalData
import com.woleapp.netpos.model.pay.QrTransactionResponseModel
import com.woleapp.netpos.ui.fragments.DashboardFragment
import com.woleapp.netpos.util.MPGS_TRANSACTION_RESULT_BUNDLE_KEY
import com.woleapp.netpos.util.MPGS_TRANSACTION_RESULT_REQUEST_KEY
import com.woleapp.netpos.util.RandomNumUtil.formatCurrency
import com.woleapp.netpos.viewmodels.PayByZenithViewModel
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class ResponseModal @Inject constructor() : DialogFragment() {
    private lateinit var binding: TransactionStatusModalBinding
    private lateinit var lottieIcon: LottieAnimationView
    private lateinit var statusTv: TextView
    private lateinit var cancelBtn: ImageView
    private lateinit var doneBtn: Button
    private lateinit var viewGeneratedQR: Button
    private lateinit var amountTv: TextView
    private var modalData: ModalData? = null
    private var responseFromWebView: Any? = null
    private val qrViewModel by activityViewModels<PayByZenithViewModel>()


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setFragmentResultListener(MPGS_TRANSACTION_RESULT_REQUEST_KEY) { _, bundle ->
            responseFromWebView = bundle.getParcelable(MPGS_TRANSACTION_RESULT_BUNDLE_KEY)
            val webViewResponse = responseFromWebView
            modalData = if (webViewResponse != null) {
                when (webViewResponse) {
                    is QrTransactionResponseModel -> {
                        if (webViewResponse.code == "00") {
                            viewGeneratedQR.visibility = View.VISIBLE
                        }
                        Log.d("NEW_DATA_W", webViewResponse.toString())
//                        transactionViewModel.saveQrTransaction(webViewResponse)
                        ModalData(
                            webViewResponse.code == "00", webViewResponse.amount.toDouble()
                        )
                    }
                    else -> {
                        ModalData(
                            false, 0.0
                        )
                    }
                }
            } else {
                ModalData(
                    false, 0.0
                )
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        binding =
            DataBindingUtil.inflate(inflater, R.layout.transaction_status_modal, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initViews()
        dialog?.window?.apply {
            setBackgroundDrawableResource(R.drawable.curve_bg)
            isCancelable = false
        }

    }

    override fun onResume() {
        super.onResume()
        setData()
        cancelBtn.setOnClickListener {
            dialog?.dismiss()
            showFragment(
                targetFragment = DashboardFragment(),
                className = "Dashboard Fragment"
            )
        }
        doneBtn.setOnClickListener {
            dialog?.dismiss()
            showFragment(
                targetFragment = DashboardFragment(),
                className = "Dashboard Fragment"
            )
        }
        qrViewModel.payResponse.removeObservers(viewLifecycleOwner)
        qrViewModel._payResponse.value = null
    }

    private fun initViews() {
        with(binding) {
            lottieIcon = statusIconLAV
            statusTv = successFailed
            cancelBtn = cancelButton
            amountTv = qrAmount
            doneBtn = done
        }
    }

    private fun setData() {
        modalData?.let {
            if (it.status) {
                lottieIcon.setAnimation(R.raw.lottiesuccess)
                statusTv.text = getString(R.string.success_mpgs)
                statusTv.setTextColor(resources.getColor(R.color.success))
            } else {
                lottieIcon.setAnimation(R.raw.failed)
                statusTv.text = getString(R.string.failed_mpgs)
                statusTv.setTextColor(resources.getColor(R.color.failed))
            }
            amountTv.text = it.amount.toLong().formatCurrency("\u20A6")
        }
    }

    private fun showFragment(targetFragment: Fragment?, className: String?) {
        val ft = requireActivity().supportFragmentManager.beginTransaction()
        ft.setCustomAnimations(R.anim.right_to_left, R.anim.left_to_right)
        ft.replace(R.id.container_main, targetFragment!!, className)
        ft.commitAllowingStateLoss()
    }
}
