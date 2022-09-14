package com.woleapp.netpos.network

import com.woleapp.netpos.util.Singletons
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ZenithPayByTransferRepository @Inject constructor(
    private val zenithPayByTransferService: ZenithPayByTransferService
) {
    fun getUserVirtualAccount() {
        val mid = Singletons.getCurrentlyLoggedInUser()?.netplus_id ?: ""
        zenithPayByTransferService.getUserAccount(mid)
    }
}
