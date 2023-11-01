package com.woleapp.netpos.util

import com.woleapp.netpos.BuildConfig
import com.woleapp.netpos.model.GetZenithPayByTransferUserTransactionsModel

const val STATE_PAYMENT_STAND_BY = 0
const val STATE_PAYMENT_STARTED = 1
const val STATE_PAYMENT_APPROVED = 2
const val STRING_EOD_TITLE_HEADER = "         RRN           ST  TIME  AMOUNT"
const val HISTORY_ACTION = "history_action"
const val WEMA_AGENCY_PD = "CEF5A9E7-97EE-492A-8EB6-8981FE88CC46"
const val HISTORY_ACTION_DEFAULT = "Default"
const val HISTORY_ACTION_REPRINT = "Reprint"
const val HISTORY_ACTION_REFUND = "Refund"
const val HISTORY_ACTION_PREAUTH = "PreAuth"
const val HISTORY_ACTION_EOD = "End Of Day"
const val HISTORY_ACTION_CASH = "Cash"
const val TRANSACTION_TYPE = "transaction_type"
const val PREF_APP_TOKEN = "app_token"
const val PREF_USER_TOKEN = "user_token"
const val PREF_ZENITH_PBT_USER_ACCOUNT = "PREF_ZENITH_PBT_USER_ACCOUNT"
const val PREF_USER = "user"
const val PREF_AUTHENTICATED = "authenticated"
const val PREF_CONFIGURATION_DATA = "configuration_data"
const val PREF_KEYHOLDER = "pref_keyholder"
const val PREF_CONFIG_DATA = "pref_config_data"
const val PREF_LAST_LOCATION = "pref_last_location"
const val PREF_FIREBASE_APP_TOKEN = "pref_firebase_app_token"
const val PREF_USE_STORM_TERMINAL_ID = "pref_use_storm_tid"
const val PREF_BILLS_TOKEN = "pref_bills_token"
const val PREF_PRINTER_SETTINGS = "pref_printer_settings"
const val PREF_VALUE_PRINT_CUSTOMER_COPY_ONLY = "print_customer_copy_only"
const val PREF_VALUE_PRINT_CUSTOMER_AND_MERCHANT_COPY = "print_merchant_and_customer_copy"
const val PREF_VALUE_PRINT_ASK_BEFORE_PRINTING = "ask_before_printing"
const val PREF_VALUE_PRINT_SHARE_RECEIPT = "share"
const val PREF_VALUE_PRINT_DOWNLOAD_AND_SHARE_RECEIPT = "download_and_share"
const val PREF_VALUE_PRINT_DOWNLOAD_RECEIPT = "download"
const val PREF_VALUE_PRINT_SMS = "send_sms"
const val LAST_POS_CONFIGURATION_TIME = "last_pos_configuration_time"
const val VEND_IP = "192.168.100.68"
const val VEND_PORT = 3535
const val VEND_PROD_IP = "vend.netpluspay.com"
const val VEND_PROD_PORT = 3535
const val WRITE_PERMISSION_REQUEST_CODE = 113
const val PDF_REPRINT_IDENTIFIER = "PDF_REPRINT_IDENTIFIER"
const val TRANSACTION_LAST_LOADED_PAGE = "transaction_last_loaded_page"
const val TRANSACTION_BY_TID_LAST_LOADED_PAGE = "transaction_by_tid_last_loaded_page"
const val PREF_REPRINT_PASSWORD = "reprint_password"
val X_CLIENT_ID = if (BuildConfig.FLAVOR.equals(
        "wemacashout",
        true
    )
) "09978e4d-7c07-4987-963e-29d9fe3b9387" else "b7c4fc42-4e1a-4493-bb4e-bf798d9ce8a1"
val X_ACCESS_CODE = if (BuildConfig.FLAVOR.equals(
        "wemacashout",
        true
    )
) "b1e6ef8d102a64411deeb1f2a9b4981069264b6abf1850adf0af7b16b1e62ecd" else "9837a93abecc10faf7a36145401c1d9aabdc60d7d88cfba411a5b2dcd4423709"
val GATEWAY_MAP = HashMap<String, String>().apply {
    put("X-CLIENT-ID", X_CLIENT_ID)
    put("X-ACCESSCODE", X_ACCESS_CODE)
}

const val TAG_NOTIFICATION_RECEIVED = "TAG_NOTIFICATION_RECEIVED"
const val TAG_NOTIFICATION_RECEIVED_2 = "TAG_NOTIFICATION_RECEIVED_2"
const val STRING_FIREBASE_INTENT_ACTION = "com.woleapp.netpos.FIREBASE_ACTION"
const val TAG_NOTIFICATION_RECEIVED_FROM_BACKEND = "TAG_NOTIFICATION_RECEIVED_2"
const val TAG_NEW_TOKEN_RECEIVED = "TAG_NEW_TOKEN_RECEIVED"
const val WORKER_INPUT_PBT_TRANSACTION_TAG = "WORKER_INPUT_PBT_TRANSACTION_TAG"
const val WORKER_INPUT_FIREBASE_DEVICE_TOKEN_TAG = "WORKER_INPUT_FIREBASE_DEVICE_TOKEN_TAG"
const val TAG_PROGRESS_LOADER = "TAG_PROGRESS_LOADER"
const val STRING_LOADING_DIALOG_TAG = "STRING_LOADING_DIALOG_TAG"
const val PIN_BLOCK_RK = "PIN_BLOCK_RK"
const val PIN_BLOCK_BK = "PIN_BLOCK_BK"

const val INT_FIREBASE_PENDING_INTENT_REQUEST_CODE = 772
const val CONTACTLESS_TRANSACTION_DEFAULT_EMAIL = "contactless@gmail.com"
const val PAYMENT_KEY = "payment_key"
const val CURRENCY_KEY = "currency_key"
const val CONTACT = "contact"
const val CUSTOMER = "customer"

val testPbtTransactions = arrayListOf(
    GetZenithPayByTransferUserTransactionsModel(
        amount = 50.00,
        recipient_account_number = "1198153105",
        transaction_reference = "000013220519124631000219551844",
        paid_at = "2022-05-19T11:46:43.000Z",
        channel = "NIP Inward (Credit) - Mobile Phones",
        type = "C",
        payer_account_name = "AJALA DOYINSOLA OLUWATOSIN",
        payer_account_number = "0122565011",
        payer_bank_code = "000013",
        merchantId = "nfuuh347rfhier4hf3hie48r39f84erf3i4r7fj3847rfhie",
        partnerId = "1234567890",
        details = "NIP/GTB/AJALA DOYINSOLA OLUWATOSIN/REF421459419000000050002205191246",
        terminalId = "0987654321"
    ),
    GetZenithPayByTransferUserTransactionsModel(
        amount = 50000.00,
        recipient_account_number = "1198153105",
        transaction_reference = "000013220519124631000219551845",
        paid_at = "2022-05-20T11:46:43.000Z",
        channel = "NIP Inward (Credit) - Mobile Phones",
        type = "C",
        payer_account_name = "AJALA DOYINSOLA OLUWATOSIN",
        payer_account_number = "0122565011",
        payer_bank_code = "000013",
        merchantId = "nfuuh347rfhier4hf3hie48r39f84erf3i4r7fj3847rfhie",
        partnerId = "1234567890",
        details = "NIP/GTB/AJALA DOYINSOLA OLUWATOSIN/REF421459419000000050002205191246",
        terminalId = "0987654321"
    ),
    GetZenithPayByTransferUserTransactionsModel(
        amount = 75000.00,
        recipient_account_number = "1198153105",
        transaction_reference = "000013220519124631000219551848",
        paid_at = "2022-05-19T11:55:43.000Z",
        channel = "NIP Inward (Credit) - Mobile Phones",
        type = "C",
        payer_account_name = "AJALA DOYINSOLA OLUWATOSIN",
        payer_account_number = "0122565011",
        payer_bank_code = "000013",
        merchantId = "nfuuh347rfhier4hf3hie48r39f84erf3i4r7fj3847rfhie",
        partnerId = "1234567890",
        details = "NIP/GTB/AJALA DOYINSOLA OLUWATOSIN/REF421459419000000050002205191246",
        terminalId = "0987654321"
    ),
    GetZenithPayByTransferUserTransactionsModel(
        amount = 200000.00,
        recipient_account_number = "1198153105",
        transaction_reference = "000013220519124631000219551847",
        paid_at = "2022-05-20T12:55:43.000Z",
        channel = "NIP Inward (Credit) - Mobile Phones",
        type = "C",
        payer_account_name = "AJALA DOYINSOLA OLUWATOSIN",
        payer_account_number = "0122565011",
        payer_bank_code = "000013",
        merchantId = "nfuuh347rfhier4hf3hie48r39f84erf3i4r7fj3847rfhie",
        partnerId = "1234567890",
        details = "NIP/GTB/AJALA DOYINSOLA OLUWATOSIN/REF421459419000000050002205191246",
        terminalId = "0987654321"
    )


)
