package com.woleapp.netpos.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.woleapp.netpos.model.GetZenithPayByTransferUserTransactionsModel
import io.reactivex.Single

@Dao
interface ZenithPayByTransferUserTransactionsDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    fun insertTransaction(zenithPbtTransaction: GetZenithPayByTransferUserTransactionsModel): Single<Long>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    fun insertMultipleTransactions(zenithPbtTransaction: List<GetZenithPayByTransferUserTransactionsModel>): Single<LongArray>

    @Query("SELECT * FROM pbtTransaction ORDER BY paid_at DESC LIMIT 1")
    fun getTheLastTransaction(): Single<GetZenithPayByTransferUserTransactionsModel>

    @Query("SELECT * FROM pbtTransaction ORDER BY paid_at DESC")
    fun getAllTransactions(): Single<List<GetZenithPayByTransferUserTransactionsModel>>

    @Query("SELECT * FROM pbtTransaction WHERE paid_at LIKE :eodDate ORDER BY paid_at DESC")
    fun getEoD(eodDate: String): Single<List<GetZenithPayByTransferUserTransactionsModel>>
}
