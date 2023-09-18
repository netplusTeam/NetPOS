package com.woleapp.netpos.viewmodels

import android.util.Patterns
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.auth0.android.jwt.BuildConfig
import com.auth0.android.jwt.JWT
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.pixplicity.easyprefs.library.Prefs
import com.woleapp.netpos.model.AuthError
import com.woleapp.netpos.model.User
import com.woleapp.netpos.network.StormApiService
import com.woleapp.netpos.util.*
import com.woleapp.netpos.util.Singletons.gson
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import org.json.JSONObject
import retrofit2.HttpException
import timber.log.Timber
import java.util.*

class AuthViewModel : ViewModel() {
    private val disposables = CompositeDisposable()
    var stormApiService: StormApiService? = null
    var appCredentials: JsonObject? = null
    val authInProgress = MutableLiveData(false)
    val passwordResetInProgress = MutableLiveData(false)
    val usernameLiveData = MutableLiveData("")
    val passwordLiveData = MutableLiveData("")
    private val _message = MutableLiveData<Event<String>>()
    private val _authDone = MutableLiveData<Event<Boolean>>()
    private val _gotoAdminPage = MutableLiveData<Event<Boolean>>()
    private val _passwordResetSent = MutableLiveData<Event<Boolean>>()

    val passwordResetSent: LiveData<Event<Boolean>>
        get() = _passwordResetSent

    val gotoAdminPage: LiveData<Event<Boolean>>
        get() = _gotoAdminPage

    val authDone: LiveData<Event<Boolean>>
        get() = _authDone

    val message: LiveData<Event<String>>
        get() = _message

    fun login() {
        val username = usernameLiveData.value
        val password = passwordLiveData.value
        if (username.isNullOrEmpty() || password.isNullOrEmpty()) {
            _message.value = Event("All fields are required")
            return
        }
        if (!Patterns.EMAIL_ADDRESS.matcher(username).matches()) {
            _message.value = Event("Please enter a valid email")
            return
        }
        auth(username, password)
    }
    fun login(deviceId:String) {
        val username = usernameLiveData.value
        val password = passwordLiveData.value
        if (username.isNullOrEmpty() || password.isNullOrEmpty()) {
            _message.value = Event("All fields are required")
            return
        }
        if (!Patterns.EMAIL_ADDRESS.matcher(username).matches()) {
            _message.value = Event("Please enter a valid email")
            return
        }
            easyPOSAuth(username, password, deviceId)
    }

    private fun auth(username: String, password: String) {
        authInProgress.value = true
        val credentials = JsonObject()
            .apply {
                addProperty("username", username)
                addProperty("password", password)
            }
        stormApiService!!.userToken(credentials)
            .flatMap {
                Timber.d("DATA_GOTTEN_USER_DATA_TOKENRESPONSE===>$it")
                Timber.e(it.toString())
                if (BuildConfig.BUILD_TYPE.equals(
                        "releaseAdmin",
                        true,
                    ) || BuildConfig.BUILD_TYPE.equals("nibssserverdebug", true)
                ) {
                    if (username == "dapo@webmallng.com") {
                        // raise a fake exception to stop the process
                        throw Exception("ADMIN")
                    }
                }
                if (!it.success) {
                    throw Exception("Login Failed, Check Credentials")
                }
                val userToken = it.token
                val stormId: String =
                    JWTHelper.getStormId(userToken) ?: throw Exception("Login Failed")
                Prefs.putString(PREF_USER_TOKEN, userToken)
                val userTokenDecoded = JWT(userToken)
                val user = User().apply {
                    this.business_phone_number =
                        if (userTokenDecoded.claims.containsKey("phoneNumber")) {
                            userTokenDecoded.getClaim(
                                "phoneNumber",
                            ).asString()
                        } else {
                            " "
                        }
                    this.business_address =
                        if (userTokenDecoded.claims.containsKey("business_address")) {
                            userTokenDecoded.getClaim(
                                "business_address",
                            ).asString()
                        } else {
                            " "
                        }
                    this.terminal_id =
                        if (userTokenDecoded.claims.containsKey("terminalId")) {
                            userTokenDecoded.getClaim(
                                "terminalId",
                            ).asString()
                        } else {
                            " "
                        }
                    this.business_name =
                        if (userTokenDecoded.claims.containsKey("businessName")) {
                            userTokenDecoded.getClaim(
                                "businessName",
                            ).asString()
                        } else {
                            " "
                        }
                    this.netplus_id =
                        if (userTokenDecoded.claims.containsKey("stormId")) {
                            userTokenDecoded.getClaim(
                                "stormId",
                            ).asString()
                        } else {
                            " "
                        }
                    this.mid =
                        if (userTokenDecoded.claims.containsKey("mid")) {
                            userTokenDecoded.getClaim("mid")
                                .asString()
                        } else {
                            " "
                        }
                    this.partnerId =
                        if (userTokenDecoded.claims.containsKey("partnerId")) {
                            userTokenDecoded.getClaim(
                                "partnerId",
                            ).asString()
                        } else {
                            " "
                        }
                }
                Timber.e("DATA_OOOO=====>${user.terminal_id}")
                Single.just(user)
            }.subscribeOn(Schedulers.io())
            .doFinally { authInProgress.postValue(false) }
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe { res, error ->
                res?.let {
                    Timber.d("NEWADDRESS--->${it.business_address}")
                    Timber.d("NEWPHONADDRESS--->${it.business_phone_number}")
                    Prefs.putString(PREF_USER, gson.toJson(it))
                    Prefs.putBoolean(PREF_AUTHENTICATED, true)
                    _authDone.value = Event(true)
                }
                error?.let {
                    Timber.e(it)
                    if (it.message.equals("admin", true)) {
                        _gotoAdminPage.value = Event(true)
                        return@let
                    }
                    Timber.e(it.localizedMessage)
                    (it as? HttpException).let { httpException ->
                        val errorMessage = httpException?.response()?.errorBody()?.string()
                            ?: "{\"success\":false,\"message\":\"Unexpected error\"}"
                        _message.value = Event(
                            try {
                                gson.fromJson(errorMessage, AuthError::class.java).message
                                    ?: "Login Failed"
                            } catch (e: Exception) {
                                "login failed"
                            },
                        )
                        Timber.e(errorMessage)
                    }
                }
            }.disposeWith(disposables)
    }

    private fun easyPOSAuth(username: String, password: String, deviceId:String) {
        authInProgress.value = true
        val credentials = JsonObject()
            .apply {
                addProperty("username", username)
                addProperty("password", password)
                addProperty("deviceId", deviceId)
            }
        stormApiService!!.userToken(credentials)
            .flatMap {
                Timber.d("DATA_GOTTEN_USER_DATA_TOKENRESPONSE===>$it")
                Timber.e(it.toString())
                if (BuildConfig.BUILD_TYPE.equals(
                        "releaseAdmin",
                        true,
                    ) || BuildConfig.BUILD_TYPE.equals("nibssserverdebug", true)
                ) {
                    if (username == "dapo@webmallng.com") {
                        // raise a fake exception to stop the process
                        throw Exception("ADMIN")
                    }
                }
                if (!it.success) {
                    throw Exception("Login Failed, Check Credentials")
                }
                val userToken = it.token
                val stormId: String =
                    JWTHelper.getStormId(userToken) ?: throw Exception("Login Failed")
                Prefs.putString(PREF_USER_TOKEN, userToken)
                val userTokenDecoded = JWT(userToken)
                val user = User().apply {
                    this.business_phone_number =
                        if (userTokenDecoded.claims.containsKey("phoneNumber")) {
                            userTokenDecoded.getClaim(
                                "phoneNumber",
                            ).asString()
                        } else {
                            " "
                        }
                    this.business_address =
                        if (userTokenDecoded.claims.containsKey("business_address")) {
                            userTokenDecoded.getClaim(
                                "business_address",
                            ).asString()
                        } else {
                            " "
                        }
                    this.terminal_id =
                        if (userTokenDecoded.claims.containsKey("terminalId")) {
                            userTokenDecoded.getClaim(
                                "terminalId",
                            ).asString()
                        } else {
                            " "
                        }
                    this.business_name =
                        if (userTokenDecoded.claims.containsKey("businessName")) {
                            userTokenDecoded.getClaim(
                                "businessName",
                            ).asString()
                        } else {
                            " "
                        }
                    this.netplus_id =
                        if (userTokenDecoded.claims.containsKey("stormId")) {
                            userTokenDecoded.getClaim(
                                "stormId",
                            ).asString()
                        } else {
                            " "
                        }
                    this.mid =
                        if (userTokenDecoded.claims.containsKey("mid")) {
                            userTokenDecoded.getClaim("mid")
                                .asString()
                        } else {
                            " "
                        }
                    this.partnerId =
                        if (userTokenDecoded.claims.containsKey("partnerId")) {
                            userTokenDecoded.getClaim(
                                "partnerId",
                            ).asString()
                        } else {
                            " "
                        }
                }
                Timber.e("DATA_OOOO=====>${user.terminal_id}")
                Single.just(user)
            }.subscribeOn(Schedulers.io())
            .doFinally { authInProgress.postValue(false) }
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe { res, error ->
                res?.let {
                    Timber.d("NEWADDRESS--->${it.business_address}")
                    Timber.d("NEWPHONADDRESS--->${it.business_phone_number}")
                    Prefs.putString(PREF_USER, gson.toJson(it))
                    Prefs.putBoolean(PREF_AUTHENTICATED, true)
                    _authDone.value = Event(true)
                }
                error?.let {
                    Timber.e(it)
                    if (it.message.equals("admin", true)) {
                        _gotoAdminPage.value = Event(true)
                        return@let
                    }
                    Timber.e(it.localizedMessage)
                    (it as? HttpException).let { httpException ->
                        val errorMessage = httpException?.response()?.errorBody()?.string()
                            ?: "{\"success\":false,\"message\":\"Unexpected error\"}"
                        _message.value = Event(
                            try {
                                gson.fromJson(errorMessage, AuthError::class.java).message
                                    ?: "Login Failed"
                            } catch (e: Exception) {
                                "login failed"
                            },
                        )
                        Timber.e(errorMessage)
                    }
                }
            }.disposeWith(disposables)
    }

    fun resetPassword() {
        val username = usernameLiveData.value
        if (username.isNullOrEmpty()) {
            _message.value = Event("Please enter your email address")
            return
        }

        val payload = JsonObject().apply {
            addProperty("username", username)
        }
        stormApiService!!.passwordReset(payload).subscribeOn(Schedulers.io())
            .doOnSubscribe {
                passwordResetInProgress.postValue(true)
            }.doFinally { passwordResetInProgress.postValue(false) }
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe { t1, t2 ->
                t1?.let {
                    _message.value = if (it.code() != 200) {
                        Event("Password reset failed")
                    } else {
                        val res = JSONObject(Gson().toJson(it.body()))
                        if (!res.getBoolean("success")) {
                            Event("Password reset failed")
                        } else {
                            _passwordResetSent.value = Event(true)
                            Event("A password reset mail has been sent to $username")
                        }
                    }
                }
                t2?.let {
                    _message.value = Event("Password reset failed, try again.")
                }
            }.disposeWith(disposables)
    }

    private fun fakeLogin() = User().apply {
        this.terminal_id = "2101JJ41"
        this.business_name = "Fake Business"
        this.netplus_id = UUID.randomUUID().toString()
        this.mid = "2101JJ41MFDJ999EWR9"
        this.partnerId = UUID.randomUUID().toString()
        Prefs.putString(
            PREF_USER_TOKEN,
            "eyJ0eXAiOiJKV1QiLCJhbGciOiJSUzI1NiJ9.eyJzdG9ybUlkIjoiZDg0ZDRjYmEtMmQxNC00Y2ViLTk0NGYtYTBhNDgzNDRiNzY4IiwiYXBwbmFtZSI6InN0b3JtX2FwcCIsImJ1c2luZXNzTmFtZSI6Ik5ldHBsdXNEb3RDb20iLCJyb2xlcyI6WyJuZXRwbHVzLXNlcnZpY2UiXSwicGVybWlzc2lvbnMiOlsic3Rvcm0iXSwiaWF0IjoxNjQ2MzgzNTQ4LCJleHAiOjE2NDY0Njk5NDgsImlzcyI6InN0b3JtOmFjY291bnRzIiwic3ViIjoic2VydmljZSJ9.F15WuaqsoXozmTT7v4bfff5GnOYafNenA_ZgRXMSFMKUpYbf3PiPs3hnlh8lPmC7Fcp-0jEm7d_zBYp1RYcwmpyeuyzQVtwtj1j0WiiGJcIU9JQrwt7cZ-78Uutts0hFZwBKkOiiFuROUD2UX3npxef6hxVhn2poVxq-N5CEHdu79BUBAeWDhj-QIFCQAqAqMONgHPffSqqRP4rVxYwAG2OHnEX00aBtVohJX2bEYt6Lr2SN2BVwCKquCIfXgz2gGAL-Sv1U_vEmCkxkHt0ELXbBzjle-r4IT-KKG8pPnq06iYhScyHLujAvZ_dUDYpKkJzLxGH2dBCMypEbhqKn4g",
        )
    }

    override fun onCleared() {
        super.onCleared()
        disposables.clear()
    }
}
