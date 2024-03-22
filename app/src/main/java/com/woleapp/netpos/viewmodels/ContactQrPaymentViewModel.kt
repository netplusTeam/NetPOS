package com.woleapp.netpos.viewmodels

import androidx.lifecycle.ViewModel
import com.pixplicity.easyprefs.library.Prefs
import com.woleapp.netpos.model.AppConstants
import com.woleapp.netpos.model.FeedbackRequest
import com.woleapp.netpos.model.PayWithQrRequest
import com.woleapp.netpos.network.ContactQrPaymentRepository
import com.woleapp.netpos.network.SubmitComplaintsRepository
import com.woleapp.netpos.util.Singletons.gson
import com.woleapp.netpos.util.resourceWrapper.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import io.reactivex.Single
import io.reactivex.disposables.CompositeDisposable
import javax.inject.Inject

@HiltViewModel
class ContactQrPaymentViewModel @Inject constructor(
    private val contactlessQrPaymentRepository: ContactQrPaymentRepository,
    private val disposable: CompositeDisposable,
    private val submitComplaintsRepository: SubmitComplaintsRepository
) : ViewModel() {

    fun paymentWithQr(payWithQrRequest: PayWithQrRequest) =
        contactlessQrPaymentRepository.payWithQr(payWithQrRequest)
            .flatMap {
                if (it.isSuccessful) {
                    savePaymentWithQrResponse(it.body().toString())
                    Single.just(Resource.success(it.body()))
                } else {
                    Single.just(Resource.error(it.errorBody()))
                }
            }


    fun feedbackFromMerchants(feedbackRequest: FeedbackRequest, terminalId: String, deviceId: String) =
        submitComplaintsRepository.feedbackFromMerchants(feedbackRequest, terminalId, deviceId)
            .flatMap {
                if (it.isSuccessful) {
                    Single.just(Resource.success(it.body()))
                } else {
                    Single.just(Resource.error(it.errorBody()))
                }
            }

    private fun savePaymentWithQrResponse(data: String) {
        Prefs.putString(AppConstants.PAYMENT_WITH_QR_STRING, gson.toJson(data))
    }

    override fun onCleared() {
        super.onCleared()
        disposable.clear()
    }
}
