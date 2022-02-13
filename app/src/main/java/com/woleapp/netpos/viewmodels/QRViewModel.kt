package com.woleapp.netpos.viewmodels

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Handler
import android.os.Looper
import androidx.lifecycle.*
import androidx.paging.LivePagedListBuilder
import androidx.paging.PagedList
import com.google.gson.JsonObject
import com.woleapp.netpos.model.*
import com.woleapp.netpos.network.*
import com.woleapp.netpos.util.*
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.PublishSubject
import retrofit2.HttpException
import timber.log.Timber
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

open class QRViewModel(
    private val masterPassQRService: MasterPassQRService,
    private val nibssQRService: NibssQRService,
    private val zenithQRService: ZenithQrService,
    private val blueCodeService: BlueCodeService
) : ViewModel() {
    private var retryAttempts = 1
    var stillHasRetryAttempts = true
    val disposable = CompositeDisposable()
    private val _masterPassQrBitmap = MutableLiveData<Event<Bitmap>>()
    val masterPassQrBitmap: LiveData<Event<Bitmap>>
        get() = _masterPassQrBitmap

    val message = MediatorLiveData<Event<String>>()

    val qrErrorMessageMutableLiveData = MutableLiveData<Event<String>>()
    val qrErrorMessage: LiveData<Event<String>>
        get() = qrErrorMessageMutableLiveData

    private val lastNibssOrderNumber = MutableLiveData<String>()

    private val _nibssQRBitmap = MutableLiveData<Event<Bitmap>>()
    val nibssQRBitmap: LiveData<Event<Bitmap>>
        get() = _nibssQRBitmap

    private val _reQuerying = MutableLiveData<Event<Boolean>>()

    val reQuerying: LiveData<Event<Boolean>>
        get() = _reQuerying

    private val _createZenithMerchant = MutableLiveData<Event<String?>>()

    val createZenithMerchant: LiveData<Event<String?>>
        get() = _createZenithMerchant

    private val _zenithCityList = MutableLiveData<Event<List<ZenithCity>>>()

    val zenithCityList: LiveData<Event<List<ZenithCity>>>
        get() = _zenithCityList

    val createZenithMerchantPayload = MutableLiveData(CreateZenithMerchantPayload())

    val registrationInProgress = MutableLiveData(false)

    private val _paginationHelper = MutableLiveData<PaginationHelper>()
    private val emptyListLiveData = _paginationHelper.switchMap {
        it.emptyResultLiveData!!
    }

    private var registrationGateway: String? = null

    init {
        message.addSource(emptyListLiveData) {
            message.value = Event("No results found")
        }
    }


    val loadingStateLiveData = _paginationHelper.switchMap {
        it.eventLiveData!!
    }

    private val _zenithQr = MutableLiveData<Event<Bitmap?>>()
    val zenithQr: LiveData<Event<Bitmap?>>
        get() = _zenithQr

    private val _zenithQrRegistrationDone = MutableLiveData<Event<Boolean>>()
    val zenithQrRegistrationDone: LiveData<Event<Boolean>>
        get() = _zenithQrRegistrationDone


    val zenithMccList = _paginationHelper.switchMap {
        Timber.e("size: ${it.data?.value?.size.toString()}")
        if (it.data == null) {
            Timber.e("data is null")
        }
        it.data!!
    }
    private val subject = PublishSubject.create<String>()

    private val config = PagedList.Config.Builder()
        .setInitialLoadSizeHint(20)
        .setPageSize(20)
        .setEnablePlaceholders(false)
        .build()


    fun getMasterPassQr(amount: Double) {
        Timber.e("Get masterpass")
        val qrRequestBody = JsonObject()
        val user = Singletons.getCurrentlyLoggedInUser()!!
        qrRequestBody.apply {
            addProperty("amount", amount.toString())
            addProperty("order_id", UUID.randomUUID().toString())
            addProperty("merchant_id", user.netplus_id)
            addProperty("currency_code", "NGN")
            addProperty("country_code", "NG")
            addProperty("business_name", user.business_name)
            addProperty("merchant_city", "Lagos")
        }
        masterPassQRService.getStaticQr(qrRequestBody)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .flatMap {
                val bmp = BitmapFactory.decodeStream(it.byteStream())
                if (bmp != null)
                    Single.just(bmp)
                else
                    throw NullPointerException("Bitmap is null")
            }
            .subscribe { t1, t2 ->
                t1?.let { bmp ->
                    Timber.e("gotten and amount is $amount")
                    _masterPassQrBitmap.value = Event(bmp)
                }
                t2?.let { error ->
                    val httpException = error as? HttpException
                    httpException?.let {
                        Timber.e("body ${it.response()?.errorBody()?.string()}")
                        Timber.e("message ${it.message()}")
                        Timber.e(it.message ?: "Error")
                    }
                    qrErrorMessageMutableLiveData.value = Event("Error}")
                    message.value = Event(
                        "An error occurred while fetching QR"
                    )
                }
            }.disposeWith(disposable)
    }

    fun getNibssQR(amount: Double) {
        val start: Long = 111_111_111_111
        val end: Long = 999_999_999_999
        val range1 = (start..end).random()
        val range2 = (start..end).random()
        lastNibssOrderNumber.value =
            "${
                SimpleDateFormat(
                    "yMM",
                    Locale.getDefault()
                ).format(Date(System.currentTimeMillis()))
            }$range1$range2"
        val jsonObject = JsonObject()
        jsonObject.addProperty("amount", amount.toString())
        jsonObject.addProperty("order_no", lastNibssOrderNumber.value!!)
        nibssQRService.getQr(jsonObject)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .flatMap {
                if (it.returnCode.isNullOrEmpty() || it.returnCode != "Success" || it.codeUrl.isNullOrEmpty())
                    throw Exception("Could not fetch QR code")
                Single.just(encodeAsBitmap(it.codeUrl, 150, 150))
            }
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe { t1, t2 ->
                t1?.let {
                    _nibssQRBitmap.value = Event(it)
                    runHandler()
                }
                t2?.let {
                    message.value = Event("Error")
                }
            }
            .disposeWith(disposable)
    }

    private fun runHandler() {
        if (retryAttempts > 15) {
            stillHasRetryAttempts = false
            message.value = Event("Too many attempts without response")
            return
        }
        Handler(Looper.getMainLooper()).postDelayed({
            queryTransaction()
        }, 4000)
    }

    private fun queryTransaction() {
        retryAttempts += 1
        _reQuerying.value = Event(true)
        lastNibssOrderNumber.value?.let {
            val jsonObject = JsonObject()
            jsonObject.addProperty("order_no", it)
            nibssQRService.queryTransactionStatus(jsonObject)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .doFinally { _reQuerying.value = Event(false) }
                .subscribe { t1, t2 ->
                    t1?.let { nibssQRResponse ->
                        when (nibssQRResponse.returnCode) {
                            "Paying" -> runHandler()
                            "Success" -> {
                                stillHasRetryAttempts = false
                                message.value = Event("Success, payment confirmed")
                            }
                            else -> {
                                message.value = Event("Failed, payment failed")
                                stillHasRetryAttempts = false
                            }
                        }
                    }
                    t2?.let {
                        retryAttempts -= 1
                        message.value = Event("Retrying")
                    }
                }.disposeWith(disposable)
        }
    }

    fun getZenithQR(type: String, amount: Double) {
        val jsonObject = JsonObject().apply {
            addProperty("amount", amount)
        }
        val req =
            if (amount == 0.0) zenithQRService.getZenithQr(type) else zenithQRService.getDynamicQr(
                type,
                amount.toInt().toString()
            )
        req.subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .flatMap {
                val bitmap: Bitmap? = it.qrCode.decodeBase64ToBitmap()
                if (bitmap != null)
                    Single.just(bitmap)
                else
                    throw NullPointerException("Bitmap is null")
            }
            .subscribe { t1, t2 ->
                t1?.let {
                    _zenithQr.value = Event(it)
                }
                t2?.let {
//                    val temp = "iVBORw0KGgoAAAANSUhEUgAAAfQAAAH0AQAAAADjreInAAADw0lEQVR42u2cQY7bMAxFOfAiyxzBR8nR6qPlKD6Cl1kYViXyk5IdBQFaoEChr8WM4eh5RZAUP0VJf7U2IU+ePHny5MmTJ0+e/D/mD7E1HZKWB978Kv/3H9v4En1Y7y9svZMnX3nbNOU/i8zbTfln3pT5/DDnn8oefUj2IfLkG37JJqWb8oPcitllQ9wk7xZ5FPsT54uxPsiT7/LFbW23Xa4ezQwRe8iT/2R/qWySsjut+IjuthhJnnyX9/hX7U/jX3KP5hnVt/hJfki+9VYrAmH/4Xv+Tn40vllwWz8p3JbaX3lTUvOv9QPyA/Jqf8it82lN/VcJhLrU7DR/Endk7/GP/PA8ol1SXvOntn5kodEwhEby5OXktiZPktRbudlZ2mRml+0v4Sfy5CsvXjZK2K3H/siW8EUvbT/nRJ58mz/h2K9hz+tHmm3X+pEgf9KKJHnyp/pRkT38N7itLU50JyEkAiF58jX+hf2Z2UWRSM9vU2ujy5v9kh+dR7VRz2YuhHhp8lLI1mI3efKt/wq11R1ZyciRbaeL2yJP/qp/+JG+WNsN1SIIadoIYomUfrqrn5AfmjdtQ+NfTrJ381au4ptFRiEbro08+dP5DdXGFWVrkbP+gYy8KUSSJ1/5sLYUjkxXDYTiioj06kfkx+Wr/uGntdr/URQRmSJGRo81efJVP2uPbd5/5m/g0ZL3L/b0N/Ij85DEDvdNjbWZol9l+0M6/R/kx+aLScHIklWrI9pp/u1pk0dE8uTfz/92yF+tWh1laz3273G0gxBCnnxHf0WTR9wfa75oifiOi0DkyZ/r1+g2e85+bSw6itB2Jl4I6On35MflraUs2qajEQ2Kfol/kX+jInAnT/6t/xX3fyJx8kZY3EiEa+v4L/Jj86g/etod3a6XtvtLIxp58tH/MTdJNt6kem0DrbEh7d/Jk7/Ur+2SRjQpRkcsWtOK/W3d+z/kR+dj7Eu9m7GgbHQuW7/8DXnyp/urccm55E/nQoCPfRHnO/o9+ZH5Rn9tLvl42Tr2zFVaI0/+Ur+O+Gf6x6OqHdUi5dP8O/Lj8rEOdCtCP7taZMIgoV7/Gflx+Zj/0swPirRJDRFXyxAju/M7yA/Mx5AF271LnciAq2Vxo6zN0cmTT838u2T1Ixc5vCMkxUTXZiIMefK9+ZtxEbFO5Fwh5Iv36Hf0e/LkW9m1md/xiDcxEebL/Ffyo/Ex9iXVIVNou19qRj7XQfbkyXfnb4Za5kKauNtaxAfZf8rfyQ/J//EiT548efLkyZMnT578f8T/BrkxBrThrRD3AAAAAElFTkSuQmCC"
//                    _zenithQr.value = Event(temp.decodeBase64ToBitmap())
                    Timber.e(it)
                    val responseBody = it.getResponseBody()
                    if (it.isHttpStatusCode(404) && responseBody.contains("Merchant not registered")) {
                        _createZenithMerchant.value = Event(type)
                    } else {
                        _createZenithMerchant.value = Event("")
                        if (responseBody.contains("html", true))
                            message.value = Event("Server Error")
                        else
                            message.value = Event(responseBody)
                    }
                }
            }.disposeWith(disposable)
    }


    val cityLoading = MutableLiveData(false)
    fun getCities(state: String) {
        zenithQRService.getCity(state)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .doOnSubscribe {
                cityLoading.value = true
            }.doFinally {
                cityLoading.value = false
            }
            .retry(2)
            .subscribe { t1, t2 ->
                t1?.let {
                    _zenithCityList.value = Event(it.cityList)
                }
                t2?.let {
                    Timber.e(it.localizedMessage)
                    message.value = Event("An error occurred while fetching cities in $state")
                }
            }
            .disposeWith(disposable)
    }

    fun setSelectedCity(position: Int) {
        createZenithMerchantPayload.value = createZenithMerchantPayload.value?.apply {
            zenithCityList.value?.peekContent()?.let {
                this.cityName = it[position].cityName
                this.regionName = it[position].regionName
            }
        }
        Timber.e(createZenithMerchantPayload.value.toString())
    }

    private lateinit var dataSourceFactory: MCCDataSourceFactory

    fun getMCC(MCCDto: MCCDto, mccService: MCCService) {
        dataSourceFactory =
            MCCDataSourceFactory(MCCDto, disposable, mccService, zenithQRService, blueCodeService)
        val networkResourceLiveData: LiveData<Event<NetworkResource>> = Transformations.switchMap(
            dataSourceFactory.itemLiveDataSource
        ) {
            it.networkResource
        }

        val emptyResultLiveData: LiveData<Event<Boolean>> = Transformations.switchMap(
            dataSourceFactory.itemLiveDataSource
        ) {
            it.emptyResultLiveData
        }

        val data: LiveData<PagedList<MerchantCategory>> =
            LivePagedListBuilder(dataSourceFactory, config).build()
        _paginationHelper.postValue(
            PaginationHelper(
                networkResourceLiveData,
                emptyResultLiveData,
                data
            )
        )
    }

    fun textChanged(filter: String) = subject.onNext(filter)

    fun initSearchFilter(mccService: MCCService) {
        subject
            .debounce(1, TimeUnit.SECONDS)
            .filter { it.isEmpty().not() }
            .distinctUntilChanged()
            .subscribe {
                Timber.e(it)
                getMCC(MCCDto(it), mccService)
            }.disposeWith(disposable)
    }

    override fun onCleared() {
        super.onCleared()
        disposable.dispose()
    }

    fun textChangeComplete() {
        subject.onComplete()
    }

    open fun setSelectedMerchantCategory(it: MerchantCategory) {
        createZenithMerchantPayload.value = createZenithMerchantPayload.value?.apply {
            this.merchantCategoryCode = it.merchantCategoryCode
            this.merchantCategoryDescription = it.merchantCategoryDescription
        }
    }

    fun registerZenithMerchant() {
        Timber.e(createZenithMerchantPayload.value.toString())
        val payload = createZenithMerchantPayload.value!!
        if (payload.bvn.isNullOrEmpty() || payload.bvn!!.length < 11) {
            message.value = Event("Enter a valid bank verification number")
            return
        }
        if (payload.merchantCategoryCode.isNullOrEmpty()) {
            message.value = Event("Select a merchant category")
            return
        }
        if (payload.cityName.isNullOrEmpty()) {
            message.value = Event("Select a LGA")
            return
        }
        zenithQRService.createZenithQRMerchant(payload)
            .subscribeOn(Schedulers.io()).doOnSubscribe {
                message.postValue(Event("Registering, please wait"))
                registrationInProgress.postValue(true)
            }.doFinally { registrationInProgress.postValue(false) }
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe { t1, t2 ->
                t1?.let {
                    _zenithQrRegistrationDone.value = Event(true)
                    message.value = Event(it.message)
                }
                t2?.let {
                    Timber.e(it)
                    message.value = Event("Registration failed")
                    message.value = Event(it.getResponseBody())
                }
            }.disposeWith(disposable)
    }

    fun clearSelectedCity() {
        createZenithMerchantPayload.value = createZenithMerchantPayload.value?.apply {
            this.cityName = null
            this.regionName = null
        }
    }

    fun setType(type: String) {
        registrationGateway = type
    }
}