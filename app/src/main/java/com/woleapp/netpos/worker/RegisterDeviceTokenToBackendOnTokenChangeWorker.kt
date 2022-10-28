package com.woleapp.netpos.worker

import android.content.Context
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.woleapp.netpos.model.ZenithPayByTransferRegisterDeviceTokenModel
import com.woleapp.netpos.network.ZenithPayByTransferClient
import com.woleapp.netpos.util.RxUtils.getSingleTransformer
import com.woleapp.netpos.util.Singletons
import com.woleapp.netpos.util.WORKER_INPUT_FIREBASE_DEVICE_TOKEN_TAG
import com.woleapp.netpos.util.disposeWith
import io.reactivex.disposables.CompositeDisposable

class RegisterDeviceTokenToBackendOnTokenChangeWorker(
    val context: Context,
    private val parameters: WorkerParameters
) : Worker(context, parameters) {
    private val compositeDisposable: CompositeDisposable = CompositeDisposable()

    private val payByTransferRepository = ZenithPayByTransferClient.getInstance()

    override fun doWork(): Result {
        val terminalId = Singletons.getCurrentlyLoggedInUser()?.terminal_id
        val deviceToken = inputData.getString(WORKER_INPUT_FIREBASE_DEVICE_TOKEN_TAG)

        val response = terminalId?.let { tid ->
            var response = ""
            deviceToken?.let { token ->
                val req = ZenithPayByTransferRegisterDeviceTokenModel(token, tid)
                payByTransferRepository.registerDeviceToken(req)
                    .compose(getSingleTransformer())
                    .subscribe { value ->
                        response = value
                    }.disposeWith(compositeDisposable)
            }
            response
        } ?: ""

        return if (response == "Device registration token added successfully" || response.length > 5) {
            Result.success()
        } else {
            Result.retry()
        }
    }
}
