package com.woleapp.netpos.network

import org.simpleframework.xml.Element
import org.simpleframework.xml.Path
import org.simpleframework.xml.Root

@Root(strict = false, name = "transferRequest")
class TransferRequest {

    @field:Path("terminalInformation")
    @field:Element(name = "batteryInformation", required = false)
    var batteryInformation = 0

    @field:Path("terminalInformation")
    @field:Element(name = "currencyCode", required = false)
    var currencyCode = ""

    @field:Path("terminalInformation")
    @field:Element(name = "languageInfo", required = false)
    var languageInfo = ""

    @field:Path("terminalInformation")
    @field:Element(name = "merchantId", required = false)
    var merchantId = ""

    @field:Path("terminalInformation")
    @field:Element(name = "merhcantLocation")
    var merchantLocation = ""

    @field:Path("terminalInformation")
    @field:Element(name = "posConditionCode")
    var posConditionCode: String? = null

    @field:Path("terminalInformation")
    @field:Element(name = "posDataCode")
    var posDataCode: String? = null

    @field:Path("terminalInformation")
    @field:Element(name = "posEntryMode")
    var posEntryMode: String? = null

    @field:Path("terminalInformation")
    @field:Element(name = "posGeoCode")
    var posGeoCode: String? = null

    @field:Path("terminalInformation")
    @field:Element(name = "printerStatus")
    var printerStatus = 0

    @field:Path("terminalInformation")
    @field:Element(name = "terminalId")
    var terminalId = ""

    @field:Path("terminalInformation")
    @field:Element(name = "terminalType")
    var terminalType = ""

    @field:Path("terminalInformation")
    @field:Element(name = "transmissionDate")
    var transmissionDate = ""

    @field:Path("terminalInformation")
    @field:Element(name = "uniqueId")
    var uniqueId = ""

    @field:Path("cardData")
    @field:Element(name = "cardSequenceNumber")
    var cardSequenceNumber = "0"

    @field:Path("cardData/emvData")
    @field:Element(name = "AmountAuthorized")
    var amountAuthorized = ""

    @field:Path("cardData/emvData")
    @field:Element(name = "AmountOther")
    var amountOther = ""

    @field:Path("cardData/emvData")
    @field:Element(name = "ApplicationInterchangeProfile")
    var applicationInterchangeProfile = ""

    @field:Path("cardData/emvData")
    @field:Element(name = "atc")
    var atc = ""

    @field:Path("cardData/emvData")
    @field:Element(name = "Cryptogram")
    var cryptogram = ""

    @field:Path("cardData/emvData")
    @field:Element(name = "CryptogramInformationData")
    var cryptogramInformationData = 0

    @field:Path("cardData/emvData")
    @field:Element(name = "CvmResults")
    var cvmResults = "0"

    @field:Path("cardData/emvData")
    @field:Element(name = "iad")
    var iad = ""

    @field:Path("cardData/emvData")
    @field:Element(name = "TransactionCurrencyCode")
    var transactionCurrencyCode = ""

    @field:Path("cardData/emvData")
    @field:Element(name = "TerminalVerificationResult")
    var terminalVerficationResult = ""

    @field:Path("cardData/emvData")
    @field:Element(name = "TerminalCountryCode")
    var terminalCountryCode = ""

    @field:Path("cardData/emvData")
    @field:Element(name = "TerminalType")
    var terminalType2 = ""

    @field:Path("cardData/emvData")
    @field:Element(name = "TerminalCapabilities")
    var terminalCapabilities = ""

    @field:Path("cardData/emvData")
    @field:Element(name = "TransactionDate")
    var transationDate = ""

    @field:Path("cardData/emvData")
    @field:Element(name = "TransactionType")
    var transactionType = ""

    @field:Path("cardData/emvData")
    @field:Element(name = "UnpredictableNumber")
    var unpredictableNumber = ""

    @field:Path("cardData/emvData")
    @field:Element(name = "DedicatedFileName")
    var dedicatedFileName = ""

    @field:Path("cardData/track2")
    @field:Element(name = "pan")
    var pan = ""

    @field:Path("cardData/track2")
    @field:Element(name = "expiryMonth")
    var expiryMonth = ""

    @field:Path("cardData/track2")
    @field:Element(name = "expiryYear")
    var expiryYear = ""

    @field:Path("cardData/track2")
    @field:Element(name = "track2")
    var track2 = ""

    @field:Element(name = "originalTransmissionDateTime")
    var originalTransmissionDate = ""

    @field:Element(name = "stan")
    var stan = "0"

    @field:Element(name = "retrievalReferenceNumber")
    var rrn = "0"

    @field:Element(name = "fromAccount")
    var fromAccount = ""

    @field:Element(name = "toAccount")
    var toAccount = ""

    @field:Element(name = "minorAmount")
    var minorAmount = ""

    @field:Element(name = "receivingInstitutionId")
    var receivingInstitutionId = ""

    @field:Element(name = "surcharge")
    var surcharge = ""

    @field:Path("pinData")
    @field:Element(name = "ksnd")
    var ksnd = ""

    @field:Path("pinData")
    @field:Element(name = "ksn")
    var ksn = ""

    @field:Path("pinData")
    @field:Element(name = "pinType")
    var pinType = ""

    @field:Path("pinData")
    @field:Element(name = "pinBlock")
    var pinBlock = ""

    @field:Element(name = "keyLabel")
    var keyLabel = ""

    @field:Element(name = "destinationAccountNumber")
    var destinationAccountNumber = ""
    var header = ""

    @field:Element(name = "extendedTransactionType")
    var extendedTransactionType = ""
}
