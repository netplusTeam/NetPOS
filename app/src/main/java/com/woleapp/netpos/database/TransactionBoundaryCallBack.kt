package com.woleapp.netpos.database

import androidx.lifecycle.MutableLiveData
import androidx.paging.PagedList
import com.netpluspay.nibssclient.models.TransactionResponse
import com.pixplicity.easyprefs.library.Prefs
import com.woleapp.netpos.database.dao.TransactionResponseDao
import com.woleapp.netpos.network.GatewayService
import com.woleapp.netpos.nibss.NetPosTerminalConfig
import com.woleapp.netpos.util.*
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import timber.log.Timber
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.HashMap

class TransactionBoundaryCallBack(
    private val queryParams: HashMap<String, String>,
    private val gatewayService: GatewayService,
    private val transactionResponseDao: TransactionResponseDao

) : PagedList.BoundaryCallback<TransactionResponse>() {

    private val disposables = CompositeDisposable()
    private var isLoadInProgress = false
    private var dataLoadedFinished = false
    private var loadingState = MutableLiveData<Event<LoadingState>>()

    override fun onZeroItemsLoaded() {
         queryParams.apply {
            put("count", "20")
            put("page", "1")
        }
        loadingState.value = Event(LoadingInitial)
        getTransaction(queryParams)
    }

    override fun onItemAtFrontLoaded(itemAtFront: TransactionResponse) {
        loadingState.value = Event(LoadingMore)
        super.onItemAtFrontLoaded(itemAtFront)
    }

    override fun onItemAtEndLoaded(itemAtEnd: TransactionResponse) {
        queryParams.apply {
            put("count", "20")
            put("page", Prefs.getInt(TRANSACTION_LAST_LOADED_PAGE, 0).plus(1).toString())
        }
        getTransaction(queryParams)
    }

    private fun getTransaction(queryParams: Map<String, String>) {
        if (dataLoadedFinished || isLoadInProgress)
            return
        isLoadInProgress = true
        gatewayService.getTransactions(queryParams)
            .retry(3)
            .flatMap {
                if (it.result.isEmpty())
                    dataLoadedFinished = true
                Timber.e(it.result.size.toString())
                it.result = it.result.map { transaction ->
                    transaction.amount = transaction.amount.times(100)
                    val dateFormat = SimpleDateFormat("dd-MM-yyyy hh:mm:ss", Locale.getDefault())
                    val parsedDate: Date = dateFormat.parse(
                        transaction.transactionTime.replace("T", " ").replace("Z", "")
                    ) ?: Date()
                    Timber.e(parsedDate.time.toString())
                    transaction.transactionTimeInMillis = parsedDate.time
                    transaction
                }
                Timber.e(it.toString())
                transactionResponseDao.insertNewTransaction(it.result)
                Single.just(queryParams["page"])
            }
            .doFinally {
                isLoadInProgress = false
            }
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe { t1, t2 ->
                t1?.let {
                    loadingState.value = Event(LoadingDone)
                    Prefs.putInt(TRANSACTION_LAST_LOADED_PAGE, it.toInt())
                    Timber.e(it)
                }
                t2?.let {
                    loadingState.value =
                        Event(LoadingError("an error occurred while loading transactions", it))
                    Timber.e(it)
                }
            }.disposeWith(disposables)
    }

    fun clearDisposable() {
        disposables.clear()
    }
}