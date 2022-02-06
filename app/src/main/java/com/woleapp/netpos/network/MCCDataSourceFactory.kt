package com.woleapp.netpos.network

import androidx.lifecycle.MutableLiveData
import androidx.paging.DataSource
import com.woleapp.netpos.model.MCCDto
import com.woleapp.netpos.model.MerchantCategory
import io.reactivex.disposables.CompositeDisposable

class MCCDataSourceFactory(
    private val MCCDto: MCCDto,
    private val disposable: CompositeDisposable,
    private val mccService: MCCService,
    private val zenithService: ZenithQrService,
    private val blueCodeService: BlueCodeService
) :
    DataSource.Factory<Int, MerchantCategory>() {
    val itemLiveDataSource = MutableLiveData<MCCDataSource>()

    override fun create(): DataSource<Int, MerchantCategory> {
        val xx = MCCDataSource(MCCDto, disposable, mccService, zenithService, blueCodeService)
        itemLiveDataSource.postValue(xx)
        return xx
    }
}