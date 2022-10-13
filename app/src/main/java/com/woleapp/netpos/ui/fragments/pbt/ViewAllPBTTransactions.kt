package com.woleapp.netpos.ui.fragments.pbt

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.SearchView
import android.widget.Toast
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.RecyclerView
import com.danbamitale.epmslib.entities.TransactionResponse
import com.danbamitale.epmslib.entities.responseMessage
import com.danbamitale.epmslib.extensions.formatCurrencyAmount
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.woleapp.netpos.R
import com.woleapp.netpos.adapter.EODAdapter
import com.woleapp.netpos.adapter.TransactionClickListener
import com.woleapp.netpos.database.AppDatabase
import com.woleapp.netpos.databinding.FragmentViewAllPBTTransactionsBinding
import com.woleapp.netpos.databinding.LayoutPrintEndOfDayBinding
import com.woleapp.netpos.model.GetZenithPayByTransferUserTransactionsModel
import com.woleapp.netpos.model.mapToZenithPbtTransactionModel
import com.woleapp.netpos.model.mapZenithPayByTransferToNormalTransaction
import com.woleapp.netpos.ui.fragments.BaseFragment
import com.woleapp.netpos.ui.fragments.TransactionHistoryFragment
import com.woleapp.netpos.ui.fragments.dialog.LoadingDialog
import com.woleapp.netpos.util.HISTORY_ACTION_EOD
import com.woleapp.netpos.util.RandomNumUtil.convertDateToStringFromMillis
import com.woleapp.netpos.util.STRING_LOADING_DIALOG_TAG
import com.woleapp.netpos.util.disposeWith
import com.woleapp.netpos.util.printEndOfDay
import com.woleapp.netpos.util.resourceWrapper.Status
import com.woleapp.netpos.viewmodels.NetPosViewModelFactories
import com.woleapp.netpos.viewmodels.PayByZenithViewModel
import com.woleapp.netpos.viewmodels.TransactionsViewModel
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import timber.log.Timber
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ViewAllPBTTransactions @Inject constructor() : BaseFragment() {
    private lateinit var binding: FragmentViewAllPBTTransactionsBinding
    private lateinit var searchTransactionSV: SearchView
    private lateinit var calendarBtn: ImageView
    private var endOfDayProgressDialog = LoadingDialog()
    private lateinit var transactionsRV: RecyclerView
    private val viewModel by activityViewModels<PayByZenithViewModel>()
    private val transactionViewModel by activityViewModels<TransactionsViewModel> {
        NetPosViewModelFactories(AppDatabase.getDatabaseInstance(requireContext()))
    }
    private lateinit var rvAdapter: EODAdapter
    private lateinit var adapterListener: TransactionClickListener
    private var transactions: List<TransactionResponse> = emptyList()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        binding = DataBindingUtil.inflate(
            inflater,
            R.layout.fragment_view_all_p_b_t_transactions,
            container,
            false
        )
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        adapterListener = object : TransactionClickListener {
            override fun invoke(p1: TransactionResponse) {
                viewModel.setClickedTransaction(
                    p1.mapToZenithPbtTransactionModel().copy(amount = p1.amount.div(100).toInt())
                )
                addFragmentWithoutRemove(ZenithPbtTransactionDetailsFragment())
            }
        }
        rvAdapter = EODAdapter(adapterListener)
        transactions = viewModel.getTransactions().map {
            it.mapZenithPayByTransferToNormalTransaction().copy(amount = it.amount * 100L)
        }
        rvAdapter.submitList(transactions)
        initViews()
        transactionsRV.addItemDecoration(
            DividerItemDecoration(
                requireContext(),
                DividerItemDecoration.VERTICAL
            )
        )
        transactionsRV.adapter = rvAdapter

        observePbtEodTransactions()
    }

    private fun observePbtEodTransactions() {
        viewModel.eodTransactions.observe(viewLifecycleOwner) {
            when (it.status) {
                Status.SUCCESS -> {
                    endOfDayProgressDialog.dismiss()
                    showEndOfDayBottomSheetDialog(it.data!!.map { mapped -> mapped.copy(amount = mapped.amount * 100) })
                }
                Status.TIMEOUT -> {
                    endOfDayProgressDialog.dismiss()
                    showSnackBar(getString(R.string.time_out), binding.root)
                }
                Status.LOADING -> {
                    endOfDayProgressDialog.show(
                        childFragmentManager,
                        STRING_LOADING_DIALOG_TAG
                    )
                }
                Status.ERROR -> {
                    endOfDayProgressDialog.dismiss()
                    showSnackBar(getString(R.string.failed), binding.root)
                }
                Status.INITIAL_DEFAULT -> {
                    // Do nothing
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()

        searchTransactionSV.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(str: String?): Boolean {
                str?.let {
                    rvAdapter.submitList(filterTransaction(it))
                    rvAdapter.notifyDataSetChanged()
                }
                return true
            }

            override fun onQueryTextChange(str: String?): Boolean {
                str?.let {
                    rvAdapter.submitList(filterTransaction(it))
                    rvAdapter.notifyDataSetChanged()
                }
                return true
            }
        })
        calendarBtn.setOnClickListener {
            showCalendarDialog()
        }
    }

    fun filterTransaction(query: String): List<TransactionResponse> = transactions.filter {
        it.transmissionDateTime.contains(query, true) || it.RRN.contains(
            query,
            true
        ) || it.STAN.contains(query, true) || it.responseMessage.contains(
            query,
            true
        )
    }

    private fun initViews() {
        with(binding) {
            searchTransactionSV = searchView
            calendarBtn = getEodIv
            transactionsRV = recyclerView
        }
    }

    private fun showCalendarDialog() {
        val calendar = Calendar.getInstance()
        DatePickerDialog(
            requireContext(),
            { _, i, i2, i3 ->
                val selectedDateInMillis =
                    Calendar.getInstance().apply { set(i, i2, i3) }.timeInMillis
                val selectedDate = convertDateToStringFromMillis(selectedDateInMillis)
                Timber.d("DATE_SELECTED=======>%s", selectedDate)
                getEndOfDayTransactions(selectedDate)
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ).show()
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
                                getString(R.string.noTransactionsToPrint),
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

    override fun onPause() {
        super.onPause()
        viewModel.resetEodToDefault()
    }

    private fun getEndOfDayTransactions(selectedDate: String) {
        viewModel.getEoD(selectedDate)
    }
}
