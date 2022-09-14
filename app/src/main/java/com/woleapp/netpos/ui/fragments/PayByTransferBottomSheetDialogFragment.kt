package com.woleapp.netpos.ui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.databinding.DataBindingUtil
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.woleapp.netpos.R
import com.woleapp.netpos.databinding.LayoutPayWithTransferBinding
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
@AndroidEntryPoint
class PayByTransferBottomSheetDialogFragment @Inject constructor() : BottomSheetDialogFragment() {
    private lateinit var binding: LayoutPayWithTransferBinding
    private lateinit var bankNameTv: TextView
    private lateinit var accountNumTv: TextView
    private lateinit var accountNameTv: TextView
    private lateinit var buttonDone: Button

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        binding =
            DataBindingUtil.inflate(inflater, R.layout.layout_pay_with_transfer, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        init()
    }

    override fun onResume() {
        super.onResume()
        buttonDone.setOnClickListener {
            dismiss()
        }
    }

    private fun init() {
        with(binding) {
            bankNameTv = bank
            accountNameTv = accountName
            accountNumTv = accountNumber
            buttonDone = btnDone
        }
    }
}
