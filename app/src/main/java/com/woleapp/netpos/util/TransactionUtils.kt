package com.woleapp.netpos.util

import android.content.Context
import android.view.LayoutInflater
import android.widget.Toast
import com.danbamitale.epmslib.entities.TransactionResponse
import com.danbamitale.epmslib.entities.TransactionType
import com.danbamitale.epmslib.entities.responseMessage
import com.danbamitale.epmslib.utils.IsoAccountType
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.gson.JsonObject
import com.woleapp.netpos.R
import com.woleapp.netpos.databinding.LayoutPayWithTransferBinding
import com.woleapp.netpos.model.GetPayByTransferUserAccount
import com.woleapp.netpos.model.GetPayByTransferUserAccountModel
import com.woleapp.netpos.model.MerchantCategory
import com.woleapp.netpos.model.MerchantCategoryList
import timber.log.Timber

fun gatewayErrorTransactionResponse(
    amount: Long = 0,
    transactionType: TransactionType = TransactionType.PURCHASE,
    accountType: IsoAccountType = IsoAccountType.DEFAULT_UNSPECIFIED
) = TransactionResponse().apply {
    this.transactionType = transactionType
    this.responseCode = "A5"
    this.RRN = "000000000000"
    this.STAN = "000000"
    this.AID = ""
    this.TSI = ""
    this.TVR = ""
    this.responseMessage
    this.accountType = accountType
    this.transactionTimeInMillis = System.currentTimeMillis()
    this.acquiringInstCode = ""
    this.amount = amount
}

private const val bankString = "[\n" +
    "    { \"id\": \"1\", \"name\": \"Access Bank\" ,\"code\":\"044\" },\n" +
    "    { \"id\": \"2\", \"name\": \"Citibank\",\"code\":\"023\" },\n" +
    "    { \"id\": \"3\", \"name\": \"Diamond Bank\",\"code\":\"063\" },\n" +
    "    { \"id\": \"5\", \"name\": \"Ecobank Nigeria\",\"code\":\"050\" },\n" +
    "    { \"id\": \"6\", \"name\": \"Fidelity Bank Nigeria\",\"code\":\"070\" },\n" +
    "    { \"id\": \"7\", \"name\": \"First Bank of Nigeria\",\"code\":\"011\" },\n" +
    "    { \"id\": \"8\", \"name\": \"First City Monument Bank\",\"code\":\"214\" },\n" +
    "    { \"id\": \"9\", \"name\": \"Guaranty Trust Bank\",\"code\":\"058\" },\n" +
    "    { \"id\": \"10\", \"name\": \"Heritage Bank Plc\",\"code\":\"030\" },\n" +
    "    { \"id\": \"11\", \"name\": \"Jaiz Bank\",\"code\":\"301\" },\n" +
    "    { \"id\": \"12\", \"name\": \"Keystone Bank Limited\",\"code\":\"082\" },\n" +
    "    { \"id\": \"13\", \"name\": \"Providus Bank Plc\",\"code\":\"101\" },\n" +
    "    { \"id\": \"14\", \"name\": \"Polaris Bank\",\"code\":\"076\" },\n" +
    "    { \"id\": \"15\", \"name\": \"Stanbic IBTC Bank Nigeria Limited\",\"code\":\"221\" },\n" +
    "    { \"id\": \"16\", \"name\": \"Standard Chartered Bank\",\"code\":\"068\" },\n" +
    "    { \"id\": \"17\", \"name\": \"Sterling Bank\",\"code\":\"232\" },\n" +
    "    { \"id\": \"18\", \"name\": \"Suntrust Bank Nigeria Limited\",\"code\":\"100\" },\n" +
    "    { \"id\": \"19\", \"name\": \"Union Bank of Nigeria\",\"code\":\"032\" },\n" +
    "    { \"id\": \"20\", \"name\": \"United Bank for Africa\",\"code\":\"033\" },\n" +
    "    { \"id\": \"21\", \"name\": \"Unity Bank Plc\",\"code\":\"215\" },\n" +
    "    { \"id\": \"22\", \"name\": \"Wema Bank\",\"code\":\"035\" },\n" +
    "    { \"id\": \"23\", \"name\": \"Zenith Bank\",\"code\":\"057\" }\n" +
    "]"

data class Bank(val id: String, val name: String, val code: String)

var banks: List<Bank> =
    Singletons.gson.fromJson(
        bankString,
        Array<Bank>::class.java
    ).asList()

data class StateWithZip(val state: String, val postal: String)

fun JsonObject.getListOfStateWithZips(): List<StateWithZip> {
    val statesWithZip = arrayListOf<StateWithZip>()
    try {
        this.entrySet().forEach { item ->
            if (item.value.isJsonObject) {
                item.value.asJsonObject.entrySet().forEach { subItem ->
                    statesWithZip.add(
                        StateWithZip(
                            "${item.key} - ${subItem.key}",
                            subItem.value.asString
                        )
                    )
                }
                return@forEach
            }
            statesWithZip.add(StateWithZip(item.key, item.value.asString))
        }
    } catch (e: Exception) {
        Timber.e(e.localizedMessage)
    }
    return statesWithZip
}

fun JsonObject.toMerchantCategoryList(): MerchantCategoryList {
    val list = arrayListOf<MerchantCategory>()
    this.entrySet().forEach {
        list.add(MerchantCategory(it.value.asString, it.key))
    }
    return MerchantCategoryList(list)
}

fun showPayWithTransferDialog(context: Context, userAccount: GetPayByTransferUserAccountModel) {
    val bankDetailsBinding: LayoutPayWithTransferBinding = LayoutPayWithTransferBinding.inflate(
        LayoutInflater.from(context),
        null,
        false
    )
    val bottomSheetDialog = BottomSheetDialog(context, R.style.SheetDialog)
    bottomSheetDialog.setCancelable(false)
    bottomSheetDialog.setContentView(bankDetailsBinding.root)
    bankDetailsBinding.accountNumber.text = userAccount.acctNumber
    bankDetailsBinding.bank.text =
        context.getString(R.string.zenith_bank) // This is hardcoded at the moment
    // because bankname is not returned from the zenith pay by transfer service at the moment
    bankDetailsBinding.accountName.text = userAccount.businessName
    bankDetailsBinding.accountNumber.setOnClickListener {
        copyTextToClipboard(
            context,
            "Account Number",
            userAccount.acctNumber
        )
        Toast.makeText(
            context,
            "Account number copied to clipboard",
            Toast.LENGTH_SHORT
        )
            .show()
    }

    bankDetailsBinding.btnDone.setOnClickListener {
        if (bottomSheetDialog.isShowing) bottomSheetDialog.cancel()
    }
    bottomSheetDialog.show()
}
