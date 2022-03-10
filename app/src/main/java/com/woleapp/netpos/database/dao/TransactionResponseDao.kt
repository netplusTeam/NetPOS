package com.woleapp.netpos.database.dao

import androidx.lifecycle.LiveData
import androidx.paging.DataSource
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy.IGNORE
import androidx.room.Query
import androidx.room.Update
import com.netpluspay.nibssclient.models.TransactionResponse
import com.netpluspay.nibssclient.models.TransactionType
import io.reactivex.Completable
import io.reactivex.Single

@Dao
interface TransactionResponseDao {
    @Insert(onConflict = IGNORE)
    fun insertNewTransaction(transactionResponse: TransactionResponse): Single<Long>

    @Insert(onConflict = IGNORE)
    fun insertNewTransaction(transactionResponses: List<TransactionResponse>)

    @Update
    fun updateTransaction(transactionResponse: TransactionResponse): Single<Int>

    @Query("SELECT * FROM transactionresponse WHERE terminalId=:terminalId ORDER BY transactionTimeInMillis DESC")
    fun getTransactions(terminalId: String): DataSource.Factory<Int, TransactionResponse>

    @Query("SELECT * FROM transactionresponse WHERE transactionTimeInMillis >= :beginningOfDay and transactionTimeInMillis <= :endOfDay and terminalId=:terminalId")
    fun getEndOfDayTransaction(
        beginningOfDay: Long,
        endOfDay: Long,
        terminalId: String
    ): LiveData<List<TransactionResponse>>

    @Query("SELECT * FROM transactionresponse WHERE transactionTimeInMillis >= :beginningOfDay and transactionTimeInMillis <= :endOfDay and terminalId=:terminalId")
    fun getEndOfDayTransactionSingle(
        beginningOfDay: Long,
        endOfDay: Long,
        terminalId: String
    ): Single<List<TransactionResponse>>

    @Query("SELECT * FROM transactionresponse WHERE transactionType=:transactionType ORDER BY id DESC")
    fun getTransactionByTransactionType(transactionType: TransactionType): LiveData<List<TransactionResponse>>

    @Query("SELECT * FROM transactionresponse WHERE transactionType='PURCHASE' AND responseCode='00'")
    fun getRefundableTransactions(): LiveData<List<TransactionResponse>>

    @Query("DELETE FROM transactionresponse")
    fun nukeAllTransactions(): Completable

}