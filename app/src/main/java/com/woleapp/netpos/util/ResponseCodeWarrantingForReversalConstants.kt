package com.woleapp.netpos.util

object ResponseCodeWarrantingForReversalConstants {
    // falls under completed Partially message reason
    private const val RESPONSE_CODE_COMPLETED_PARTIALLY = "32"

    // falls under Unspecified reason
    private const val RESPONSE_CODE_APPROVED_FOR_PARTIAL_AMOUNT = "10"
    private const val RESPONSE_CODE_APPROVED_VIP = "11"
    private const val RESPONSE_CODE_APPROVED_UPDATE_TRACK_3 = "16"
    private const val RESPONSE_CODE_SUSPECTED_MALFUNCTION = "22"
    private const val RESPONSE_CODE_SYSTEM_MALFUNCTION = "96"

    fun wasTransactionCompletedPartially(responseCode: String): Boolean =
        responseCode == RESPONSE_CODE_COMPLETED_PARTIALLY

    fun doesResponseCodeWarrantsReversal(responseCode: String): Boolean {
        return when (responseCode) {
            RESPONSE_CODE_APPROVED_FOR_PARTIAL_AMOUNT -> true
            RESPONSE_CODE_APPROVED_VIP -> true
            RESPONSE_CODE_APPROVED_UPDATE_TRACK_3 -> true
            RESPONSE_CODE_SUSPECTED_MALFUNCTION -> true
            RESPONSE_CODE_SYSTEM_MALFUNCTION -> true
            else -> false
        }
    }
}
