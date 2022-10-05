package com.woleapp.netpos.ui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.activityViewModels
import androidx.paging.PagedListAdapter
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.danbamitale.epmslib.entities.TransactionResponse
import com.pixplicity.easyprefs.library.Prefs
import com.woleapp.netpos.R
import com.woleapp.netpos.adapter.EODAdapter
import com.woleapp.netpos.adapter.TransactionClickListener
import com.woleapp.netpos.adapter.TransactionsAdapter
import com.woleapp.netpos.adapter.TransactionsViewHolder
import com.woleapp.netpos.database.AppDatabase
import com.woleapp.netpos.databinding.FragmentTransactionHistoryBinding
import com.woleapp.netpos.util.* // ktlint-disable no-wildcard-imports
import com.woleapp.netpos.viewmodels.NetPosViewModelFactories
import com.woleapp.netpos.viewmodels.TransactionsViewModel

class TransactionHistoryFragment : BaseFragment() {
    private lateinit var globalAction: String

    companion object {
        fun newInstance(action: String = HISTORY_ACTION_DEFAULT) =
            TransactionHistoryFragment().apply {
                arguments = Bundle().apply {
                    globalAction = action
                    putString(HISTORY_ACTION, action)
                }
            }
    }

    private lateinit var binding: FragmentTransactionHistoryBinding
    private val viewModel by activityViewModels<TransactionsViewModel> {
        NetPosViewModelFactories(AppDatabase.getDatabaseInstance(requireContext()))
    }
    private lateinit var adapter: RecyclerView.Adapter<TransactionsViewHolder>
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentTransactionHistoryBinding.inflate(inflater, container, false)
        return binding.root
    }

    @Suppress("UNCHECKED_CAST")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val action = requireArguments().getString(HISTORY_ACTION, HISTORY_ACTION_DEFAULT)
        if (action != HISTORY_ACTION_DEFAULT) {
            binding.historyHeader.text = getString(R.string.history_header_template, action)
        }
        viewModel.setAction(action)
        val adapterListener = object : TransactionClickListener {
            override fun invoke(p1: TransactionResponse) {
                viewModel.setSelectedTransaction(p1)
                viewModel.setAction(HISTORY_ACTION_REPRINT)
                globalAction = HISTORY_ACTION_REPRINT
                addFragmentWithoutRemove(TransactionDetailsFragment())
            }
        }
        if (action == HISTORY_ACTION_PREAUTH) {
            val header = "Select PREAUTH Transaction"
            binding.historyHeader.text = header
            binding.historyButton.visibility = View.GONE
            binding.searchButton.visibility = View.GONE
        }
        if (action == HISTORY_ACTION_EOD) {
            val header = "End Of Day"
            binding.historyHeader.text = header
            binding.historyButton.visibility = View.GONE
            binding.searchButton.visibility = View.GONE
            adapter = EODAdapter(adapterListener)
        } else {
            adapter = TransactionsAdapter(adapterListener)
        }
        val tabListener = View.OnClickListener {
            val selected = when (it) {
                binding.historyButton -> 0
                else -> 1
            }
            setSelectedTab(selected)
        }
        binding.historyButton.setOnClickListener(tabListener)
        binding.searchButton.setOnClickListener(tabListener)
        binding.rvTransactionsHistory.adapter = adapter
        binding.rvTransactionsHistory.addItemDecoration(
            DividerItemDecoration(
                requireContext(),
                DividerItemDecoration.VERTICAL
            )
        )

        if (action != HISTORY_ACTION_EOD) {
            viewModel.pagedTransaction.observe(viewLifecycleOwner) {
                (adapter as PagedListAdapter<TransactionResponse, *>).submitList(it)
            }
        } else {
            val eodList = viewModel.getEodList()
            (adapter as ListAdapter<TransactionResponse, TransactionsViewHolder>).submitList(eodList)
        }
        setSelectedTab()
    }

    override fun onDestroy() {
        super.onDestroy()
        if (globalAction == HISTORY_ACTION_REPRINT) {
            val currentPage = Prefs.getInt(TRANSACTION_BY_TID_LAST_LOADED_PAGE, 1)
            Prefs.putInt(TRANSACTION_BY_TID_LAST_LOADED_PAGE, (currentPage - 1))
        }
    }

    private fun setSelectedTab(selectedTab: Int = 0) {
        if (selectedTab == 0) {
            binding.historyButton.setBackgroundColor(
                ContextCompat.getColor(
                    requireContext(),
                    R.color.colorPrimary
                )
            )
            binding.searchButton.setBackgroundColor(
                ContextCompat.getColor(
                    requireContext(),
                    android.R.color.darker_gray
                )
            )
            binding.historyButton.isEnabled = false
            binding.searchButton.isEnabled = true
            binding.searchLayout.visibility = View.GONE
            binding.rvTransactionsHistory.visibility = View.VISIBLE
        } else {
            binding.searchButton.isEnabled = false
            binding.historyButton.isEnabled = true
            binding.historyButton.setBackgroundColor(
                ContextCompat.getColor(
                    requireContext(),
                    android.R.color.darker_gray
                )
            )
            binding.searchButton.setBackgroundColor(
                ContextCompat.getColor(
                    requireContext(),
                    R.color.colorPrimary
                )
            )
            binding.searchLayout.visibility = View.VISIBLE
            binding.rvTransactionsHistory.visibility = View.GONE
        }
    }
}
