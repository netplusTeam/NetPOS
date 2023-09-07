package com.woleapp.netpos.util.encryption

object DeleteGabage {

    private val aa = mapOf(
        "AuthUserName" to "qrUser2022",
        "AuthPassword" to "Qr3*fgZ(vBcdfP0^%klo51r",
        "QrBaseUrl" to "https://getqr.netpluspay.com/",
        "SendVerveOtpBaseUrl" to "https://webpay.netpluspay.com/",
        "MerchantId" to "MID635ff140365c5",
        "WebViewBaseUrl" to "https://webpay.netpluspay.com/transactions/requery/",
        "ContactlessExistingBaseUrl" to "https://contactless.netpluspay.com/",
        "ContactlessPaymentWithQrBaseUrl" to "https://qrapi.netpluspay.com/",
        "MpgsTag" to "MPGS-QR",
        "NotificationBaseUrl" to "https://netpos.netpluspay.com/api/",
        "BaseUrlNetPosMqtt" to "storm-mqtt.netpluspay.com",
        "BaseUrlStormUtilities" to "https://storm-utilities.netpluspay.com/",
        "ConfigurationDataIp" to "196.6.103.18",
        "ConfigurationDataPort" to "5016",
        "ConfigurationDataVendorIp" to "192.168.100.68",
        "ConfigurationDataVendorPort" to "3535",
        "AppName" to "storm_app",
        "AppPassword" to "C0R3MELTDOWN!",
        "BaseUrlBills" to "https://storm.netpluspay.com/",
        "MasterPassQr" to "https://masterpassqr.netpluspay.com/api/v1/",
        "SmsBaseUrl" to "https://sms.netpluspay.com",
        "ZenityBaseUrl" to "https://api.zenith.netpluspay.com/qr/",
        "StringNotificationBaseUrlNoApi" to "https://netpos.netpluspay.com/",
        "NetposTransactionApiBaseUrl" to "https://device.netpluspay.com/",
        "RrnServiceBaseUrl" to "https://getrrn.netpluspay.com",
        "TestKey2" to "86CBCDE3B0A22354853E04521686863D",
        "TestKey1" to "5D25072F04832A2329D93E4F91BA23A2",
        "EpmsLiveKey1" to "E6891F73948F16C4D6E979D68534D0F4",
        "EpmsLiveKey2" to "3D10EF707F98E3543E32B570E9E9AE86",
        "CertKey1" to "5D25072F04832A2329D93E4F91BA23A2",
        "CertKey2" to "86CBCDE3B0A22354853E04521686863D",
        "PosVasLiveKey1" to "9BF76D3E13ADD67A51549B7C3EB0E3AD",
        "PosVasLiveKey2" to "A4BAEC5E31BFD913919262C7A7A76D52",
        "PartnerIdFirstB" to "7FD43DF1-633F-4250-8C6F-B49DBB9650EA",
        "PartnerIdEasyPay" to "1B0E68FD-7676-4F2C-883D-3931C3564190",
        "PartnerIdFcmbEasyPay" to "1B0E68FD-7676-4F2C-883D-3931C3564190",
        "PartnerIdEasyPayFcmb" to "1B0E68FD-7676-4F2C-883D-3931C3564190",
        "PartnerIdProvidusPos" to "8B26F328-040F-4F27-A5BC-4414AB9D1EFA",
        "PartnerIdStanbic" to "8B26F328-040F-4F27-A5BC-4414AB9D1EFA",
        "PartnerIdProvidus" to "8B26F328-040F-4F27-A5BC-4414AB9D1EFA",
        "PartnerIdProvidusSoftPos" to "8B26F328-040F-4F27-A5BC-4414AB9D1EFA",
        "PatnerIdWemaB" to "1E3D050B-6995-495F-982A-0511114959C8",
        "PartnerIdZenith" to "C936667C-0B02-4A34-80D0-0FC5B525256E",
        "XClientId" to "85522f45-e459-4548-8b20-3a922196c515",
        "XAccessCode" to "a14014e18e2cffc4d74e150ed68a472bd94189db82d374306d5b307dc7620f20",
        "TlvByteWrapperConstant" to "48656c6c6f20576f726c64207772697474656e206f6e20",
        "EncryptionSecreteKey" to "hq+fAexgn7hK2edm",
        "EncryptionSecreteIv" to "0FJk4uPzoWBjXI+z",
        "NetPosMqttPortNumber" to "8883",
    )

    fun doesGabagesExist() {
//        val encryptionHelper: DataEncryptionAndDecryption = DataEncryptionAndDecryptionImpl(
//            UtilityParam.STRING_REQ_CRED_SEC_K,
//            UtilityParam.STRING_REQ_CRED_IV,
//        )
//
//        aa.forEach { key, value ->
//            val encryptedValue = encryptionHelper.encryptData(value)
//            println("$key  =================>     $encryptedValue \n")
//            println()
//        }
    }

    val nibssApiTest = mapOf(
        "RrnUrl" to "https://getrrn.netpluspay.com",
        "TestKeyOne" to "5D25072F04832A2329D93E4F91BA23A2",
        "TestKeyTwo" to "86CBCDE3B0A22354853E04521686863D",
        "EpmsLiveKeyOne" to "E6891F73948F16C4D6E979D68534D0F4",
        "EpmsLiveKeyTwo" to "3D10EF707F98E3543E32B570E9E9AE86",
        "PosvasLiveKeyOne" to "9BF76D3E13ADD67A51549B7C3EB0E3AD",
        "PosvasLiveKeyTwo" to "A4BAEC5E31BFD913919262C7A7A76D52",
        "NibssIp" to "196.6.103.18",
        "NibssPort" to "5016",
        "NibssIp" to "196.6.103.18",
        "NibssPort" to "4016",
        "NibssTestUrl" to "epms.test.netpluspay.com",
        "NibssTestPort" to "6868",
        "NetPlusPayUrl" to "https://device.netpluspay.com/",
        "DefaultTid" to "2057H63U",
        "NibssTestIp" to "196.6.103.10",
        "NibssConnectionTestPort" to "55533",
        "NibssConnectionTestKeyOne" to "5D25072F04832A2329D93E4F91BA23A2",
        "NibssConnectionTestKeyTwo" to "86CBCDE3B0A22354853E04521686863D",
        "NibssConnectionTestTid" to "20398A4C",
    )
}
