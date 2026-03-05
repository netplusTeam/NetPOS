package com.woleapp.netpos.util

object TerminalDetector {
    fun isHorizonPay(): Boolean = android.os.Build.MODEL.contains("K11", true) || 
                                  android.os.Build.MANUFACTURER.contains("Horizon", true)
    
    fun isKozen(): Boolean = android.os.Build.MODEL.contains("P12", true) // Example Kozen model
}