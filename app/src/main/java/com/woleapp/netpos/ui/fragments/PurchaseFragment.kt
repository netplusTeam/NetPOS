package com.woleapp.netpos.ui.fragments

import android.app.AlertDialog
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.widget.doOnTextChanged
import com.google.android.material.textfield.TextInputEditText
import com.pixplicity.easyprefs.library.Prefs
import com.woleapp.netpos.R
import com.woleapp.netpos.databinding.FragmentPurchaseBinding
import com.woleapp.netpos.model.Alerter.showToast
import com.woleapp.netpos.model.User
import com.woleapp.netpos.ui.fragments.webview.CompletePaymentWebViewFragment
import com.woleapp.netpos.util.CONTACTLESS_TRANSACTION_DEFAULT_EMAIL
import com.woleapp.netpos.util.PREF_USER
import com.woleapp.netpos.util.Singletons.gson


class PurchaseFragment : BaseFragment() {

    private lateinit var binding: FragmentPurchaseBinding
    private lateinit var amount: TextInputEditText


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        binding = FragmentPurchaseBinding.inflate(inflater, container, false)

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initViews()

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
                    val bundle = Bundle()
                    bundle.putString("key", amount.text.toString())
                    val fragment = PurchaseFragment()
                    fragment.arguments = bundle
                    addFragmentWithoutRemove(CompletePaymentWebViewFragment())
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


}