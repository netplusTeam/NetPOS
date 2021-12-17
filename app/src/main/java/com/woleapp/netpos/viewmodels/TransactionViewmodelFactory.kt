package com.woleapp.netpos.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.woleapp.netpos.database.AppDatabase

class TransactionViewmodelFactory(val appDatabase: AppDatabase) : ViewModelProvider.Factory {

    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        return TransactionsViewModel(appDatabase) as T
    }
}