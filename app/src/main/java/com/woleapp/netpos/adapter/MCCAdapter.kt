package com.woleapp.netpos.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.paging.PagedListAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.woleapp.netpos.databinding.ItemMccBinding
import com.woleapp.netpos.model.MerchantCategory

typealias MCCClickListener = (item: MerchantCategory) -> Unit

object MCCDiffUtil : DiffUtil.ItemCallback<MerchantCategory>() {
    override fun areItemsTheSame(
        oldItem: MerchantCategory,
        newItem: MerchantCategory
    ): Boolean = oldItem.merchantCategoryCode == newItem.merchantCategoryCode

    override fun areContentsTheSame(
        oldItem: MerchantCategory,
        newItem: MerchantCategory
    ): Boolean = oldItem.merchantCategoryCode == newItem.merchantCategoryCode

}

class MCCAdapter(private val clickListener: MCCClickListener) :

    PagedListAdapter<MerchantCategory, MCCViewHolder>(MCCDiffUtil) {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MCCViewHolder {
        return MCCViewHolder.from(viewGroup = parent)
    }

    override fun onBindViewHolder(holder: MCCViewHolder, position: Int) {
        getItem(position)?.let {
            holder.bind(it)
            holder.binding.root.setOnClickListener { _ -> clickListener.invoke(it) }
        }
    }

}

class MCCViewHolder private constructor(val binding: ItemMccBinding) :
    RecyclerView.ViewHolder(binding.root) {
    companion object {
        fun from(viewGroup: ViewGroup): MCCViewHolder {
            return MCCViewHolder(
                ItemMccBinding.inflate(
                    LayoutInflater.from(viewGroup.context),
                    viewGroup,
                    false
                )
            )
        }
    }

    fun bind(merchantCategory: MerchantCategory) {
        binding.text.text = merchantCategory.merchantCategoryDescription
    }
}