package com.woleapp.netpos.ui.fragments.pbt

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.SearchView
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.RecyclerView
import com.danbamitale.epmslib.entities.TransactionResponse
import com.woleapp.netpos.R
import com.woleapp.netpos.adapter.EODAdapter
import com.woleapp.netpos.adapter.TransactionClickListener
import com.woleapp.netpos.databinding.FragmentViewAllPBTTransactionsBinding
import com.woleapp.netpos.model.mapToZenithPbtTransactionModel
import com.woleapp.netpos.model.mapZenithPayByTransferToNormalTransaction
import com.woleapp.netpos.ui.fragments.BaseFragment
import com.woleapp.netpos.viewmodels.PayByZenithViewModel
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ViewAllPBTTransactions @Inject constructor() : BaseFragment() {
    private lateinit var binding: FragmentViewAllPBTTransactionsBinding
    private lateinit var searchTransactionSV: SearchView
    private lateinit var calendarBtn: ImageView
    private lateinit var transactionsRV: RecyclerView
    private val viewModel by activityViewModels<PayByZenithViewModel>()
    private lateinit var rvAdapter: EODAdapter
    private lateinit var adapterListener: TransactionClickListener

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
        rvAdapter.submitList(
            viewModel.getTransactions().map {
                it.mapZenithPayByTransferToNormalTransaction().copy(amount = it.amount * 100L)
            }
        )
        initViews()
        transactionsRV.addItemDecoration(
            DividerItemDecoration(
                requireContext(),
                DividerItemDecoration.VERTICAL
            )
        )
        transactionsRV.adapter = rvAdapter
    }

    private fun initViews() {
        with(binding) {
            searchTransactionSV = searchView
            calendarBtn = getEodIv
            transactionsRV = recyclerView
        }
    }
}
