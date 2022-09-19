package com.woleapp.netpos.network

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ZenithPayByTransferRepository @Inject constructor(
    private val zenithPayByTransferService: ZenithPayByTransferService
) {

    fun getUserVirtualAccount(terminalId: String = "2033ALWF") =
        zenithPayByTransferService.getUserAccount(terminalId)

    fun getUserTransactions(date: String, otherRequestParam: String) =
        zenithPayByTransferService.getTransactions("$otherRequestParam.$date")
}
