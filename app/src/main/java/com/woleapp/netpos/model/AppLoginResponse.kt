package com.woleapp.netpos.model

data class AppLoginResponse(
    val `data`: DataXX,
    val success: Boolean,
    val token: String
)