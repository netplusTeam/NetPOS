package com.woleapp.netpos.network

import com.woleapp.netpos.database.dao.ZenithPayByTransferUserTransactionsDao
import com.woleapp.netpos.model.GetPayByTransferUserAccount
import com.woleapp.netpos.model.GetZenithPayByTransferUserTransactions
import com.woleapp.netpos.model.GetZenithPayByTransferUserTransactionsModel
import com.woleapp.netpos.model.ZenithPayByTransferRegisterDeviceTokenModel
import com.woleapp.netpos.util.Singletons
import io.reactivex.Single
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ZenithPayByTransferRepository @Inject constructor(
    private val zenithPayByTransferService: ZenithPayByTransferService
) {

    fun getUserVirtualAccount(terminalId: String = "2033ALWF"): Single<GetPayByTransferUserAccount> =
        zenithPayByTransferService.getUserAccount(terminalId)

    fun getUserTransactions(
        date: String,
        otherRequestParam: String
    ): Single<GetZenithPayByTransferUserTransactions> =
        zenithPayByTransferService.getTransactions("$otherRequestParam.$date")

    fun registerDeviceToken(token: String): Single<String> =
        zenithPayByTransferService.registerDeviceToken(
            ZenithPayByTransferRegisterDeviceTokenModel(
                token,
                Singletons.getCurrentlyLoggedInUser()!!.terminal_id!!
            )
        )

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

    fun getEoD(transDate: String): Single<List<GetZenithPayByTransferUserTransactionsModel>> =
        zenithPayByTransferLocal.getEoD(transDate + "T" + "%")
}
