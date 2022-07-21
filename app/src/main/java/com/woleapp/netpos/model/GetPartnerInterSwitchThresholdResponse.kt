package com.woleapp.netpos.model

data class GetPartnerInterSwitchThresholdResponse(
    val interSwitchThreshold: Int,
    val bankAccountNumber: String,
    val institutionalCode: String
)
