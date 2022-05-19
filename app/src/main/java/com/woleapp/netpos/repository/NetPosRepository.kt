package com.woleapp.netpos.repository

import com.woleapp.netpos.database.dao.TransactionResponseDao
import javax.inject.Inject

class NetPosRepository @Inject constructor(private val transactionDao: TransactionResponseDao) {

}
