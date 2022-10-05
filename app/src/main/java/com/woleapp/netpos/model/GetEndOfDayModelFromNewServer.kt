package com.woleapp.netpos.model

data class GetEndOfDayModelFromNewServer(
    val data: Data,
    val message: String,
    val status: String
)

data class GetEndOfDayModelFromNewServerModified(
    val value: GetEndOfDayModelFromNewServer
)

data class GetEndOfDayModelFromNewServer2(
    val data: Data2,
    val message: String,
    val status: String
)
