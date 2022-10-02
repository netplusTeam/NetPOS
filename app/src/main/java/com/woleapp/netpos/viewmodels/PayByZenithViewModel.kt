package com.woleapp.netpos.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.gson.Gson
import com.pixplicity.easyprefs.library.Prefs
import com.woleapp.netpos.model.GetZenithPayByTransferUserTransactionsModel
import com.woleapp.netpos.network.ZenithPayByTransferRepository
import com.woleapp.netpos.network.ZenithPayByTransferRepositoryLocal
import com.woleapp.netpos.util.PREF_ZENITH_PBT_USER_ACCOUNT
import com.woleapp.netpos.util.Singletons
import dagger.hilt.android.lifecycle.HiltViewModel
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class PayByZenithViewModel @Inject constructor(
    private val zenithPbtRepository: ZenithPayByTransferRepository,
    private val zenithPbtRepositoryLocal: ZenithPayByTransferRepositoryLocal
) : ViewModel() {
    private val compositeDisposable = CompositeDisposable()
    private val _clickedTransaction: MutableLiveData<GetZenithPayByTransferUserTransactionsModel> =
        MutableLiveData()
    val clickedTransaction: LiveData<GetZenithPayByTransferUserTransactionsModel> get() = _clickedTransaction
    private val terminalId = Singletons.getCurrentlyLoggedInUser()?.terminal_id ?: ""
    private val _lastTransaction: MutableLiveData<GetZenithPayByTransferUserTransactionsModel> =
        MutableLiveData()
    val lastTransaction: LiveData<GetZenithPayByTransferUserTransactionsModel> get() = _lastTransaction

    private val _allTransactions: MutableLiveData<List<GetZenithPayByTransferUserTransactionsModel>> =
        MutableLiveData()
    val allTransactions: LiveData<List<GetZenithPayByTransferUserTransactionsModel>> get() = _allTransactions

    @Inject
    lateinit var gson: Gson
    private val _zenithPbtTransactions: MutableLiveData<List<GetZenithPayByTransferUserTransactionsModel>> =
        MutableLiveData()
    val zenithPbtTransactions: LiveData<List<GetZenithPayByTransferUserTransactionsModel>> get() = _zenithPbtTransactions

    fun getZenithPbtUserAccount() {
        compositeDisposable.add(
            zenithPbtRepository.getUserVirtualAccount()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe { data, error ->
                    data?.let {
                        Prefs.putString(
                            PREF_ZENITH_PBT_USER_ACCOUNT,
                            gson.toJson(it.user)
                        )
                    }
                    error?.let {
                        Timber.d(it.localizedMessage)
                    }
                }
        )
    }

    fun getZenithPbtTransactions(date: String, requestParam: String = "2033ALWF") {
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
                }
        )
    }

    fun getLastTransaction() {
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
            }
    }

    fun saveTestTransactions(testTransactions: List<GetZenithPayByTransferUserTransactionsModel>) {
        zenithPbtRepositoryLocal.saveMultipleTransactions(testTransactions)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe { t1, t2 ->
                t1?.let { Timber.d(it.toString()) }
                t2?.let { Timber.d(it.localizedMessage) }
            }
    }

    fun getAllTransaction() {
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
            }
    }

    fun setClickedTransaction(clickedTrans: GetZenithPayByTransferUserTransactionsModel) {
        _clickedTransaction.postValue(clickedTrans)
    }

    fun getTransactions(): List<GetZenithPayByTransferUserTransactionsModel> =
        allTransactions.value ?: emptyList()

    override fun onCleared() {
        super.onCleared()
        compositeDisposable.clear()
    }
}
