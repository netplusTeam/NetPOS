package com.woleapp.netpos.worker

import android.content.Context
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.woleapp.netpos.database.AppDatabase
import com.woleapp.netpos.model.DataToLogAfterConnectingToNibss
import com.woleapp.netpos.model.TransactionResponseXForTracking
import com.woleapp.netpos.network.StormApiClient
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import timber.log.Timber

class RepushFailedTransactionToBackendWorker(
    context: Context,
    workParams: WorkerParameters
) : Worker(context, workParams) {
    private val stormApiService = StormApiClient.getStormApiLoginInstance()
    private val transactionTrackingTableDao =
        AppDatabase.getDatabaseInstance(context).transactionTrackingTableDao()

    override fun doWork(): Result {
        val yetToBeUpdatedTransactions =
            transactionTrackingTableDao.getAllYetToBeUpdatedTransactions()
        var counter = yetToBeUpdatedTransactions.size
        yetToBeUpdatedTransactions.forEach {
            repushTransactionTransaction(it) {
                --counter
            }
        }

        return if (transactionTrackingTableDao.getAllYetToBeUpdatedTransactions()
            .isEmpty() && counter == 0
        ) {
            Result.success()
        } else Result.retry()
    }

    private fun repushTransactionTransaction(
        transactionToRepush: TransactionResponseXForTracking,
        decrementCounter: () -> Unit
    ) {
        val transactionResponse = DataToLogAfterConnectingToNibss(
            transactionToRepush.status,
            transactionToRepush.transRespX,
            transactionToRepush.temporalRRN
        )
        stormApiService.updateLogAfterConnectingToNibss(
            transactionToRepush.temporalRRN,
            transactionResponse
        ).subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe { t1, t2 ->
                t1?.let {
                    if (it.code() in 200..299 || it.code() == 409 || it.message()
                        .contains("There is an error") || it.code() == 404 || it.code() == 500
                    ) {
                        transactionTrackingTableDao.deleteTransactionAfterSuccessfulUpdateAtBackend(
                            transactionToRepush
                        )
                        decrementCounter()
                    }
                }
                t2?.let {
                    Timber.d("SEND_TRANS_TO_BACKEND_ERROR%s", it.localizedMessage)
                }
            }
    }
}
