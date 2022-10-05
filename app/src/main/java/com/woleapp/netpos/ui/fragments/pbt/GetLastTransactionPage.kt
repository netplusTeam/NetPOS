package com.woleapp.netpos.ui.fragments.pbt

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.woleapp.netpos.R
import com.woleapp.netpos.databinding.FragmentGetLastTransactionPageBinding
import com.woleapp.netpos.model.mapZenithPayByTransferToNormalTransaction
import com.woleapp.netpos.util.RandomNumUtil.formatHtml
import com.woleapp.netpos.util.disposeWith
import com.woleapp.netpos.util.print
import com.woleapp.netpos.viewmodels.PayByZenithViewModel
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import timber.log.Timber

class GetLastTransactionPage : Fragment() {
    private val compositeDisposable = CompositeDisposable()
    private lateinit var binding: FragmentGetLastTransactionPageBinding
    private lateinit var showTransactionTextView: TextView
    private lateinit var printBtn: Button
    private val zenithPbtViewModel by activityViewModels<PayByZenithViewModel>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = DataBindingUtil.inflate(
            inflater,
            R.layout.fragment_get_last_transaction_page,
            container,
            false
        )
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        zenithPbtViewModel.getLastTransaction()
        initViews()
    }

    override fun onResume() {
        super.onResume()
        zenithPbtViewModel.lastTransaction.observe(viewLifecycleOwner) { transaction ->
            showTransactionTextView.text = formatHtml(transaction.toString())
            printBtn.setOnClickListener {
                Timber.d("PRINT_BUTTON_CLICKED==>%s", "$transaction")
                if (transaction != null) {
                    transaction.mapZenithPayByTransferToNormalTransaction()
                        .copy(amount = transaction.amount * 100L)
                        .print(requireContext()).subscribeOn(Schedulers.io())
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
                        }).disposeWith(compositeDisposable)
                }
            }
        }
    }

    private fun initViews() {
        with(binding) {
            showTransactionTextView = transactionTv
            printBtn = printButton
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        compositeDisposable.clear()
    }
}
