package com.woleapp.netpos.util.resourceWrapper

data class Resource<out T>(
    val status: Status,
    val data: T?,
    val message: String
) {
    companion object {
        fun<T> success(data: T?): Resource<T> = Resource(
            Status.SUCCESS,
            data,
            "Success"
        )

        fun<T> error(data: T?): Resource<T> = Resource(
            Status.ERROR,
            data,
            "Failed..."
        )

        fun<T> loading(): Resource<T> = Resource(
            Status.LOADING,
            null,
            "Loading..."
        )

        fun<T> timeOut(): Resource<T> = Resource(
            Status.TIMEOUT,
            null,
            "Time out! Please try again later."
        )

        fun<T> initialDefault(): Resource<T> = Resource(
            Status.INITIAL_DEFAULT,
            null,
            "App just start up, no action required"
        )
    }
}
