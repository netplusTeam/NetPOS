package com.woleapp.netpos.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.TextView


class CurrencyAdapter(
    categoryList: ArrayList<String>,
    context: Context,
    layoutId: Int
) :
    ArrayAdapter<String>(context, layoutId, categoryList) {


    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {

        val view = LayoutInflater.from(parent.context)
            .inflate(
                android.R.layout.simple_expandable_list_item_1,
                parent,
                false
            ) as TextView

        view.text = super.getItem(position).toString()

        return view

    }

}


