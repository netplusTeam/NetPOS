package com.woleapp.netpos.network

import com.woleapp.netpos.model.PayWithQrRequest
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ContactQrPaymentRepository @Inject constructor(
    private val qrPaymentService: QrPaymentService,
) {
    fun payWithQr(
        payWithQrRequest: PayWithQrRequest,
    ) = qrPaymentService.payWithQr(
        payWithQrRequest
    )
}
