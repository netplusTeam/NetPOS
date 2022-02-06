package com.woleapp.netpos.network

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.paging.PageKeyedDataSource
import com.woleapp.netpos.model.*
import com.woleapp.netpos.util.Event
import com.woleapp.netpos.util.disposeWith
import com.woleapp.netpos.util.toMerchantCategoryList
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import timber.log.Timber

enum class MCCService {
    BLUECODE,
    ZENITH
}

class MCCDataSource(
    private val MCCDto: MCCDto,
    private val disposables: CompositeDisposable,
    private val mccService: MCCService,
    private val zenithService: ZenithQrService,
    private val blueCodeService: BlueCodeService
) :
    PageKeyedDataSource<Int, MerchantCategory>() {

    private val _networkResourceLiveData: MutableLiveData<Event<NetworkResource>> =
        MutableLiveData<Event<NetworkResource>>()
    private val _emptyResultLiveData = MutableLiveData<Event<Boolean>>()

    val emptyResultLiveData: LiveData<Event<Boolean>>
        get() = _emptyResultLiveData

    val networkResource: LiveData<Event<NetworkResource>>
        get() = _networkResourceLiveData

    private var loadedAll = false

    override fun loadInitial(
        params: LoadInitialParams<Int>,
        callback: LoadInitialCallback<Int, MerchantCategory>
    ) {
        Timber.e("loading initial")
        Timber.e(mccService.name)
        _networkResourceLiveData.postValue(Event(NetworkResource(LoadingState.LOADING_INITIAL)))
        val query = if (mccService == MCCService.ZENITH) {
            val page = "1.20"
            Timber.e(page)
            if (MCCDto.filter.isNullOrEmpty())
                zenithService.getMerchantCategoryList(page)
            else
                zenithService.getMerchantCategoryListWithFilter(MCCDto.filter, page)
        } else {
            Timber.e("using bluecode")
            val page = "1&20"
            Timber.e(page)
             if (MCCDto.filter.isNullOrEmpty())
                blueCodeService.getMCC(page).flatMap {
                    Single.just(it.toMerchantCategoryList())
                }
            else
                blueCodeService.getMCCWithCategory(MCCDto.filter).flatMap {
                    Single.just(it.toMerchantCategoryList())
                }
        }
        query.retry(2).subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe { t1, t2 ->
                t1?.let {
                    if (it.merchantCategoryList.isEmpty()) {
                        _emptyResultLiveData.postValue(Event(true))
                        loadedAll = true
                    }
                    Timber.e("list")
                    _networkResourceLiveData.postValue(
                        Event(
                            NetworkResource(LoadingState.LOADING_COMPLETE)
                        )
                    )
                    callback.onResult(it.merchantCategoryList, null, 2)
                }
                t2?.let {
                    _networkResourceLiveData.postValue(
                        Event(
                            NetworkResource(LoadingState.LOADING_FAILED)
                        )
                    )
                }
            }
            .disposeWith(disposables)
    }

    override fun loadBefore(
        params: LoadParams<Int>,
        callback: LoadCallback<Int, MerchantCategory>
    ) {

    }

    override fun loadAfter(
        params: LoadParams<Int>,
        callback: LoadCallback<Int, MerchantCategory>
    ) {
        if (loadedAll)
            return
        _networkResourceLiveData.postValue(Event(NetworkResource(LoadingState.LOADING_MORE)))
        Timber.e("loading more")
        Timber.e(mccService.name)
        val query = if (mccService == MCCService.ZENITH) {
            val page = "${params.key}.20"
            Timber.e(page)
            if (MCCDto.filter.isNullOrEmpty())
                zenithService.getMerchantCategoryList(page)
            else
                zenithService.getMerchantCategoryListWithFilter(MCCDto.filter, page)
        } else {
            Timber.e("using bluecode")
            val page = "${params.key}&20"
            Timber.e(page)
            if (MCCDto.filter.isNullOrEmpty())
                blueCodeService.getMCC(page).flatMap {
                    Single.just(it.toMerchantCategoryList())
                }
            else
                blueCodeService.getMCCWithCategory(MCCDto.filter).flatMap {
                    Single.just(it.toMerchantCategoryList())
                }
        }
        query.retry(2).subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe { t1, t2 ->
                t1?.let {
                    if (it.merchantCategoryList.isEmpty())
                        loadedAll = true
                    callback.onResult(it.merchantCategoryList, params.key + 1)

                    _networkResourceLiveData.postValue(
                        Event(
                            NetworkResource(LoadingState.LOADING_COMPLETE)
                        )
                    )
                }
                t2?.let {
                    _networkResourceLiveData.postValue(
                        Event(
                            NetworkResource(LoadingState.LOADING_FAILED)
                        )
                    )
                }
            }
            .disposeWith(disposables)
    }
}