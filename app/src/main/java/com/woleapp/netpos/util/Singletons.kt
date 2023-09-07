package com.woleapp.netpos.util

import com.danbamitale.epmslib.entities.ConfigData
import com.danbamitale.epmslib.entities.KeyHolder
import com.danbamitale.epmslib.entities.TransactionResponse
import com.danbamitale.epmslib.entities.responseMessage
import com.google.gson.Gson
import com.pixplicity.easyprefs.library.Prefs
import com.woleapp.netpos.model.ConfigurationData
import com.woleapp.netpos.model.NibssResponse
import com.woleapp.netpos.model.User
import com.woleapp.netpos.nibss.DEFAULT_TERMINAL_ID
import com.woleapp.netpos.nibss.Keys

fun useStormTerminalId() = Prefs.getBoolean(PREF_USE_STORM_TERMINAL_ID, true)
fun TransactionResponse.toNibssResponse(remark: String? = null): NibssResponse =
    Singletons.gson.fromJson(
        Singletons.gson.toJson(this),
        NibssResponse::class.java,
    ).also {
        it.responseMessage = try {
            this.responseMessage
        } catch (e: Exception) {
            ""
        }
        it.additionalAmount = this.additionalAmount_54.toLongOrNull() ?: 0
        it.localDate = this.localDate_13
        it.localTime = this.localTime_12
        it.amount = this.amount.div(100)
        remark?.let { r ->
            it.remark = r
        }
    }

object Singletons {
    fun setUseStormTid(useStormTid: Boolean) =
        Prefs.putBoolean(PREF_USE_STORM_TERMINAL_ID, useStormTid)

    val gson = Gson()
    fun getCurrentlyLoggedInUser(): User? =
        gson.fromJson(Prefs.getString(PREF_USER, ""), User::class.java)

    fun getSavedConfigurationData(): ConfigurationData {
        return ConfigurationData(
            "196.6.103.18",
            "4016",
            DEFAULT_TERMINAL_ID,
            Keys.posvasLiveKey1,
            Keys.posvasLiveKey2,
        )
//        configurationData?.let {
//            return it
//        }
//        return ConfigurationData(
//            DEFAULT_NIBSS_IP,
//            DEFAULT_NIBSS_PORT.toString(),
//            DEFAULT_TERMINAL_ID,
//            Keys.liveKey1,
//            Keys.liveKey2
//        )
    }

    fun getKeyHolder(): KeyHolder? =
        gson.fromJson(Prefs.getString(PREF_KEYHOLDER, null), KeyHolder::class.java)

    fun getConfigData(): ConfigData? =
        gson.fromJson(Prefs.getString(PREF_CONFIG_DATA, null), ConfigData::class.java)
}

var TransactionResponse.additionalAmount: Long?
    get() = 0
    set(value) {
    }

sealed class LoadingState

object LoadingMore : LoadingState()
object LoadingInitial : LoadingState()
object LoadingDone : LoadingState()
data class LoadingError(val errorMessage: String, val exception: Throwable) : LoadingState()
