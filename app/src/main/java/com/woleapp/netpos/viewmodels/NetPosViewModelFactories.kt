package com.woleapp.netpos.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.woleapp.netpos.database.AppDatabase
import com.woleapp.netpos.network.*

class NetPosViewModelFactories() : ViewModelProvider.Factory {
    var masterPassQRService: MasterPassQRService = StormApiClient.getMasterPassQrServiceInstance()
    var nibssQRService: NibssQRService = StormApiClient.getNibssQRServiceInstance()
    var zenithQrService: ZenithQrService = StormApiClient.getZenithQRServiceInstance()
    var blueCodeService: BlueCodeService = StormApiClient.getBlueCodeService()
    private var appDatabase: AppDatabase? = null

    constructor(appDatabase: AppDatabase) : this() {
        this.appDatabase = appDatabase
    }

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        return when {
            modelClass.isAssignableFrom(QRViewModel::class.java) -> QRViewModel(
                masterPassQRService,
                nibssQRService,
                zenithQrService,
                blueCodeService
            ) as T
            modelClass.isAssignableFrom(TransactionsViewModel::class.java) -> TransactionsViewModel(appDatabase!!) as T
            modelClass.isAssignableFrom(BlueCodeViewModel::class.java) -> BlueCodeViewModel(masterPassQRService,
                nibssQRService,
                zenithQrService,
                blueCodeService) as T
            else -> throw IllegalArgumentException("Cannot instantiate viewModel")
        }
    }
}