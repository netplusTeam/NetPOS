package com.woleapp.netpos.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.paging.PagedListAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.netpluspay.nibssclient.models.TransactionResponse
import com.netpluspay.nibssclient.util.formatCurrencyAmount
import com.woleapp.netpos.databinding.LayoutTransactionItemBinding
import com.woleapp.netpos.util.formatDate

typealias TransactionClickListener = (TransactionResponse) -> Unit

private object TransactionItemDiffUtil : DiffUtil.ItemCallback<TransactionResponse>() {
    override fun areItemsTheSame(
        oldItem: TransactionResponse,
        newItem: TransactionResponse
    ) = oldItem.id == newItem.id

    override fun areContentsTheSame(
        oldItem: TransactionResponse,
        newItem: TransactionResponse
    ) = oldItem.transactionType == newItem.transactionType
}

class TransactionsAdapter(val listener: TransactionClickListener) :
    PagedListAdapter<TransactionResponse, TransactionsViewHolder>(TransactionItemDiffUtil) {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        TransactionsViewHolder.from(parent)

    override fun onBindViewHolder(holder: TransactionsViewHolder, position: Int) {
        getItem(position)?.let { transactionResponse ->
            holder.binding.root.setOnClickListener { listener.invoke(transactionResponse) }
            holder.bind(transactionResponse)
        }
    }
}

class TransactionsViewHolder private constructor(val binding: LayoutTransactionItemBinding) :
    RecyclerView.ViewHolder(binding.root) {
    companion object {
        fun from(parent: ViewGroup) = TransactionsViewHolder(
            LayoutTransactionItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        )
    }

    fun bind(transactionResponse: TransactionResponse) {
        binding.executePendingBindings()
        val cardDetails =
            "${if (transactionResponse.cardLabel.isEmpty()) "card" else transactionResponse.cardLabel} ending with ${
                transactionResponse.maskedPan.takeLast(
                    4
                )
            }"
        binding.cardDetails.text = cardDetails
        binding.transactionStatus.text =
            if (transactionResponse.responseCode == "00") "Approved" else "Declined"
        binding.holderName.text = transactionResponse.cardHolder.ifEmpty { "CUSTOMER" }
        binding.transactionRef.text = transactionResponse.RRN
        binding.transactionAmount.text =
            transactionResponse.amount.div(100).formatCurrencyAmount("\u20A6")
        binding.transactionDate.text = transactionResponse.transactionTimeInMillis.formatDate()
    }
}