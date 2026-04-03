@file:Suppress("ktlint:standard:no-wildcard-imports")

package com.woleapp.netpos.nibss

import android.content.Context
import android.content.Intent
import android.text.format.DateUtils
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.danbamitale.epmslib.entities.*
import com.danbamitale.epmslib.processors.TerminalConfigurator
import com.horizonpay.smartpossdk.aidl.pinpad.DukptObj
import com.horizonpay.smartpossdk.data.PinpadConst
import com.netpluspay.netpossdk.NetPosSdk
import com.netpluspay.netpossdk.NetPosSdk.writeTpkKey
import com.pixplicity.easyprefs.library.Prefs
import com.woleapp.netpos.app.DeviceHelper
import com.woleapp.netpos.model.ConfigurationData
import com.woleapp.netpos.util.*
import com.woleapp.netpos.util.Singletons.getSavedConfigurationData
import com.woleapp.netpos.util.Singletons.gson
import com.woleapp.netpos.util.horizonpay.HexUtil
import com.woleapp.netpos.util.horizonpay.K11HardwareBridge
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import timber.log.Timber
import java.util.concurrent.TimeUnit

// const val NIBSS_TEST_IP = "196.6.103.72"
// const val NIBSS_PROD_IP = "196.6.103.73"
// const val TERMINAL_SERIAL = "0123456789ABC"
// NOTE: Currently using test IP 196.6.103.72 in Singletons.getSavedConfigurationData()
const val CONFIGURATION_STATUS = "terminal_configuration_status"
const val CONFIGURATION_ACTION = "com.woleapp.netpos.TERMINAL_CONFIGURATION"
const val DEFAULT_TERMINAL_ID = "2057H63U"

class NetPosTerminalConfig {
    companion object {
        private var configurationData: ConfigurationData = getSavedConfigurationData()
        private val disposables = CompositeDisposable()
        var connectionData: ConnectionData =
            ConnectionData(
                ipAddress = configurationData.ip,
                ipPort = configurationData.port.toInt(),
                isSSL = true,
            )
        private var terminalId: String? = null
        var isConfigurationInProcess = false
        var configurationStatus = -1
        private val mutableLiveData = MutableLiveData(Event(-99))
        val liveData: LiveData<Event<Int>>
            get() = mutableLiveData
        private val sendIntent = Intent(CONFIGURATION_ACTION)
        private var terminalConfigurator: TerminalConfigurator =
            TerminalConfigurator(connectionData)

        fun getTerminalId() = terminalId ?: ""

        private fun refreshConnection() {
            // Ensure we always pick up the latest config (and recreate configurator).
            configurationData = getSavedConfigurationData()
            connectionData =
                ConnectionData(
                    ipAddress = configurationData.ip,
                    ipPort = configurationData.port.toInt(),
                    isSSL = true,
                )
            terminalConfigurator = TerminalConfigurator(connectionData)
        }

        private fun setTerminalId() {
            terminalId = (Singletons.getCurrentlyLoggedInUser()?.terminal_id).toString().trim()
        }

        private var keyHolder: KeyHolder? = null

        private var configData: ConfigData? = null

        fun getConfigData(): ConfigData? = configData

        fun getKeyHolder(): KeyHolder? {
            Timber.d("KEYHOLDER_DEBUG: NetPosTerminalConfig.getKeyHolder() in-memory - exists=%s, sessionKeyLen=%s, pinKeyLen=%s", keyHolder != null, keyHolder?.clearSessionKey?.length ?: "N/A", keyHolder?.clearPinKey?.length ?: "N/A")
            return keyHolder
        }

        fun init(
            context: Context,
            configureSilently: Boolean = false,
        ) {
            Timber.d("NIBSS_INIT: called with configureSilently=$configureSilently, isConfigurationInProcess=$isConfigurationInProcess")
            refreshConnection()
            KeyHolder.setHostKeyComponents(
                configurationData.key1,
                configurationData.key2,
            ) // default to test  //Set your base keys here

            Timber.d(
                "NIBSS_INIT: connectionData ip=${connectionData.ipAddress}, port=${connectionData.ipPort}, ssl=${connectionData.isSSL}"
            )

            setTerminalId()
            Timber.e("NIBSS_INIT: Terminal ID resolved to: $terminalId")
            keyHolder = Singletons.getKeyHolder()
            configData = Singletons.getConfigData()
            Timber.d("KEYHOLDER_DEBUG: init() - Initial load from Prefs - keyHolder=%s, configData=%s", keyHolder != null, configData != null)
            
            // Initialize DUKPT if keyHolder exists
            keyHolder?.let { holder ->
                Timber.d("DUKPT_INIT: KeyHolder exists at startup, initializing DUKPT")
                try {
                    val pinpad = DeviceHelper.getPinpad()
                    Timber.d("DUKPT_INIT: Setting key algorithm to DUKPT_2")
//                    pinpad.setKeyAlgorithm(PinpadConst.KeyAlgorithm.DUKPT_2)
//                    val ksn = "FFFF9876543210" + "000001"
//                    Timber.d("DUKPT_INIT: Loading DUKPT key at startup")
//                    val dukptObj = DukptObj(holder.clearPinKey, ksn, PinpadConst.DukptKeyType.DUKPT_IPEK_PLAINTEXT, PinpadConst.DukptKeyIndex.DUKPT_KEY_INDEX_0)
//                    val ret = pinpad.dukptKeyLoad(dukptObj)

                    pinpad.setKeyAlgorithm(PinpadConst.KeyAlgorithm.DES) // Ensure algorithm is DES/3DES

                    val pinKeyBytes = HexUtil.hexStringToByte(holder.clearPinKey)
                    // In standard MSK, we usually just pass a dummy KCV if we don't have one to verify
                    val dummyKcv = ByteArray(4)

                    // Inject the PIN Key into Index 0
                    val isPinLoaded = pinpad.injectPlainWorkKey(
                        0, // WORK_KEY_INDEX
                        PinpadConst.PinPadKeyType.TPINK,
                        pinKeyBytes,
                        dummyKcv
                    )
                    Timber.d("3DES_INIT: PIN Key load result: $isPinLoaded")

                } catch (e: Exception) {
                    Timber.e(e, "DUKPT_INIT: DUKPT load failed at startup")
                }
            }
            
            val localBroadcastManager = LocalBroadcastManager.getInstance(context)
            if (isConfigurationInProcess) {
                Timber.w("NIBSS_INIT: configuration already in process; will not start a second one")
                // Still emit current status so UI can decide what to do (e.g., keep waiting or show retry).
                if (configureSilently.not()) {
                    mutableLiveData.value = Event(configurationStatus)
                    mutableLiveData.value = Event(-99)
                }
                return
            }
            configurationStatus = 0
            sendIntent.putExtra(CONFIGURATION_STATUS, configurationStatus)
            localBroadcastManager.sendBroadcast(sendIntent)
            if (configureSilently.not()) {
                mutableLiveData.value = Event(configurationStatus)
                mutableLiveData.value = Event(-99)
            }
            val lastConfigTime = Prefs.getLong(LAST_POS_CONFIGURATION_TIME, 0)
            Timber.d("NIBSS_INIT: lastConfigTime=$lastConfigTime, isToday=${DateUtils.isToday(lastConfigTime)}, hasKeyHolder=${keyHolder != null}, hasConfigData=${configData != null}")

            val req =
                when {
                    DateUtils.isToday(lastConfigTime).not() -> {
                        Timber.e("NIBSS_INIT: last configuration time was not today, configure terminal now")
                        configureTerminal(context)
                    }
                    keyHolder != null && configData != null -> {
                        Timber.e("NIBSS_INIT: calling home with existing keys/config")
                        configurationStatus = 1
                        callHome(context).onErrorResumeNext {
                            Timber.e(it)
                            Timber.e("NIBSS_INIT: call home failed, fall back to configureTerminal")
                            configureTerminal(context)
                        }
                    }
                    else -> {
                        Timber.e("NIBSS_INIT: missing keys/config, calling configureTerminal")
                        configureTerminal(context)
                    }
                }
            val disposable =
                req.subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .doOnSubscribe {
                        Timber.d("NIBSS_INIT: subscription started")
                        isConfigurationInProcess = true
                    }
                    .doFinally {
                        Timber.d("NIBSS_INIT: subscription finished (success or error)")
                        isConfigurationInProcess = false
                    }
                    .subscribe { pair, error ->
                        Timber.d("NIBSS_INIT: subscribe callback - error=%s, pair=%s", error != null, pair != null)
                        error?.let {
                            // TerminalManager.getInstance().beep(context, TerminalManager.BEEP_MODE_FAILURE)
                            configurationStatus = -1
                            Timber.e(it, "NIBSS_INIT: configuration error, setting status=-1")
                            if (configureSilently.not()) {
                                mutableLiveData.value = Event(configurationStatus)
                                mutableLiveData.value = Event(-99)
                            }
                            sendIntent.putExtra(CONFIGURATION_STATUS, configurationStatus)
                            localBroadcastManager.sendBroadcast(sendIntent)
                            Timber.e(it)
                        }
                        pair?.let {
                            Timber.d("NIBSS_INIT: pair is not null, first=%s", pair.first != null)
                            // Use pair.first if available, otherwise use the existing keyHolder (from callHome)
                            val keyHolderToUse = pair.first ?: this.keyHolder
                            Timber.d("NIBSS_INIT: keyHolderToUse=%s (from pair=%s, fallback=%s)", keyHolderToUse != null, pair.first != null, this.keyHolder != null)
                            
                            keyHolderToUse?.let {
                                Timber.d("NIBSS_INIT: received fresh keys and config, persisting to Prefs")
                                Timber.d("KEYHOLDER_DEBUG: Fresh keyHolder received from server - sessionKeyLen=%s, pinKeyLen=%s", it.clearSessionKey.length, it.clearPinKey.length)
                                Prefs.putLong(LAST_POS_CONFIGURATION_TIME, System.currentTimeMillis())
                                pair.second?.let { Prefs.putString(PREF_CONFIG_DATA, gson.toJson(it)) }
                                Prefs.putString(PREF_KEYHOLDER, gson.toJson(it))
//                                writeTpkKey(0, it.clearPinKey, context)
                                Timber.d("DUKPT_INIT: About to initialize DUKPT")
                                try {
                                    Timber.d("DUKPT_INIT: Getting pinpad")
                                    val pinpad = DeviceHelper.getPinpad()
                                    Timber.d("DUKPT_INIT: Setting key algorithm to DUKPT_2")
//                                    pinpad.setKeyAlgorithm(PinpadConst.KeyAlgorithm.DUKPT_2)
//                                    val ksn = "FFFF9876543210" + "000001"
//                                    Timber.d("DUKPT_INIT: Creating DukptObj with clearPinKey and ksn=$ksn")
//                                    val dukptObj = DukptObj(it.clearPinKey, ksn, PinpadConst.DukptKeyType.DUKPT_IPEK_PLAINTEXT, PinpadConst.DukptKeyIndex.DUKPT_KEY_INDEX_0)
//                                    Timber.d("DUKPT_INIT: Loading DUKPT key")
//                                    val ret = pinpad.dukptKeyLoad(dukptObj)
//                                    Timber.d("DUKPT_INIT: DUKPT load result: $ret")

                                    pinpad.setKeyAlgorithm(PinpadConst.KeyAlgorithm.DES) // Ensure algorithm is DES/3DES

                                    val pinKeyBytes = HexUtil.hexStringToByte(it.clearPinKey)
                                    // In standard MSK, we usually just pass a dummy KCV if we don't have one to verify
                                    val dummyKcv = ByteArray(4)

                                    // Inject the PIN Key into Index 0
                                    val isPinLoaded = pinpad.injectPlainWorkKey(
                                        0, // WORK_KEY_INDEX
                                        PinpadConst.PinPadKeyType.TPINK,
                                        pinKeyBytes,
                                        dummyKcv
                                    )
                                    Timber.d("3DES_INIT: PIN Key load result: $isPinLoaded")

                                } catch (e: Exception) {
                                    Timber.e(e, "DUKPT_INIT: DUKPT load failed")
                                }
                                Prefs.putString(CLEAR_PIN_KEY, it.clearPinKey)
                                Log.d("CLEAR_PIN_KEY", it.clearPinKey)
                                this.keyHolder = it
                                Timber.d("KEYHOLDER_DEBUG: In-memory keyHolder updated - sessionKeyLen=%s, pinKeyLen=%s", this.keyHolder?.clearSessionKey?.length, this.keyHolder?.clearPinKey?.length)
                                pair.second?.let { this.configData = it }
                            } ?: run {
                                Timber.e("DUKPT_INIT: keyHolderToUse is null, cannot initialize DUKPT")
                            }
                            configurationStatus = 1
                            Timber.d("NIBSS_INIT: configuration successful, status=1")
                            sendIntent.putExtra(CONFIGURATION_STATUS, configurationStatus)
                            localBroadcastManager.sendBroadcast(sendIntent)
                            if (configureSilently.not()) {
                                mutableLiveData.value = Event(configurationStatus)
                                mutableLiveData.value = Event(-99)
                            }
                            Timber.e("Config data set")
                            disposeDisposables()
                        }
                    }
            disposables.add(disposable)
        }

        private fun callHome(context: Context): Single<Pair<KeyHolder?, ConfigData?>> {
            Timber.e(keyHolder.toString())

            val serial = if (android.os.Build.MODEL.contains("K11")) {
                Log.d("SERIAL NUMBER", "OKAYYY")
                K11HardwareBridge.getSecureSN()
            } else {
                Log.d("SERIAL NUMBER222", "OKAYYYYYYNNNN")
                NetPosSdk.getDeviceSerial()
            }
            Timber.d("NIBSS_CALLHOME: tid=${getTerminalId()}, serial=$serial, sessionKeyPresent=${keyHolder?.clearSessionKey.isNullOrBlank().not()}")

            return terminalConfigurator.nibssCallHome(
                context,
                getTerminalId(),
                keyHolder?.clearSessionKey ?: "",
                serial,
            )
                .timeout(45, TimeUnit.SECONDS)
                .doOnSubscribe { Timber.d("NIBSS_CALLHOME: request started") }
                .doOnSuccess { Timber.d("NIBSS_CALLHOME: raw result=$it") }
                .doOnError { Timber.e(it, "NIBSS_CALLHOME: failed") }
                .flatMap {
                Timber.e("call home result $it")
                if (it == "00") {
                    return@flatMap Single.just(Pair(null, null))
                } else {
                    Single.error(Exception("call home failed"))
                }
            }
        }

        private fun configureTerminal(context: Context): Single<Pair<KeyHolder?, ConfigData?>> =
            terminalConfigurator.downloadNibssKeys(context, getTerminalId())
                .timeout(30, TimeUnit.SECONDS)
                .doOnSubscribe { Timber.d("NIBSS_CONFIG: downloadNibssKeys started tid=${getTerminalId()}") }
                .doOnSuccess { Timber.d("NIBSS_CONFIG: downloadNibssKeys success sessionKeyLen=${it.clearSessionKey.length}, pinKeyLen=${it.clearPinKey.length}") }
                .doOnError { Timber.e(it, "NIBSS_CONFIG: downloadNibssKeys failed") }
                .flatMap { nibssKeyHolder ->
                    keyHolder = nibssKeyHolder

                    val serial = if (android.os.Build.MODEL.contains("K11")) {
                        Log.d("SSSSERIAL_NUMBER", "${K11HardwareBridge.getSecureSN()}")
//                        Log.d("SSSSERIAL_NUMBER11", "${NetPosSdk.getDeviceSerial()}")
                        K11HardwareBridge.getSecureSN()
                    } else {
                        Log.d("SSSSERIAL_NUMBER222", "OKAYYY")
                        NetPosSdk.getDeviceSerial()
                    }
                    Timber.d(
                        "NIBSS_CONFIG: downloadTerminalParameters starting tid=${getTerminalId()}, serial=$serial, sessionKeyPrefix=${nibssKeyHolder.clearSessionKey.take(8)}"
                    )

                    terminalConfigurator.downloadTerminalParameters(
                        context,
                        getTerminalId(),
                        nibssKeyHolder.clearSessionKey,
                        serial,
                    )
                        .timeout(25, TimeUnit.SECONDS)
                        .doOnSuccess { Timber.d("NIBSS_CONFIG: downloadTerminalParameters success") }
                        .doOnError { Timber.e(it, "NIBSS_CONFIG: downloadTerminalParameters failed") }
                        .map { nibssConfigData ->
                        configData = nibssConfigData
                        return@map Pair(nibssKeyHolder, nibssConfigData)
                    }
                }

        fun disposeDisposables() {
            disposables.clear()
        }
    }
}
