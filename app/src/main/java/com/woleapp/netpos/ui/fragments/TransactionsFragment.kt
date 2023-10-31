package com.woleapp.netpos.ui.fragments

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.work.Constraints
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.danbamitale.epmslib.entities.TransactionType
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
import com.woleapp.netpos.worker.RepushFailedTransactionToBackendWorker

class TransactionsFragment : BaseFragment() {
    private lateinit var workManager: WorkManager
    private lateinit var adapter: ServiceAdapter
    private lateinit var binding: FragmentTransactionsBinding
    private lateinit var inputPasswordDialog: androidx.appcompat.app.AlertDialog
    private lateinit var passwordDialogBinding: LayoutEnterPasswordBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        workManager = WorkManager.getInstance(requireActivity().applicationContext)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
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
                    PREF_REPRINT_PASSWORD,
                    "",
                )
            ) {
                inputPasswordDialog.cancel()
                addFragmentWithoutRemove(TransactionHistoryFragment.newInstance(action = HISTORY_ACTION_REPRINT))
            } else {
                Toast.makeText(requireContext(), "Password is incorrect", Toast.LENGTH_SHORT).show()
            }
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
            "aellacredit" -> setUpAdapterForAellaCredit()
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
                                HISTORY_ACTION_PREAUTH,
                            ),
                        )
                    }
                    cancelButton.setOnClickListener {
                        dialog.dismiss()
                    }
                }
        dialog.setView(preAuthDialogBinding.root)
        dialog.show()
    }

    override fun onResume() {
        super.onResume()
        repushTransactionsToBackend()
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
                    // showQRBottomSheetDialog()
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
                1 -> SalesFragment.newInstance(TransactionType.DEPOSIT)
                2 -> {
                    showPreAuthDialog()
                    null
                }
                4 -> {
//                    showQRBottomSheetDialog()
                    QRFragment()
                }
                5 -> {
                    if ((BuildConfig.FLAVOR == "wema" || BuildConfig.FLAVOR == "zenith") && Prefs.contains(
                            PREF_REPRINT_PASSWORD,
                        )
                    ) {
                        inputPasswordDialog.show()
                        return@ServiceAdapter
                    }
                    ReprintFragment()
//                    TransactionHistoryFragment.newInstance(action = HISTORY_ACTION_REPRINT)
                }
//                6 -> SalesFragment.newInstance(isVend = true)
                6 -> ZenithPayByTransferFragment()
                7 -> PurchaseFragment()
                else -> SalesFragment.newInstance(TransactionType.CASH_ADVANCE)
            }
            nextFrag?.let { fragment ->
                addFragmentWithoutRemove(fragment)
            }
        }
        val listOfService = if (BuildConfig.FLAVOR.equals("wemacashout", true)) {
            arrayListOf(
                Service(0, "Cash Out", R.drawable.ic_purchase),
                Service(2, "PRE AUTHORIZATION", R.drawable.ic_pre_auth),
                Service(4, "QR", R.drawable.ic_qr_code),
                Service(5, "Reprint", R.drawable.ic_print),
                Service(6, "VEND", R.drawable.ic_vend),
            )
        } else {
            val serviceArr = arrayListOf(
                Service(0, "Purchase", R.drawable.ic_purchase),
                Service(1, "Cash", R.drawable.ic_baseline_money_24),
                Service(2, "PRE AUTHORIZATION", R.drawable.ic_pre_auth),
                Service(3, "Cash Advance", R.drawable.ic_pay_cash_icon),
                Service(4, "QR", R.drawable.ic_qr_code),
                Service(5, "Reprint", R.drawable.ic_print),
                Service(7, "Purchase", R.drawable.ic_purchase),
            )
            if (BuildConfig.FLAVOR.equals("zenith", true)) {
                serviceArr.add(
                    Service(
                        6,
                        getString(R.string.pay_by_transfer),
                        R.drawable.ic_lending,
                    ),
                )
            }
            serviceArr
        }

        adapter.submitList(listOfService)
    }

    private fun setUpAdapterForAellaCredit() {
        adapter = ServiceAdapter {
            val nextFrag: Fragment? = when (it.id) {
                0 -> SalesFragment.newInstance()
                1 -> SalesFragment.newInstance(TransactionType.DEPOSIT)
                2 -> {
                    showPreAuthDialog()
                    null
                }
                4 -> {
//                    showQRBottomSheetDialog()
                    QRFragment()
                }
                5 -> {
                    if (BuildConfig.FLAVOR == "wema" && Prefs.contains(PREF_REPRINT_PASSWORD)) {
                        inputPasswordDialog.show()
                        return@ServiceAdapter
                    }
                    ReprintFragment()
//                    TransactionHistoryFragment.newInstance(action = HISTORY_ACTION_REPRINT)
                }
                6 -> SalesFragment.newInstance(isVend = true)
                else -> SalesFragment.newInstance(TransactionType.CASH_ADVANCE)
            }
            nextFrag?.let { fragment ->
                addFragmentWithoutRemove(fragment)
            }
        }
        val listOfService =
            arrayListOf(
                Service(0, "New", R.drawable.ic_purchase),
//                Service(1, "Cash", R.drawable.ic_baseline_money_24),
//                Service(2, "PRE AUTHORIZATION", R.drawable.ic_pre_auth),
//                Service(3, "Cash Advance", R.drawable.ic_pay_cash_icon),
//                Service(4, "QR", R.drawable.ic_qr_code),
                Service(5, "Reprint", R.drawable.ic_print),
//                Service(6, "VEND", R.drawable.ic_vend)
            )

        adapter.submitList(listOfService)
    }

    private fun repushTransactionsToBackend() {
        val workRequest = OneTimeWorkRequestBuilder<RepushFailedTransactionToBackendWorker>()
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(
                        NetworkType.CONNECTED,
                    ).build(),
            ).build()
        workManager.enqueue(workRequest)
    }
}
