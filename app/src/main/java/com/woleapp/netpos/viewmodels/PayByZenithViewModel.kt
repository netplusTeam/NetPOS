package com.woleapp.netpos.viewmodels

import android.content.Context
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.danbamitale.epmslib.entities.CardData
import com.google.gson.Gson
import com.pixplicity.easyprefs.library.Prefs
import com.woleapp.netpos.model.GetZenithPayByTransferUserTransactionsModel
import com.woleapp.netpos.model.MerchantDetailsResponse
import com.woleapp.netpos.model.checkout.CheckOutModel
import com.woleapp.netpos.model.checkout.CheckOutResponse
import com.woleapp.netpos.model.pay.PayResponse
import com.woleapp.netpos.model.pay.PayResponseErrorModel
import com.woleapp.netpos.network.PayByTransferRepository
import com.woleapp.netpos.network.ZenithPayByTransferRepository
import com.woleapp.netpos.network.ZenithPayByTransferRepositoryLocal
import com.woleapp.netpos.util.*
import com.woleapp.netpos.util.RandomNumUtil.createClientDataForNonVerveCard
import com.woleapp.netpos.util.RandomNumUtil.stringToBase64
import com.woleapp.netpos.util.RxUtils.getSingleTransformer
import com.woleapp.netpos.util.UtilityParams.PAY_BY_TRANSFER_BEARER_TOKEN
import com.woleapp.netpos.util.resourceWrapper.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import retrofit2.HttpException
import timber.log.Timber
import java.net.SocketTimeoutException
import javax.inject.Inject

@HiltViewModel
class PayByZenithViewModel
    @Inject
    constructor(
        private val zenithPbtRepository: ZenithPayByTransferRepository,
        private val zenithPbtRepositoryLocal: ZenithPayByTransferRepositoryLocal,
        private val zenithPayByTransferRepository: PayByTransferRepository,
        private val disposable: CompositeDisposable,
    ) : ViewModel() {
        private val compositeDisposable = CompositeDisposable()
        private val _clickedTransaction: MutableLiveData<GetZenithPayByTransferUserTransactionsModel> =
            MutableLiveData()
        val clickedTransaction: LiveData<GetZenithPayByTransferUserTransactionsModel> get() = _clickedTransaction
        private val terminalId = Singletons.getCurrentlyLoggedInUser()?.terminal_id
        private val _lastTransaction: MutableLiveData<GetZenithPayByTransferUserTransactionsModel> =
            MutableLiveData()
        val lastTransaction: LiveData<GetZenithPayByTransferUserTransactionsModel> get() = _lastTransaction

        private val _allTransactions: MutableLiveData<List<GetZenithPayByTransferUserTransactionsModel>> =
            MutableLiveData()
        val allTransactions: LiveData<List<GetZenithPayByTransferUserTransactionsModel>> get() = _allTransactions

        private val _eodTransactions: MutableLiveData<Resource<List<GetZenithPayByTransferUserTransactionsModel>>> =
            MutableLiveData(Resource.initialDefault())
        val eodTransactions: LiveData<Resource<List<GetZenithPayByTransferUserTransactionsModel>>> get() = _eodTransactions

        private val _payByTransfer: MutableLiveData<Resource<MerchantDetailsResponse>> =
            MutableLiveData()
        val payByTransfer: LiveData<Resource<MerchantDetailsResponse>> get() = _payByTransfer

        val _payResponse: MutableLiveData<Resource<PayResponse>?> = MutableLiveData()
        val payResponse: LiveData<Resource<PayResponse>?> get() = _payResponse

        private val _payMessage = MutableLiveData<Event<String>>()
        val payMessage: LiveData<Event<String>>
            get() = _payMessage

        var cardData: CardData? = null
        var cvv: String? = null

        @Inject
        lateinit var gson: Gson
        private val _zenithPbtTransactions: MutableLiveData<List<GetZenithPayByTransferUserTransactionsModel>> =
            MutableLiveData()
        val zenithPbtTransactions: LiveData<List<GetZenithPayByTransferUserTransactionsModel>> get() = _zenithPbtTransactions

        fun getZenithPbtUserAccount() {
            terminalId?.let { tid ->
                compositeDisposable.add(
                    zenithPbtRepository.getUserVirtualAccount(tid)
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe { data, error ->
                            data?.let {
                                Prefs.putString(
                                    PREF_ZENITH_PBT_USER_ACCOUNT,
                                    gson.toJson(it.user),
                                )
                            }
                            error?.let {
                                Timber.d(it.localizedMessage)
                            }
                        },
                )
            }
        }

        fun getZenithPbtTransactions(
            date: String,
            requestParam: String = "2033ALWF",
        ) {
            compositeDisposable.add(
                zenithPbtRepository.getUserTransactions(date, requestParam)
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe { transactions, error ->
                        transactions?.let {
                            _zenithPbtTransactions.postValue(it.transactions)
                        }
                        error?.let {
                            Timber.d(it.localizedMessage)
                        }
                    },
            )
        }

        fun registerDeviceToken(token: String) {
            compositeDisposable.add(
                zenithPbtRepository.registerDeviceToken(token)
                    .compose(getSingleTransformer())
                    .subscribe(),
            )
        }

        fun getMerchantDetails(netPlusPayMid: String) {
            compositeDisposable.add(
                zenithPayByTransferRepository.getMerchantDetails(
                    PAY_BY_TRANSFER_BEARER_TOKEN,
                    netPlusPayMid,
                )
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .flatMap { response ->
                        Single.just(response.body())
                    }
                    .subscribe { data, error ->
                        data?.let {
                            _payByTransfer.value = Resource.success(it)
                        }
                        error?.let { throwable ->
                            Timber.d("PAY_BY_TRANSFER_ERROR_VP%s", throwable.localizedMessage)
                            _payByTransfer.value =
                                if (throwable is SocketTimeoutException) {
                                    Resource.timeOut()
                                } else {
                                    Resource.error(
                                        null,
                                    )
                                }
                        }
                    },
            )
        }

        fun getProvidusMerchantDetails(netPlusPayMid: String) {
            compositeDisposable.add(
                zenithPayByTransferRepository.getProvidusMerchantDetails(
                    PAY_BY_TRANSFER_BEARER_TOKEN,
                    netPlusPayMid,
                )
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .flatMap { response ->
                        Single.just(response.body())
                    }
                    .subscribe { data, error ->
                        data?.let {
                            _payByTransfer.value = Resource.success(it)
                        }
                        error?.let { throwable ->
                            Timber.d("PAY_BY_TRANSFER_ERROR_VP%s", throwable.localizedMessage)
                            _payByTransfer.value =
                                if (throwable is SocketTimeoutException) {
                                    Resource.timeOut()
                                } else {
                                    Resource.error(
                                        null,
                                    )
                                }
                        }
                    },
            )
        }

        fun getFcmbMerchantDetails(netPlusPayMid: String) {
            compositeDisposable.add(
                zenithPayByTransferRepository.getFcmbMerchantDetails(
                    PAY_BY_TRANSFER_BEARER_TOKEN,
                    netPlusPayMid,
                )
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .flatMap { response ->
                        Single.just(response.body())
                    }
                    .subscribe { data, error ->
                        data?.let {
                            _payByTransfer.value = Resource.success(it)
                        }
                        error?.let { throwable ->
                            Timber.d("PAY_BY_TRANSFER_ERROR_VP%s", throwable.localizedMessage)
                            _payByTransfer.value =
                                if (throwable is SocketTimeoutException) {
                                    Resource.timeOut()
                                } else {
                                    Resource.error(
                                        null,
                                    )
                                }
                        }
                    },
            )
        }

        fun getLastTransaction() {
            compositeDisposable.add(
                zenithPbtRepositoryLocal.getLastTransaction()
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe { data, error ->
                        data?.let {
                            _lastTransaction.postValue(it)
                        }
                        error?.let {
                            Timber.d(it)
                        }
                    },
            )
        }

        fun saveMultipleTransactionsToDatabase(testTransactions: List<GetZenithPayByTransferUserTransactionsModel>) {
            compositeDisposable.add(
                zenithPbtRepositoryLocal.saveMultipleTransactions(testTransactions)
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe { t1, t2 ->
                        t1?.let { Timber.d(it.toString()) }
                        t2?.let { Timber.d(it.localizedMessage) }
                    },
            )
        }

        fun getAllTransaction() {
            compositeDisposable.add(
                zenithPbtRepositoryLocal.getAllTransaction()
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe { data, error ->
                        data?.let {
                            _allTransactions.postValue(it)
                        }
                        error?.let {
                            Timber.d(it)
                        }
                    },
            )
        }

        fun getEoD(date: String) {
            _eodTransactions.postValue(Resource.loading())
            zenithPbtRepositoryLocal.getEoD(date)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe { t1, t2 ->
                    t1?.let {
                        _eodTransactions.postValue(Resource.success(it))
                    }
                    t2?.let {
                        _eodTransactions.postValue(Resource.error(null))
                    }
                }.disposeWith(compositeDisposable)
        }

        fun payQrCharges(
            context: Context,
            checkOutModel: CheckOutModel,
        ) {
//        _payResponse.postValue(Resource.loading(null))
            disposable.add(
                zenithPayByTransferRepository.checkOut(checkOutModel).flatMap {
                    saveTransIDAndAmountResponse(
                        context,
                        CheckOutResponse(
                            it.amount,
                            it.customerId,
                            it.domain,
                            it.merchantId,
                            it.status,
                            it.transId,
                        ),
                    )

                    val clientDataString =
                        createClientDataForNonVerveCard(
                            it.transId,
                            cardData!!.pan,
                            convertExpiryDate(cardData!!.expiryDate),
                            pickFirstThreeDigits(cvv!!),
                        )
                    val clientData = stringToBase64(clientDataString)
                    val newClientData = clientData.replace("\n", "")
                    zenithPayByTransferRepository.pay(newClientData)
                }.subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread())
                    .subscribe { data, error ->
                        data?.let {
                            val masterVisaResponse = gson.fromJson(it, PayResponse::class.java)
                            if (it.has("TermUrl")) {
                                _payResponse.postValue(Resource.success(masterVisaResponse))
                            }
                            val failedResponse = gson.fromJson(it, PayResponseErrorModel::class.java)
                            if (failedResponse.code == "90") {
                                Log.d("FAILED_RESP", failedResponse.code)
                                _payMessage.value =
                                    Event(
                                        Resource.error(failedResponse).data!!.result,
                                    )
                                _payResponse.postValue(Resource.error(null))
                            }
                        }
                        error?.let {
                            _payResponse.postValue(Resource.error(null))
                            (it as? HttpException).let { httpException ->
                                val errorMessage =
                                    httpException?.response()?.errorBody()?.string()
                                        ?: "{\"message\":\"Unexpected error\"}"
                                _payMessage.value =
                                    Event(
                                        try {
                                            Gson().fromJson(
                                                errorMessage,
                                                PayResponseErrorModel::class.java,
                                            ).result
                                        } catch (e: Exception) {
                                            "Gateway Time-out"
                                        },
                                    )
                            }
                        }
                    },
            )
        }

        fun convertExpiryDate(yearAndMonth: String): String {
            // Extract year and month
            val year = yearAndMonth.substring(0, 2)
            val month = yearAndMonth.substring(2, 4)

            // Create month and year format "MM/YYYY"
            val monthAndYear = "$month/$year"
            return monthAndYear
        }

        fun pickFirstThreeDigits(pin: String): String {
            val pinStr = pin.toString()
            return if (pinStr.length > 3) pinStr.substring(0, 3) else pinStr
            println("FirstThree: ${if (pinStr.length > 3) pinStr.substring(0, 3) else pinStr}") // Output: Month and Year: 12/24
        }

        fun resetEodToDefault() {
            _eodTransactions.postValue(Resource.initialDefault())
        }

        fun setClickedTransaction(clickedTrans: GetZenithPayByTransferUserTransactionsModel) {
            _clickedTransaction.postValue(clickedTrans)
        }

        fun getTransactions(): List<GetZenithPayByTransferUserTransactionsModel> = allTransactions.value ?: emptyList()

        private fun saveTransIDAndAmountResponse(
            context: Context,
            transIDAndAmount: CheckOutResponse,
        ) {
            EncryptedPrefsUtils.putString(context, TRANS_ID_AND_AMOUNT, gson.toJson(transIDAndAmount))
        }

        override fun onCleared() {
            super.onCleared()
            _payResponse.value = null
            compositeDisposable.clear()
        }
    }
