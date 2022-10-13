package com.woleapp.netpos.worker

import android.content.Context
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.google.gson.Gson
import com.woleapp.netpos.database.AppDatabase
import com.woleapp.netpos.model.GetZenithPayByTransferUserTransactionsModel
import com.woleapp.netpos.util.WORKER_INPUT_PBT_TRANSACTION_TAG
import com.woleapp.netpos.util.disposeWith
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import timber.log.Timber

class SaveTransactionFromFirebaseMessagingServiceToDbWorker(
    context: Context,
    workerParameters: WorkerParameters
) : Worker(context, workerParameters) {
    private val compositeDisposable = CompositeDisposable()
    private val zenithPayByTransferDao =
        AppDatabase.getDatabaseInstance(context).getZenithPayByTransferDao()

    override fun doWork(): Result {
        val inputData = inputData.getString(WORKER_INPUT_PBT_TRANSACTION_TAG)
        val transactionToSave =
            Gson().fromJson(inputData, GetZenithPayByTransferUserTransactionsModel::class.java)
        val affectedRows = transactionToSave?.let { pbtTransaction ->
            var numberOfAffectedRows = 0L
            zenithPayByTransferDao.insertTransaction(pbtTransaction)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe { t1, t2 ->
                    t1?.let {
                        numberOfAffectedRows = it
                    } ?: run { numberOfAffectedRows = 0 }

                    t2?.let {
                        numberOfAffectedRows = 0
                        Timber.d(it)
                    }
                }.disposeWith(compositeDisposable)
            numberOfAffectedRows
        } ?: 0
        return if (affectedRows > 0 || affectedRows < 0) Result.success() else Result.retry()
    }

    override fun onStopped() {
        super.onStopped()
        compositeDisposable.clear()
    }
}
