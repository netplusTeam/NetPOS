package com.woleapp.netpos.ui.fragments

import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Base64
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.bumptech.glide.Glide
import com.pixplicity.easyprefs.library.Prefs
import com.woleapp.netpos.databinding.FragmentDisplayQrResultBinding
import com.woleapp.netpos.model.AppConstants
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class DisplayQrResultFragment : BaseFragment() {

    private lateinit var binding: FragmentDisplayQrResultBinding
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        binding = FragmentDisplayQrResultBinding.inflate(inflater, container, false).apply {
            lifecycleOwner = viewLifecycleOwner
            executePendingBindings()
        }
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val displayQrResult =
            Prefs.getString(AppConstants.PAYMENT_WITH_QR_STRING, "")

        val imageBytes = Base64.decode(displayQrResult, Base64.DEFAULT)
        val decodedImage = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)

        if (displayQrResult.isNotEmpty()) {
            Glide.with(requireContext()).load(decodedImage).into(binding.paymentQr)
        }
    }
}