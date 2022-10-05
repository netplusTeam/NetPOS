package com.woleapp.netpos.network

import com.woleapp.netpos.database.dao.ZenithPayByTransferUserTransactionsDao
import com.woleapp.netpos.model.GetZenithPayByTransferUserTransactionsModel
import io.reactivex.Single
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ZenithPayByTransferRepository @Inject constructor(
    private val zenithPayByTransferService: ZenithPayByTransferService
) {

    fun getUserVirtualAccount(terminalId: String = "2033ALWF") =
        zenithPayByTransferService.getUserAccount(terminalId)

    fun getUserTransactions(date: String, otherRequestParam: String) =
        zenithPayByTransferService.getTransactions("$otherRequestParam.$date")
}

@Singleton
class ZenithPayByTransferRepositoryLocal @Inject constructor(
    private val zenithPayByTransferLocal: ZenithPayByTransferUserTransactionsDao
) {
    fun getLastTransaction(): Single<GetZenithPayByTransferUserTransactionsModel> =
        zenithPayByTransferLocal.getTheLastTransaction()

    fun getAllTransaction(): Single<List<GetZenithPayByTransferUserTransactionsModel>> =
        zenithPayByTransferLocal.getAllTransactions()

    fun saveMultipleTransactions(testTrans: List<GetZenithPayByTransferUserTransactionsModel>): Single<LongArray> =
        zenithPayByTransferLocal.insertMultipleTransactions(testTrans)
}
