package com.woleapp.netpos.util

import io.reactivex.SingleTransformer
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import timber.log.Timber

object RxUtils {
    fun <T> getSingleTransformer(): SingleTransformer<T, T> = SingleTransformer {
        it.subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .doOnError { throwable ->
                Timber.d(throwable.localizedMessage)
            }
            .doOnSuccess { data ->
                Timber.d("$data")
            }
    }
}
