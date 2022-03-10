package com.woleapp.netpos.ui.fragments

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import com.netpluspay.nibssclient.models.TransactionType
import com.pixplicity.easyprefs.library.Prefs
import com.woleapp.netpos.BuildConfig
import com.woleapp.netpos.R
import com.woleapp.netpos.adapter.ServiceAdapter
import com.woleapp.netpos.databinding.FragmentTransactionsBinding
import com.woleapp.netpos.databinding.LayoutEnterPasswordBinding
import com.woleapp.netpos.databinding.LayoutPreauthDialogBinding
import com.woleapp.netpos.model.Service
import com.woleapp.netpos.util.HISTORY_ACTION_PREAUTH
import com.woleapp.netpos.util.HISTORY_ACTION_REFUND
import com.woleapp.netpos.util.HISTORY_ACTION_REPRINT
import com.woleapp.netpos.util.PREF_REPRINT_PASSWORD

class TransactionsFragment : BaseFragment() {

    private lateinit var adapter: ServiceAdapter
    private lateinit var binding: FragmentTransactionsBinding
    private lateinit var inputPasswordDialog: androidx.appcompat.app.AlertDialog
    private lateinit var passwordDialogBinding: LayoutEnterPasswordBinding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentTransactionsBinding.inflate(inflater, container, false)
        passwordDialogBinding =
            LayoutEnterPasswordBinding.inflate(LayoutInflater.from(requireContext()), null, false)
                .apply {
                    lifecycleOwner = viewLifecycleOwner
                    executePendingBindings()
                }
        inputPasswordDialog = androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setView(passwordDialogBinding.root)
            .create()
        passwordDialogBinding.proceed.setOnClickListener {
            if (passwordDialogBinding.passwordEdittext.text.toString() == Prefs.getString(
                    PREF_REPRINT_PASSWORD, ""
                )
            ) {
                inputPasswordDialog.cancel()
                addFragmentWithoutRemove(TransactionHistoryFragment.newInstance(action = HISTORY_ACTION_REPRINT))
            }else
                Toast.makeText(requireContext(), "Password is incorrect", Toast.LENGTH_SHORT).show()
        }
        inputPasswordDialog.setOnCancelListener {
            passwordDialogBinding.passwordEdittext.setText("")
        }
        inputPasswordDialog.setOnDismissListener {
            passwordDialogBinding.passwordEdittext.setText("")
        }
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        when (BuildConfig.FLAVOR) {
            "konga" -> setUpKongaAdapter()
            else -> setUpDefaultAdapter()
        }
        binding.rvTransactions.layoutManager = GridLayoutManager(context, 2)
        binding.rvTransactions.adapter = adapter
    }

    private fun showPreAuthDialog() {
        val dialog = AlertDialog.Builder(context)
            .apply {
                setCancelable(false)
            }.create()
        val preAuthDialogBinding =
            LayoutPreauthDialogBinding.inflate(LayoutInflater.from(context), null, false)
                .apply {
                    lifecycleOwner = viewLifecycleOwner
                    executePendingBindings()
                    preAuthNew.setOnClickListener {
                        dialog.dismiss()
                        addFragmentWithoutRemove(SalesFragment.newInstance(TransactionType.PRE_AUTHORIZATION))
                    }
                    preAuthComplete.setOnClickListener {
                        dialog.dismiss()
                        addFragmentWithoutRemove(
                            TransactionHistoryFragment.newInstance(
                                HISTORY_ACTION_PREAUTH
                            )
                        )
                    }
                    cancelButton.setOnClickListener {
                        dialog.dismiss()
                    }
                }
        dialog.setView(preAuthDialogBinding.root)
        dialog.show()
    }

    private fun setUpKongaAdapter() {
        adapter = ServiceAdapter {
            val nextFrag: Fragment? = when (it.id) {
                0 -> SalesFragment.newInstance()
                1 -> TransactionHistoryFragment.newInstance(action = HISTORY_ACTION_REFUND)
                2 -> {
                    showPreAuthDialog()
                    null
                }
                4 -> {
                    //showQRBottomSheetDialog()
                    QRFragment()
                }
                5 -> TransactionHistoryFragment.newInstance(action = HISTORY_ACTION_REPRINT)
                6 -> SalesFragment.newInstance(isVend = true)
                else -> SalesFragment.newInstance(TransactionType.CASH_ADVANCE)
            }
            nextFrag?.let { fragment ->
                addFragmentWithoutRemove(fragment)
            }
        }

        val listOfService = ArrayList<Service>()
            .apply {
                add(Service(0, "Purchase", R.drawable.ic_purchase))
                add(Service(5, "Reprint", R.drawable.ic_print))
            }
        adapter.submitList(listOfService)
    }

    private fun setUpDefaultAdapter() {
        adapter = ServiceAdapter {
            val nextFrag: Fragment? = when (it.id) {
                0 -> SalesFragment.newInstance()
                1 -> SalesFragment.newInstance(TransactionType.CASH)
                2 -> {
                    showPreAuthDialog()
                    null
                }
                4 -> {
                    //showQRBottomSheetDialog()
                    QRFragment()
                }
                5 -> {
                    if (BuildConfig.FLAVOR == "wema" && Prefs.contains(PREF_REPRINT_PASSWORD)){
                        inputPasswordDialog.show()
                        return@ServiceAdapter
                    }
                    TransactionHistoryFragment.newInstance(action = HISTORY_ACTION_REPRINT)
                }
                6 -> SalesFragment.newInstance(isVend = true)
                else -> SalesFragment.newInstance(TransactionType.CASH_ADVANCE)
            }
            nextFrag?.let { fragment ->
                addFragmentWithoutRemove(fragment)
            }
        }
        val listOfService = ArrayList<Service>()
            .apply {
                add(Service(0, "Purchase", R.drawable.ic_purchase))
                add(Service(1, "Cash", R.drawable.ic_baseline_money_24))
                add(Service(2, "PRE AUTHORIZATION", R.drawable.ic_pre_auth))
                add(Service(3, "Cash Advance", R.drawable.ic_pay_cash_icon))
                add(Service(4, "QR", R.drawable.ic_qr_code))
                add(Service(5, "Reprint", R.drawable.ic_print))
                add(Service(6, "VEND", R.drawable.ic_vend))
            }
        adapter.submitList(listOfService)
    }
}