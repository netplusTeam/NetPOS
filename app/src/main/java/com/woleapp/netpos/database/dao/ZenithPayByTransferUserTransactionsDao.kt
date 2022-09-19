package com.woleapp.netpos.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import com.woleapp.netpos.model.GetZenithPayByTransferUserTransactionsModel
import io.reactivex.Single

@Dao
interface ZenithPayByTransferUserTransactionsDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    fun insertTransaction(zenthPbtTransaction: GetZenithPayByTransferUserTransactionsModel): Single<Long>
}
