package com.woleapp.netpos.util

object ConnectionErrorConstants {
    private const val CONNECTION_ERROR_MESSAGE_1 =
        "Connection timed out, failed to receive response from remote server"
    private const val CONNECTION_ERROR_MESSAGE_2 =
        "Could not connect to the internet, check your connection settings and try again"
    private const val CONNECTION_ERROR_MESSAGE_3 =
        "Could not connect with remote server, check your connection settings and try again"
    private const val CONNECTION_ERROR_MESSAGE_4 =
        "Could not connect with remote server, port is unreachable, check your connection settings and try again"
    private const val CONNECTION_ERROR_MESSAGE_5 =
        "Malformed url, check your connection settings and try again"
    private const val CONNECTION_ERROR_MESSAGE_6 =
        "Could not bind socket to local address or port, check your connection settings and try again"
    private const val CONNECTION_ERROR_MESSAGE_7 =
        "Could not create socket, check your connection settings and try again"
    private const val CONNECTION_ERROR_MESSAGE_8 =
        "Host address could not be recognized, check your connection settings and try again"
    private const val CONNECTION_ERROR_MESSAGE_9 =
        "Unknown service, check your connection settings and try again"

    fun isConnectionError(responseMessage: String): Boolean {
        return when (responseMessage) {
            CONNECTION_ERROR_MESSAGE_1 -> true
            CONNECTION_ERROR_MESSAGE_2 -> true
            CONNECTION_ERROR_MESSAGE_3 -> true
            CONNECTION_ERROR_MESSAGE_4 -> true
            CONNECTION_ERROR_MESSAGE_5 -> true
            CONNECTION_ERROR_MESSAGE_6 -> true
            CONNECTION_ERROR_MESSAGE_7 -> true
            CONNECTION_ERROR_MESSAGE_8 -> true
            CONNECTION_ERROR_MESSAGE_9 -> true
            else -> false
        }
    }
}
