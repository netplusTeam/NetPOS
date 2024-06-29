package com.woleapp.netpos.model.pay

import android.os.Parcelable
import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.android.parcel.Parcelize

@Entity(tableName = "qrTransactionResponseModel")
@Parcelize
data class QrTransactionResponseModel(
    val amount: String,
    val code: String,
    val currency_code: String,
    val customerName: String,
    val email: String,
    val message: String,
    val narration: String,
    @PrimaryKey(autoGenerate = false)
    val orderId: String,
    val result: String,
    val status: String,
    val transId: String
) : Parcelable

