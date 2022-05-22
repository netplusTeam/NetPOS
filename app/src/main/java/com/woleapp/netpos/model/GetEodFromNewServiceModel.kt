package com.woleapp.netpos.model

data class GetEodFromNewServiceModel(
    var terminalId: String,
    var from: String = "",
    var to: String = "",
    var page: Int,
    var pageSize: Int
)
