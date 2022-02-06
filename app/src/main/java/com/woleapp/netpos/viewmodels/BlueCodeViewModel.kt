package com.woleapp.netpos.viewmodels

import android.graphics.Bitmap
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.google.gson.JsonObject
import com.woleapp.netpos.model.CreateBlueCodeMerchant
import com.woleapp.netpos.model.MerchantCategory
import com.woleapp.netpos.network.BlueCodeService
import com.woleapp.netpos.network.MasterPassQRService
import com.woleapp.netpos.network.NibssQRService
import com.woleapp.netpos.network.ZenithQrService
import com.woleapp.netpos.util.*
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import retrofit2.HttpException
import timber.log.Timber

class BlueCodeViewModel(
    masterPassQRService: MasterPassQRService,
    nibssQRService: NibssQRService,
    zenithQRService: ZenithQrService,
    private val blueCodeService: BlueCodeService
) :
    QRViewModel(masterPassQRService, nibssQRService, zenithQRService, blueCodeService) {
    private val _registerNewBlueCodeMerchant = MutableLiveData<Event<Boolean>>()
    val blueCodePayload: MutableLiveData<CreateBlueCodeMerchant> = MutableLiveData(
        CreateBlueCodeMerchant()
    )
    val registerNewBlueCodeMerchant: LiveData<Event<Boolean>>
        get() = _registerNewBlueCodeMerchant

    val statesLoading = MutableLiveData(false)
    val fetchStatesFailed = MutableLiveData(false)
    private val _statesWithZip = MutableLiveData<Event<List<StateWithZip>>>()
    val statesWithZip: LiveData<Event<List<StateWithZip>>>
        get() = _statesWithZip
    private val _blueCodeQr: MutableLiveData<Event<Bitmap>> by lazy {
        MutableLiveData<Event<Bitmap>>()
    }
    val blueCodeQr: LiveData<Event<Bitmap>>
        get() = _blueCodeQr
    private val _registrationComplete: MutableLiveData<Event<Boolean>> by lazy {
        MutableLiveData()
    }
    val registrationComplete: LiveData<Event<Boolean>>
        get() = _registrationComplete

    fun setSelectedBank(bank: Bank) {
        Timber.e(bank.name)
        blueCodePayload.value = blueCodePayload.value!!.apply {
            this.bankCode = bank.code
        }
    }

    fun setSelectedZip(stateWithZip: StateWithZip) {
        Timber.e(stateWithZip.state)
        blueCodePayload.value = blueCodePayload.value!!.apply {
            this.zip = stateWithZip.postal
            this.city = stateWithZip.state
        }
    }

    override fun setSelectedMerchantCategory(it: MerchantCategory) {
        Timber.e(it.merchantCategoryDescription)
        blueCodePayload.value = blueCodePayload.value!!.apply {
            this.mcc = it.merchantCategoryCode
        }
    }

    fun fetchStates() {
        fetchStatesFailed.value = false
        blueCodeService.getAllPostal()
            .subscribeOn(Schedulers.io())
            .doOnSubscribe { statesLoading.value = true }
            .doFinally { statesLoading.postValue(false) }
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe { t1, t2 ->
                t1?.let {
                    fetchStatesFailed.value = false
                    val items = it.getListOfStateWithZips()
                    if (items.isEmpty()) {
                        fetchStatesFailed.value = true
                        return@let
                    }
                    _statesWithZip.value = Event(it.getListOfStateWithZips())
                }
                t2?.let {
                    Timber.e(it)
                    fetchStatesFailed.value = true
                }
            }.disposeWith(disposable)
    }

    fun getBlueCodeQr(amount: Double) {
        val payload = JsonObject().apply {
            addProperty("amount", amount)
        }
        blueCodeService.generateQr(payload)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .flatMap {
                it.qr.split(",")[1].decodeBase64ToBitmapSingle()
            }
            .subscribe { t1, t2 ->
                t1?.let {
                    _blueCodeQr.value = Event(it)
                }
                t2?.let {
                    Timber.e(it)
                    val responseBody =
                        (it as? HttpException)?.getResponseBody() ?: "Something went wrong"
                    if (responseBody.contains("merchant not registered")) {
                        Timber.e("start registration process")
                        message.value = Event("merchant not registered")
                        _registerNewBlueCodeMerchant.value = Event(true)
                    }
                    qrErrorMessageMutableLiveData.value = Event(responseBody)
                }
            }.disposeWith(disposable)
    }

    fun registerBlueCodeMerchant() {
        Timber.e(blueCodePayload.value.toString())
        if (blueCodePayload.value?.mcc?.isEmpty() != false) {
            message.value = Event("Please select a merchant category code.")
            return
        }
        blueCodeService.createBlueCodeMerchant(payload = blueCodePayload.value!!)
            .doOnSubscribe {
                registrationInProgress.postValue(true)
            }.doFinally {
                registrationInProgress.postValue(false)
            }
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe { t1, t2 ->
                t1?.let {
                    if (it.errors != null) {
                        message.value = Event("merchant registration failed")
                        return@subscribe
                    }
                    message.value = Event(it.message)
                    _registrationComplete.value = Event(true)
                }
                t2?.let {
                    Timber.e(it)
                    message.value = Event("merchant registration failed, try again")
                }
            }
            .disposeWith(disposable)
    }
}