package com.woleapp.netpos.ui.fragments.dialog

import android.Manifest
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.databinding.DataBindingUtil
import androidx.databinding.ViewDataBinding
import androidx.fragment.app.*
import com.airbnb.lottie.LottieAnimationView
import com.danbamitale.epmslib.entities.CardData
import com.danbamitale.epmslib.entities.TransactionResponse
import com.danbamitale.epmslib.utils.IsoAccountType
import com.pixplicity.easyprefs.library.Prefs
import com.woleapp.netpos.R
import com.woleapp.netpos.database.AppDatabase
import com.woleapp.netpos.database.dao.TransactionResponseDao
import com.woleapp.netpos.databinding.LayoutPosReceiptWemaPdfBinding
import com.woleapp.netpos.databinding.TransactionStatusModalBinding
import com.woleapp.netpos.model.pay.ModalData
import com.woleapp.netpos.model.pay.QrTransactionResponseModel
import com.woleapp.netpos.ui.fragments.DashboardFragment
import com.woleapp.netpos.util.*
import com.woleapp.netpos.util.ModelMapper.mapMpgsDataToTransactionResponse
import com.woleapp.netpos.util.RandomNumUtil.showSnackBar
import com.woleapp.netpos.util.pdfUtils.createPdf
import com.woleapp.netpos.util.pdfUtils.initViewsForMpgsPdfLayout
import com.woleapp.netpos.util.pdfUtils.sharePdf
import com.woleapp.netpos.viewmodels.PayByZenithViewModel
import dagger.hilt.android.AndroidEntryPoint
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import timber.log.Timber
import java.io.File
import javax.inject.Inject

@AndroidEntryPoint
class ResponseModal
    @Inject
    constructor() : DialogFragment() {
        private lateinit var binding: TransactionStatusModalBinding
        private lateinit var lottieIcon: LottieAnimationView
        private lateinit var statusTv: TextView
        private lateinit var cancelBtn: ImageView
        private lateinit var downloadAndShareBtn: Button
        private lateinit var amountTv: TextView
        private var modalData: ModalData? = null
        private var responseFromWebView: Any? = null
        private var newWebViewResponse: TransactionResponse? = null
        private val qrViewModel by activityViewModels<PayByZenithViewModel>()
        private lateinit var transactionResponseDao: TransactionResponseDao
        private lateinit var receiptPdf: File
        private lateinit var pdfView: LayoutPosReceiptWemaPdfBinding

        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)
            transactionResponseDao = AppDatabase.getDatabaseInstance(requireContext()).transactionResponseDao()
            setFragmentResultListener(MPGS_TRANSACTION_RESULT_REQUEST_KEY) { _, bundle ->
                responseFromWebView = bundle.getParcelable(MPGS_TRANSACTION_RESULT_BUNDLE_KEY)
                val webViewResponse = responseFromWebView
                modalData =
                    if (webViewResponse != null) {
                        when (webViewResponse) {
                            is QrTransactionResponseModel -> {
                                Log.d("NEW_DATA_W", webViewResponse.toString())
                                val cardData = Singletons.gson.fromJson(Prefs.getString(PREF_CARD_DATA, ""), CardData::class.java)
                                val isoAccountType =
                                    Singletons.gson.fromJson(
                                        Prefs.getString(PREF_ISO_ACCOUNT_TYPE, ""),
                                        IsoAccountType::class.java,
                                    )
                                val transResp =
                                    webViewResponse.mapMpgsDataToTransactionResponse(
                                        cardData,
                                        isoAccountType,
                                    ).builder().toString()
                                newWebViewResponse = webViewResponse.mapMpgsDataToTransactionResponse(cardData, isoAccountType)
                                transactionResponseDao.insertNewTransaction(
                                    webViewResponse.mapMpgsDataToTransactionResponse(
                                        cardData,
                                        isoAccountType,
                                    ),
                                ).subscribeOn(Schedulers.io())
                                    .observeOn(AndroidSchedulers.mainThread())
                                    .subscribe { t1, t2 ->
                                        t1?.let { Log.d("RES_OKKK", it.toString()) }
                                        t2?.let { Timber.e(it.localizedMessage) }
                                    }
                                ModalData(
                                    webViewResponse.code == "00",
                                    webViewResponse.amount.toDouble(),
                                )
                            }
                            else -> {
                                ModalData(
                                    false,
                                    0.0,
                                )
                            }
                        }
                    } else {
                        ModalData(
                            false,
                            0.0,
                        )
                    }
            }
        }

        override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?,
        ): View {
            // Inflate the layout for this fragment
            binding =
                DataBindingUtil.inflate(inflater, R.layout.transaction_status_modal, container, false)
            return binding.root
        }

        override fun onViewCreated(
            view: View,
            savedInstanceState: Bundle?,
        ) {
            super.onViewCreated(view, savedInstanceState)
            pdfView = LayoutPosReceiptWemaPdfBinding.inflate(layoutInflater)
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
                    className = "Dashboard Fragment",
                )
            }
            downloadAndShareBtn.setOnClickListener {
                dialog?.dismiss()
                showFragment(
                    targetFragment = DashboardFragment(),
                    className = "Dashboard Fragment",
                )
                downloadPdfImpl()
                showSnackBar(
                    binding.root,
                    getString(R.string.fileDownloaded),
                )
                sharePdf(receiptPdf, this)
            }
            qrViewModel.payResponse.removeObservers(viewLifecycleOwner)
            qrViewModel._payResponse.value = null
        }

        private fun initViews() {
            with(binding) {
                cancelBtn = cancelButton
                downloadAndShareBtn = sendButton
                amountTv = transactionContent
            }
        }

        private fun setData() {
            amountTv.text = newWebViewResponse?.buildMpgsText(newWebViewResponse!!).toString()
        }

        private fun downloadPdfImpl() {
            newWebViewResponse?.let { transResponse ->
                initViewsForMpgsPdfLayout(
                    pdfView,
                    transResponse,
                )
                getPermissionAndCreatePdf(pdfView)
            }
        }

        private fun getPermissionAndCreatePdf(view: ViewDataBinding) {
            ModelMapper.genericPermissionHandler(
                requireActivity(),
                requireContext(),
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                WRITE_PERMISSION_REQUEST_CODE,
                getString(R.string.storage_permission_rationale_for_download),
            ) {
                receiptPdf = createPdf(view, this)
            }
        }

        private fun showFragment(
            targetFragment: Fragment?,
            className: String?,
        ) {
            val ft = requireActivity().supportFragmentManager.beginTransaction()
            ft.setCustomAnimations(R.anim.right_to_left, R.anim.left_to_right)
            ft.replace(R.id.container_main, targetFragment!!, className)
            ft.commitAllowingStateLoss()
        }
    }
