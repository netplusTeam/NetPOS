package com.woleapp.netpos.util

object UtilityParams {
    init {
        System.loadLibrary("module-credentials")
    }
    private external fun getRrnUrl(): String
    val RRN_BASE_URL: String = getRrnUrl()
}
