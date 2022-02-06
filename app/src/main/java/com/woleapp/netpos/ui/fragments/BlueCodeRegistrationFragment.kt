package com.woleapp.netpos.ui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.SearchView
import androidx.core.widget.doOnTextChanged
import androidx.fragment.app.viewModels
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.woleapp.netpos.adapter.MCCAdapter
import com.woleapp.netpos.databinding.DialogFragmentMccBinding
import com.woleapp.netpos.databinding.FragmentCreateBluecodeMerchantBinding
import com.woleapp.netpos.model.LoadingState
import com.woleapp.netpos.model.MCCDto
import com.woleapp.netpos.network.MCCService
import com.woleapp.netpos.util.banks
import com.woleapp.netpos.viewmodels.BlueCodeViewModel
import com.woleapp.netpos.viewmodels.NetPosViewModelFactories
import timber.log.Timber

class BlueCodeRegistrationFragment : BaseFragment() {

    private val viewModel by viewModels<BlueCodeViewModel> {
        NetPosViewModelFactories()
    }

    private lateinit var statesAdapter: ArrayAdapter<String>
    private lateinit var binding: FragmentCreateBluecodeMerchantBinding
    private lateinit var mccAlertDialog: AlertDialog
    private lateinit var mccDialogBinding: DialogFragmentMccBinding
    private lateinit var adapter: MCCAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentCreateBluecodeMerchantBinding.inflate(inflater, container, false).apply {
            lifecycleOwner = viewLifecycleOwner
            executePendingBindings()
            this.viewmodel = this@BlueCodeRegistrationFragment.viewModel
        }
        mccDialogBinding =
            DialogFragmentMccBinding.inflate(inflater, container, false).apply {
                lifecycleOwner = viewLifecycleOwner
                executePendingBindings()
            }
        mccAlertDialog = AlertDialog.Builder(
            requireContext(),
            android.R.style.Theme_Material_Light_NoActionBar_Fullscreen
        ).setView(mccDialogBinding.root)
            .create()
        adapter = MCCAdapter {
            mccAlertDialog.cancel()
            binding.getMCC.text = it.merchantCategoryDescription
            viewModel.setSelectedMerchantCategory(it)
        }
        binding.bankSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long
            ) {
                viewModel.setSelectedBank(banks[position])
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {

            }

        }
        binding.statesSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long
            ) {
                viewModel.setSelectedZip(viewModel.statesWithZip.value!!.peekContent()[position])
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {

            }

        }
        binding.register.setOnClickListener {
            validateFields(
                binding.accountNumber,
                binding.accountName,
                binding.address
            )
        }
        clearError(
            binding.accountNumber,
            binding.accountName,
            binding.address
        )
        return binding.root
    }

    private fun validateFields(vararg views: View) {
        for (view in views) {
            val text: String? = (view as? EditText)?.text?.toString()
            if (text.isNullOrEmpty()) {
                (view.parent.parent as TextInputLayout).error = "This field is required"
                view.requestFocus()
                return
            }
        }
        viewModel.registerBlueCodeMerchant()
    }

    private fun clearError(vararg views: View) {
        views.forEach {
            (it as TextInputEditText).doOnTextChanged { text, _, _, _ ->
                if (text.isNullOrEmpty().not()) {
                    (it.parent.parent as TextInputLayout).isErrorEnabled = false
                }
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel.fetchStates()
        binding.getMCC.setOnClickListener {
            mccAlertDialog.show()
            viewModel.getMCC(MCCDto(), MCCService.BLUECODE)
        }
        mccDialogBinding.mccList.adapter = adapter
        binding.bankSpinner.adapter =
            ArrayAdapter(
                requireContext(),
                android.R.layout.simple_spinner_dropdown_item,
                banks.map { it.name })
        statesAdapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_dropdown_item,
            arrayListOf()
        )
        binding.statesSpinner.adapter = statesAdapter
        viewModel.statesWithZip.observe(viewLifecycleOwner) { event ->
            event.getContentIfNotHandled()?.let {
                statesAdapter.addAll(it.map { states -> states.state })
                statesAdapter.notifyDataSetChanged()
            }
        }

        viewModel.message.observe(viewLifecycleOwner) { event ->
            event.getContentIfNotHandled()?.let {
                Toast.makeText(requireContext(), it, Toast.LENGTH_LONG).show()
            }
        }

        viewModel.registrationComplete.observe(viewLifecycleOwner) { event ->
            event.getContentIfNotHandled()?.let {
                if (it)
                    requireActivity().onBackPressed()
            }
        }

        viewModel.loadingStateLiveData.observe(viewLifecycleOwner) { event ->
            event.getContentIfNotHandled()?.let {
                when (it.loadingState) {
                    LoadingState.LOADING_COMPLETE -> {
                        mccDialogBinding.loadingMore.visibility = View.GONE
                        mccDialogBinding.progressHorizontal.visibility = View.GONE
                    }
                    LoadingState.LOADING_FAILED -> {
                        Toast.makeText(
                            requireContext(),
                            "An error occurred while fetching",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                    LoadingState.LOADING_MORE -> mccDialogBinding.loadingMore.visibility =
                        View.VISIBLE
                    LoadingState.LOADING_INITIAL -> mccDialogBinding.progressHorizontal.visibility =
                        View.VISIBLE
                }
                Timber.e(it.loadingState.name)
            }
        }
        viewModel.zenithMccList.observe(viewLifecycleOwner) {
            Timber.e("list in fragment: ${it.size}")
            adapter.submitList(it)
        }
        viewModel.initSearchFilter(MCCService.BLUECODE)
        mccDialogBinding.search.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                viewModel.textChangeComplete()
                return false
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                newText?.let {
                    viewModel.textChanged(filter = it)
                }
                return false
            }

        })
    }
}