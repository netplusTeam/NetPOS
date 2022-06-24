package com.woleapp.netpos.database

import androidx.lifecycle.MutableLiveData
import androidx.paging.PagedList
import com.danbamitale.epmslib.entities.TransactionResponse
import com.pixplicity.easyprefs.library.Prefs
import com.woleapp.netpos.database.dao.TransactionResponseDao
import com.woleapp.netpos.model.GetEodFromNewServiceModel
import com.woleapp.netpos.network.StormApiService
import com.woleapp.netpos.util.* // ktlint-disable no-wildcard-imports
import com.woleapp.netpos.util.ModelMapper.mapRowToTransactionResponse
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import timber.log.Timber

class TransactionBoundaryCallBack(
    private val queryParams: HashMap<String, String>,
    private val params: GetEodFromNewServiceModel,
    private val stormApiService: StormApiService,
    private val transactionResponseDao: TransactionResponseDao

) : PagedList.BoundaryCallback<TransactionResponse>() {

    private val disposables = CompositeDisposable()
    private var isLoadInProgress = false
    private var dataLoadedFinished = false
    private var loadingState = MutableLiveData<Event<LoadingState>>()
    private var pageNumberTracker = 0

    override fun onZeroItemsLoaded() {
        queryParams.apply {
            put("count", "20")
            put("page", "1")
        }
        loadingState.value = Event(LoadingInitial)
        Timber.d("WEIRD_TID ==> ${params.terminalId}")
        val localParams =
            GetEodFromNewServiceModel(params.terminalId, "", "", 1, 20)
        getTransactionByTerminalId(localParams)
    }

    override fun onItemAtFrontLoaded(itemAtFront: TransactionResponse) {
        loadingState.value = Event(LoadingMore)
        super.onItemAtFrontLoaded(itemAtFront)
    }

    override fun onItemAtEndLoaded(itemAtEnd: TransactionResponse) {
        queryParams.apply {
            put("count", "20")
            put("page", Prefs.getInt(TRANSACTION_BY_TID_LAST_LOADED_PAGE, 0).plus(1).toString())
        }
        val pageNumber = Prefs.getInt(TRANSACTION_BY_TID_LAST_LOADED_PAGE, 0).plus(1)
        val localParams = GetEodFromNewServiceModel(params.terminalId, "", "", pageNumber, 20)
        pageNumberTracker = pageNumber
        getTransactionByTerminalId(localParams)
    }

    private fun getTransaction(
        params: GetEodFromNewServiceModel
    ) {
        if (dataLoadedFinished || isLoadInProgress)
            return
        isLoadInProgress = true
        Timber.d("WEIRD_TERMINAL ==> ${params.terminalId}")
        Timber.d("WEIRD_FROM ==> ${params.from}")
        Timber.d("WEIRD_TO ==> ${params.to}")
        Timber.d("WEIRD_PAGE ==> ${params.page}")
        stormApiService.getTransactionsFromNewService(
            params.terminalId,
            params.from,
            params.to,
            params.page,
            params.pageSize
        )
            .retry(3)
            .flatMap {
                if (it.data.rows.isEmpty())
                    dataLoadedFinished = true
                it.data.rows = it.data.rows.map { transaction ->
                    transaction.amount =
                        if (transaction.amount is Int) (transaction.amount as Int).times(100) else (transaction.amount as Double)
                            .times(100)
                    transaction
                }
                transactionResponseDao.insertNewTransaction(it.data.rows.mapRowToTransactionResponse())
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
                    Prefs.putInt(TRANSACTION_BY_TID_LAST_LOADED_PAGE, it.toInt())
                    Timber.e("TCal" + it)
                }
                t2?.let {
                    loadingState.value =
                        Event(LoadingError("an error occurred while loading transactions", it))
                    Timber.e(it)
                }
            }.disposeWith(disposables)
    }

    private fun getTransactionByTerminalId(
        params: GetEodFromNewServiceModel
    ) {
        if (dataLoadedFinished || isLoadInProgress)
            return
        isLoadInProgress = true
        stormApiService.getTransactionsFromNewServiceByTerminalId(
            params.terminalId,
            params.page,
            params.pageSize
        )
            .retry(3)
            .flatMap {
                if (it.data.rows.isEmpty())
                    dataLoadedFinished = true
                it.data.rows = it.data.rows.map { transaction ->
                    transaction.amount =
                        if (transaction.amount is Int) (transaction.amount as Int).times(100) else (transaction.amount as Double)
                            .times(100)
                    transaction
                }
                transactionResponseDao.insertNewTransaction(it.data.rows.mapRowToTransactionResponse())
            }
            .doFinally {
                isLoadInProgress = false
//                Single.just(queryParams["page"])
            }
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .flatMap {
                Timber.d("DATAB" + it.size.toString())
                Single.just(queryParams["page"])
            }
            .subscribe { t1, t2 ->
                t1?.let {
                    loadingState.value = Event(LoadingDone)
                    Prefs.putInt(TRANSACTION_BY_TID_LAST_LOADED_PAGE, it.toInt())
                    Timber.e("TCal" + it)
                }
                t2?.let {
                    loadingState.value =
                        Event(LoadingError("an error occurred while loading transactions", it))
                    Timber.e(it)
                }
            }
            .disposeWith(disposables)
    }

    fun clearDisposable() {
        disposables.clear()
    }
}
