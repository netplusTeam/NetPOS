package com.woleapp.netpos.ui.fragments

import android.app.DatePickerDialog
import android.app.ProgressDialog
import android.content.DialogInterface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.GridLayoutManager
import com.danbamitale.epmslib.extensions.formatCurrencyAmount
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.gson.Gson
import com.pixplicity.easyprefs.library.Prefs
import com.woleapp.netpos.R
import com.woleapp.netpos.adapter.ServiceAdapter
import com.woleapp.netpos.database.AppDatabase
import com.woleapp.netpos.databinding.FragmentZenithPayByTransferBinding
import com.woleapp.netpos.databinding.LayoutPrintEndOfDayBinding
import com.woleapp.netpos.model.GetPayByTransferUserAccountModel
import com.woleapp.netpos.model.GetZenithPayByTransferUserTransactionsModel
import com.woleapp.netpos.model.Service
import com.woleapp.netpos.model.mapZenithPayByTransferToNormalTransaction
import com.woleapp.netpos.util.* // ktlint-disable no-wildcard-imports
import com.woleapp.netpos.viewmodels.NetPosViewModelFactories
import com.woleapp.netpos.viewmodels.PayByZenithViewModel
import com.woleapp.netpos.viewmodels.TransactionsViewModel
import dagger.hilt.android.AndroidEntryPoint
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import timber.log.Timber
import java.sql.Timestamp
import java.text.SimpleDateFormat
import java.time.Instant
import java.util.*
import javax.inject.Inject

@AndroidEntryPoint
class ZenithPayByTransferFragment : BaseFragment() {
    private lateinit var adapter: ServiceAdapter
    private val compositeDisposable = CompositeDisposable()
    private lateinit var endOfDayProgressDialog: ProgressDialog
    private val zenithPbtViewModel by activityViewModels<PayByZenithViewModel>()
    private var userZenithPbtVirtualAccount: GetPayByTransferUserAccountModel? = null
    private var _binding: FragmentZenithPayByTransferBinding? = null
    private val binding: FragmentZenithPayByTransferBinding get() = _binding!!
    private val transactionViewModel by activityViewModels<TransactionsViewModel> {
        NetPosViewModelFactories(AppDatabase.getDatabaseInstance(requireContext()))
    }

    @Inject
    lateinit var gson: Gson

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        _binding = FragmentZenithPayByTransferBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setUpAdapter()
        // Fetch user account
        val savedUserVirtualAccount = Prefs.getString(PREF_ZENITH_PBT_USER_ACCOUNT, "")
        userZenithPbtVirtualAccount =
            gson.fromJson(savedUserVirtualAccount, GetPayByTransferUserAccountModel::class.java)
        if (savedUserVirtualAccount.trim().isEmpty()) {
            zenithPbtViewModel.getZenithPbtUserAccount()
            val virtualAccount = Prefs.getString(PREF_ZENITH_PBT_USER_ACCOUNT, "")
            userZenithPbtVirtualAccount =
                gson.fromJson(virtualAccount, GetPayByTransferUserAccountModel::class.java)
        }

        binding.rvTransactions.layoutManager = GridLayoutManager(context, 2)
        binding.rvTransactions.adapter = adapter
        endOfDayProgressDialog = ProgressDialog(requireContext()).apply {
            this.setCancelable(false)
            this.setMessage("Please wait...")
            this.setButton(DialogInterface.BUTTON_POSITIVE, "Cancel") { dialog, _ ->
                compositeDisposable.clear()
                dialog.cancel()
            }
        }
    }

    private fun setUpAdapter() {
        adapter = ServiceAdapter {
            if (it.id == 0) {
                userZenithPbtVirtualAccount?.let { virtualAcc ->
                    showPayWithTransferDialog(requireContext(), virtualAcc)
                } ?: Toast.makeText(
                    requireContext(),
                    getString(R.string.unavailable_try_again),
                    Toast.LENGTH_LONG
                ).show()
            } else {
                addFragmentWithoutRemove(ZenithPayByTransferTransactionPage())
            }
//            {
//                showCalendarDialog()
//            }
        }

        val listOfService = ArrayList<Service>()
            .apply {
                add(Service(0, "Account Details", R.drawable.ic_account_details))
                add(Service(5, "View Transactions", R.drawable.ic_print))
            }
        adapter.submitList(listOfService)
    }

    private fun showEndOfDayBottomSheetDialog(transactions: List<GetZenithPayByTransferUserTransactionsModel>) {
        val approvedList = transactions.filter { it.details.isNotEmpty() }
        val declinedList = transactions.filter { it.details.isEmpty() }
        val endOfDay =
            LayoutPrintEndOfDayBinding.inflate(LayoutInflater.from(requireContext()), null, false)
        endOfDay.apply {
            approvedCount.text = approvedList.size.toString()
            declinedCount.text = declinedList.size.toString()
            totalTransactionsAmount.text =
                getString(
                    R.string.total_transaction_amount,
                    approvedList.sumOf { it.amount }.div(100).formatCurrencyAmount()
                )
            totalTransactions.text =
                getString(R.string.total_transaction_count, transactions.size.toString())
            print.setOnClickListener {
                if (transactions.isEmpty()) {
                    Toast.makeText(
                        context,
                        getString(R.string.noTransactionsToPrint),
                        Toast.LENGTH_LONG
                    ).show()
                } else {
                    when (chipGroup.checkedChipId) {
                        R.id.print_approved -> approvedList
                        R.id.print_declined -> declinedList
                        else -> transactions
                    }.apply {
                        if (isEmpty()) {
                            Toast.makeText(
                                requireContext(),
                                "No transactions to print",
                                Toast.LENGTH_SHORT
                            ).show()
                            return@setOnClickListener
                        }
                    }.map { it.mapZenithPayByTransferToNormalTransaction() }
                        .printEndOfDay(requireContext())
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe({ printResp ->
                            Timber.e(printResp.toString())
                        }, { err ->
                            Toast.makeText(
                                requireContext(),
                                err.localizedMessage,
                                Toast.LENGTH_LONG
                            )
                                .show()
                            // Timber.e(err.localizedMessage)
                        }).disposeWith(CompositeDisposable())
                }
            }
        }
        val bottomSheet = BottomSheetDialog(requireContext(), R.style.SheetDialog)
            .apply {
                dismissWithAnimation = true
                setCancelable(false)
                setContentView(endOfDay.root)
                show()
            }
        endOfDay.view.setOnClickListener {
            if (transactions.isNotEmpty()) {
                transactionViewModel.setEndOfDayList(transactions.map { it.mapZenithPayByTransferToNormalTransaction() })
                bottomSheet.dismiss()
                addFragmentWithoutRemove(TransactionHistoryFragment.newInstance(HISTORY_ACTION_EOD))
            } else {
                Toast.makeText(context, getString(R.string.noTransactionsToView), Toast.LENGTH_LONG)
                    .show()
            }
        }
        endOfDay.closeButton.setOnClickListener {
            bottomSheet.dismiss()
        }
    }

    private fun getEndOfDayTransactions(timestamp: Long? = null) {
        val terminalId = Singletons.getCurrentlyLoggedInUser()?.terminal_id
        endOfDayProgressDialog.show()
        val be: Long = getBeginningOfDay(timestamp)
        val be1: Long = Timestamp.from(Instant.ofEpochMilli(be).plusSeconds(86400)).time
        val df = SimpleDateFormat("dd:MM:yyyy hh:mm:ss", Locale.getDefault())
        val dateFormat = SimpleDateFormat("yyyy:MM:dd", Locale.getDefault())

        terminalId?.let {
            zenithPbtViewModel.getZenithPbtTransactions(dateFormat.format(be).replace(":", "-"))
        }
        zenithPbtViewModel.zenithPbtTransactions.observe(viewLifecycleOwner) {
            endOfDayProgressDialog.dismiss()
            showEndOfDayBottomSheetDialog(it)
        }
    }

    private fun showCalendarDialog() {
        val calendar = Calendar.getInstance()
        DatePickerDialog(
            requireContext(),
            { _, i, i2, i3 ->
                getEndOfDayTransactions(
                    Calendar.getInstance().apply { set(i, i2, i3) }.timeInMillis
                )
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    override fun onDestroy() {
        super.onDestroy()
        _binding = null
    }
}
