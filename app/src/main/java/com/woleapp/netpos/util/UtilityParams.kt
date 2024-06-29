package com.woleapp.netpos.util

object UtilityParams {
    init {
        System.loadLibrary("module-credentials")
    }
    private external fun getRrnUrl(): String
    private external fun getMerchantId(): String
    private external fun getPayByTransferBearerToken(): String
    private external fun getPayByTransferBaseUrl(): String
    private external fun getProvidusMerchantsAccountBaseUrl(): String
    private external fun getFCMBMerchantsAccountBaseUrl(): String
    private external fun getComplaintsBaseUrl(): String
    private external fun getCheckoutBaseUrl(): String
    private external fun getWebViewBaseUrl(): String
    private external fun getCheckoutMerchantId(): String


    val RRN_BASE_URL: String = getRrnUrl()
    val STRING_MERCHANT_ID = getMerchantId()
    val PAY_BY_TRANSFER_BEARER_TOKEN = getPayByTransferBearerToken()
    val PAY_BY_TRANSFER_BASE_URL = getPayByTransferBaseUrl()
    val COMPLAINTS_BASE_URL = getComplaintsBaseUrl()
    val PROVIDUS_MERCHANTS_ACCOUNT_BASE_URL = getProvidusMerchantsAccountBaseUrl()
    val FCMB_MERCHANTS_ACCOUNT_BASE_URL = getFCMBMerchantsAccountBaseUrl()
    val STRING_CHECKOUT_BASE_URL = getCheckoutBaseUrl()
    val STRING_WEB_VIEW_BASE_URL = getWebViewBaseUrl()
    val STRING_CHECKOUT_MERCHANT_ID = getCheckoutMerchantId()
}
