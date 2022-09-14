package com.woleapp.netpos.database.dao

import androidx.room.* // ktlint-disable no-wildcard-imports
import com.woleapp.netpos.model.TransactionResponseXForTracking
import io.reactivex.Single

@Dao
interface TransactionTrackingTableDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    fun insertTransactionForTracking(transResponse: TransactionResponseXForTracking): Single<Long>

    @Delete
    fun deleteTransactionAfterSuccessfulUpdateAtBackend(updatedTrans: TransactionResponseXForTracking)

    @Query("SELECT * FROM transactionTrackingTable")
    fun getAllYetToBeUpdatedTransactions(): List<TransactionResponseXForTracking>
}
