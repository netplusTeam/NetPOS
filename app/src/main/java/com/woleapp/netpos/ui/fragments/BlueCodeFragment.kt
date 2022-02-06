package com.woleapp.netpos.ui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.GridLayoutManager
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.woleapp.netpos.R
import com.woleapp.netpos.adapter.ServiceAdapter
import com.woleapp.netpos.databinding.FragmentTransactionsBinding
import com.woleapp.netpos.databinding.QrAmoutDialogBinding
import com.woleapp.netpos.databinding.QrBottomSheetDialogBinding
import com.woleapp.netpos.model.Service
import com.woleapp.netpos.viewmodels.BlueCodeViewModel
import com.woleapp.netpos.viewmodels.NetPosViewModelFactories

class BlueCodeFragment : BaseFragment() {
    private val viewModel by viewModels<BlueCodeViewModel> {
        NetPosViewModelFactories()
    }
    private lateinit var adapter: ServiceAdapter
    private lateinit var binding: FragmentTransactionsBinding
    private lateinit var qrAmoutDialogBinding: QrAmoutDialogBinding
    private lateinit var qrAmountDialog: AlertDialog
    private lateinit var blueCodeQrBottomSheetDialogBinding: QrBottomSheetDialogBinding
    private lateinit var blueCodeQrBottomSheetDialog: BottomSheetDialog

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentTransactionsBinding.inflate(inflater, container, false).apply {
            lifecycleOwner = viewLifecycleOwner
            executePendingBindings()
        }

        qrAmoutDialogBinding = QrAmoutDialogBinding.inflate(inflater, null, false)
            .apply {
                executePendingBindings()
                lifecycleOwner = viewLifecycleOwner
            }
        qrAmountDialog = AlertDialog.Builder(requireContext()).apply {
            setView(qrAmoutDialogBinding.root)
        }.create()
        blueCodeQrBottomSheetDialogBinding =
            QrBottomSheetDialogBinding.inflate(
                layoutInflater, null, false
            ).apply {
                executePendingBindings()
                lifecycleOwner = viewLifecycleOwner
                this.providerQr.visibility = View.VISIBLE
                this.providerQr.setImageResource(R.drawable.ic_bluecode_logo)
            }
        blueCodeQrBottomSheetDialog =
            BottomSheetDialog(requireContext(), R.style.SheetDialog).apply {
                setCancelable(false)
                setContentView(blueCodeQrBottomSheetDialogBinding.root)
                blueCodeQrBottomSheetDialogBinding.closeBtn.setOnClickListener { this.cancel() }
            }
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.rvTransactionsHeader.text = getString(R.string.bluecode)
        adapter = ServiceAdapter {
            if (it.id == 0)
                showAmountDialog()
        }
        binding.rvTransactions.layoutManager = GridLayoutManager(context, 2)
        binding.rvTransactions.adapter = adapter
        viewModel.message.observe(viewLifecycleOwner) { message ->
            message.getContentIfNotHandled()?.let {
                Toast.makeText(requireContext(), it, Toast.LENGTH_LONG).show()
            }
        }
        viewModel.qrErrorMessage.observe(viewLifecycleOwner) { event ->
            event.getContentIfNotHandled()?.let {
                blueCodeQrBottomSheetDialogBinding.progress.visibility = View.GONE
                blueCodeQrBottomSheetDialogBinding.errorMessage.text = it
            }
        }
        viewModel.registerNewBlueCodeMerchant.observe(viewLifecycleOwner) { event ->
            event.getContentIfNotHandled()?.let {
                if (it) {
                    blueCodeQrBottomSheetDialog.cancel()
                    addFragmentWithoutRemove(
                        BlueCodeRegistrationFragment(),
                        fragmentName = BlueCodeRegistrationFragment::class.simpleName
                    )
                }
            }
        }
        viewModel.blueCodeQr.observe(viewLifecycleOwner) { event ->
            event.getContentIfNotHandled()?.let {
                blueCodeQrBottomSheetDialogBinding.progress.visibility = View.GONE
                blueCodeQrBottomSheetDialogBinding.qr.setImageBitmap(it)
            }
        }
        setService()
    }

    private fun setService() {
        val listOfService = ArrayList<Service>()
            .apply {
                add(Service(0, "Generate QR", R.drawable.ic_qr_code))
                add(Service(1, "Transactions", R.drawable.ic_trans))
                //add(Service(2, "Zenith QR", R.drawable.ic_zenith_logo))
            }
        adapter.submitList(listOfService)
    }

    private fun showAmountDialog() {
        qrAmountDialog.show()
        qrAmoutDialogBinding.proceed.setOnClickListener {
            val amountDouble = qrAmoutDialogBinding.amount.text.toString().toDoubleOrNull()
            amountDouble?.let {
                qrAmountDialog.cancel()
                blueCodeQrBottomSheetDialogBinding.closeBtn.visibility = View.VISIBLE
                blueCodeQrBottomSheetDialog.show()
                blueCodeQrBottomSheetDialogBinding.qr.setImageBitmap(null)
                blueCodeQrBottomSheetDialogBinding.errorMessage.text = ""
                blueCodeQrBottomSheetDialogBinding.progress.visibility = View.VISIBLE
                viewModel.getBlueCodeQr(it)
            }
        }
    }
}
