package com.woleapp.netpos.ui.fragments

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.widget.doOnTextChanged
import androidx.fragment.app.activityViewModels
import com.google.android.material.textfield.TextInputEditText
import com.pixplicity.easyprefs.library.Prefs
import com.woleapp.netpos.R
import com.woleapp.netpos.databinding.FragmentDisplayQrBinding
import com.woleapp.netpos.model.PayWithQrRequest
import com.woleapp.netpos.model.User
import com.woleapp.netpos.util.CONTACTLESS_TRANSACTION_DEFAULT_EMAIL
import com.woleapp.netpos.util.PREF_USER
import com.woleapp.netpos.util.RandomNumUtil
import com.woleapp.netpos.util.RandomNumUtil.observeServerResponse
import com.woleapp.netpos.util.Singletons
import com.woleapp.netpos.viewmodels.ContactQrPaymentViewModel
import dagger.hilt.android.AndroidEntryPoint
import io.reactivex.Scheduler
import io.reactivex.disposables.CompositeDisposable
import javax.inject.Inject
import javax.inject.Named

@AndroidEntryPoint
class DisplayQrFragment : BaseFragment() {

    private lateinit var binding: FragmentDisplayQrBinding
    private var netposID: String? = null
    private var userTID: String? = null
    private var email: String? = null
    private var name: String? = null
    private lateinit var amount: TextInputEditText
    private val viewModel by activityViewModels<ContactQrPaymentViewModel>()
    private lateinit var loader: AlertDialog

    @Inject
    lateinit var compositeDisposable: CompositeDisposable

    @Inject
    @Named("io-scheduler")
    lateinit var ioScheduler: Scheduler

    @Inject
    @Named("main-scheduler")
    lateinit var mainThreadScheduler: Scheduler

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        binding = FragmentDisplayQrBinding.inflate(inflater, container, false).apply {
            lifecycleOwner = viewLifecycleOwner
            executePendingBindings()
        }
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initViews()
        val user = Singletons.gson.fromJson(Prefs.getString(PREF_USER, ""), User::class.java)
        netposID = user.netplus_id
        userTID = user.terminal_id
        name = user.business_name
        email = CONTACTLESS_TRANSACTION_DEFAULT_EMAIL

        loader = RandomNumUtil.alertDialog(requireContext())

        binding.process.setOnClickListener {
            processPayment()
        }
    }

    private fun initViews() {
        with(binding) {
            amount = priceTextBox
        }
    }

    private fun processPayment() {
        when {
            amount.text.toString().isEmpty() -> {
                showToast(getString(R.string.all_please_enter_amount))
            }
            else -> {
                if (validateSignUpFieldsOnTextChange()) {
                    generateMerchantQr()
                }
            }
        }
    }

    private fun validateSignUpFieldsOnTextChange(): Boolean {
        var isValidated = true

        amount.doOnTextChanged { _, _, _, _ ->
            when {
                amount.text.toString().trim().isEmpty() -> {
                    showToast(getString(R.string.all_please_enter_amount))
                    isValidated = false
                }
                else -> {
                    binding.priceInputLayout.error = null
                    isValidated = true
                }
            }
        }
        return isValidated
    }

    private fun generateMerchantQr() {
        loader.show()
        val paymentWithQr = PayWithQrRequest(
            terminalId = userTID.toString(),
            netposId = Singletons.getConfigData()?.cardAcceptorIdCode ?: "",
            amount = amount.text.toString(),
            name = name.toString(),
            email = email.toString(),
            bank = RandomNumUtil.getBankName() ?: "",
            NPmerchantId = RandomNumUtil.getNetPlusPayMid(),
        )

        observeServerResponse(
            viewModel.paymentWithQr(paymentWithQr),
            loader,
            compositeDisposable,
            ioScheduler,
            mainThreadScheduler,
        ) {
            showFragment(
                targetFragment = DisplayQrResultFragment(),
                className = "Display QR Result"
            )
        }
    }


}