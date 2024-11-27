package com.woleapp.netpos.util

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Context
import android.os.Build
import android.provider.Settings
import android.telephony.TelephonyManager
import android.text.Html
import android.text.Spanned
import android.util.Base64
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LiveData
import com.danbamitale.epmslib.entities.TransactionRequestData
import com.danbamitale.epmslib.entities.TransactionResponse
import com.danbamitale.epmslib.utils.IsoTimeManager
import com.google.android.material.snackbar.Snackbar
import com.woleapp.netpos.BuildConfig
import com.woleapp.netpos.R
import com.woleapp.netpos.model.Alerter.showToast
import com.woleapp.netpos.model.MakePaymentParams
import com.woleapp.netpos.model.MerchantDetailsResponse
import com.woleapp.netpos.model.TransactionResponseX
import com.woleapp.netpos.model.TransactionToLogBeforeConnectingToNibbs
import com.woleapp.netpos.ui.fragments.dialog.LoadingDialog
import com.woleapp.netpos.util.Singletons.getCurrentlyLoggedInUser
import com.woleapp.netpos.util.UtilityParams.STRING_MERCHANT_ID
import com.woleapp.netpos.util.resourceWrapper.Resource
import com.woleapp.netpos.util.resourceWrapper.Status
import io.reactivex.Scheduler
import io.reactivex.Single
import io.reactivex.disposables.CompositeDisposable
import timber.log.Timber
import java.text.*
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.*

object RandomNumUtil {
    @SuppressLint("SimpleDateFormat")
    fun getDateInMillis(dateTime: String): Long {
        val format = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'.000Z'")
        val date = format.parse(dateTime)
        return (date!!.time) + 3600000
    }

    fun Number.formatAmount(): String {
        return DecimalFormat("#.##.00").format(this)
    }

    fun Number.formatCurrencyAmountUsingCurrentModule(currencySymbol: String = "\u20A6"): String {
        val format = DecimalFormat("#,###.00")
        return "$currencySymbol${format.format(this)}"
    }

    @SuppressLint("SimpleDateFormat")
    fun getDateInTheFormatExpectedByTheNewService(date: String): String {
        val initDate = SimpleDateFormat("dd:MM:yyyy hh:mm:ss").parse(date) ?: Date()
        val formatter = SimpleDateFormat("yyyy-MM-dd")
        return formatter.format(initDate) + " 00:00:00"
    }

    @SuppressLint("SimpleDateFormat")
    fun getDateInTheFormatExpectedByTheNewServiceForEnd(date: String): String {
        val initDate = SimpleDateFormat("dd:MM:yyyy hh:mm:ss").parse(date) ?: Date()
        val formatter = SimpleDateFormat("yyyy-MM-dd")
        return formatter.format(initDate) + " 23:59:59"
    }

    @SuppressLint("SimpleDateFormat")
    fun getDateInMillis3(dateTime: String): Long {
        val format = SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
        val date = format.parse(dateTime)
        return date!!.time + 3600000
    }

    @SuppressLint("SimpleDateFormat")
    fun getDateInMillis2(dateTime: String): Long {
        val formatter: DateTimeFormatter =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
        val localDate: LocalDateTime = LocalDateTime.parse(dateTime, formatter)
        return localDate.atOffset(ZoneOffset.UTC).toInstant().toEpochMilli() + 3600000
    }

    fun dateStr2Long(dateStr: String): Long {
        return try {
            val c = Calendar.getInstance()
            c.time =
                SimpleDateFormat("yyyy-MM-dd hh:mm")
                    .parse(dateStr)!!
            c.timeInMillis
        } catch (e: ParseException) {
            e.printStackTrace()
            0
        }
    }

    @SuppressLint("SimpleDateFormat")
    fun dateStrToLong(
        dateStr: String,
        inputDateFormat: String,
    ): Long {
        val formattedDateString = dateStr.removeSuffix(dateStr.takeLast(3))
        return try {
            val c = Calendar.getInstance()
            c.time =
                SimpleDateFormat(inputDateFormat)
                    .parse(dateStr)!!
            c.timeInMillis
        } catch (e: ParseException) {
            e.printStackTrace()
            0
        }
    }

    @SuppressLint("SimpleDateFormat")
    fun getStartOfDayAsString(): String {
        val dateFormatter: DateFormat = SimpleDateFormat("yyyy-MM-dd")
        val today = Date()
        return dateFormatter.format(today) + " 00:00:00"
    }

    fun getDateInMillisFromZenithPbtTransDate(dateTime: String): Long {
        val format = SimpleDateFormat("yyyy-MM-dd hh:mm:ss")
        val date = format.parse(dateTime.replace("T", " ").removeSuffix(".000Z"))
        return date!!.time
    }

    fun getDateFromZenithPbtTransDate(dateTime: String): String {
        val format = SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
        return dateTime.replace("T", " ").removeSuffix(".000Z")
    }

    fun formatHtml(htmlText: String): Spanned? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            Html.fromHtml(htmlText, Html.FROM_HTML_MODE_COMPACT)
        } else {
            Html.fromHtml(htmlText)
        }
    }

    @SuppressLint("SimpleDateFormat")
    fun getDateInMilliSecsForLocal(date: String): Long {
        val dateFormatter = SimpleDateFormat("dd:MM:yyyy HH:mm:ss")
        val newDate = date.split(" ")[0] + " 00:00:00"
        Timber.d("DIS_TIME%s", newDate.toString())
        return dateFormatter.parse(newDate)!!.time
    }

    @SuppressLint("SimpleDateFormat")
    fun getDateInMilliSecsForLocalForEndOfDay(date: String): Long {
        val dateFormatter = SimpleDateFormat("dd:MM:yyyy HH:mm:ss")
        val newDate = date.split(" ")[0] + " 23:59:59"
        Timber.d("DIS_TIME_end%s", newDate)
        return dateFormatter.parse(newDate)!!.time
    }

    @SuppressLint("SimpleDateFormat")
    fun getEndOfDayAsString(): String {
        val dateFormatter: DateFormat = SimpleDateFormat("yyyy-MM-dd")
        val today = Date()
        return dateFormatter.format(today) + " 23:59:59"
    }

    fun generateRandomRrn(length: Int): String {
        val random = Random()
        var digits = ""
        digits += (random.nextInt(9) + 1).toString()
        for (i in 1 until length) {
            digits += (random.nextInt(10) + 0).toString()
        }
        return digits
    }

    val formattedTime =
        SimpleDateFormat("hh:mm:ss", Locale.getDefault()).format(System.currentTimeMillis())
            .format(Date())

    @SuppressLint("SimpleDateFormat")
    fun getDate(): String {
        val dateFormatter: DateFormat = SimpleDateFormat("ddMM")
        val today = Date()
        return dateFormatter.format(today)
    }

    @SuppressLint("SimpleDateFormat")
    fun getCurrentDateTime(): String {
        val dateFormatter: DateFormat = SimpleDateFormat("yyyy-MM-dd hh:mm:ss")
        val today = Date()
        return dateFormatter.format(today)
    }

    @SuppressLint("ConstantLocale")
    fun getLocaleCurrentDateTime() =
        SimpleDateFormat(
            "yyyy-MM-dd hh:mm a",
            Locale.getDefault(),
        ).format(System.currentTimeMillis())
            .format(Date())

    @SuppressLint("SimpleDateFormat")
    fun getCurrentDate(): String {
        val dateFormatter: DateFormat = SimpleDateFormat("yyyy-MM-dd")
        val today = Date()
        return dateFormatter.format(today)
    }

    fun mapTransactionResponse(transResp: TransactionResponse) =
        "CardHolder: ${transResp.cardHolder} " +
            "\nCardType: ${transResp.cardLabel}" +
            "\nAmount: ${transResp.amount} " +
            "\nRRN: ${transResp.RRN} \n" +
            "ResponseCode: ${transResp.responseCode} \n" +
            "TransmissionDateTime: ${transResp.transmissionDateTime} \n" +
            "Time in Millis: ${transResp.transactionTimeInMillis}"

    @SuppressLint("SimpleDateFormat")
    fun getDate(milliSeconds: Long): String {
        val formatter = SimpleDateFormat("yyyy-MM-dd hh:mm:ss")

        val calendar = Calendar.getInstance()
        calendar.timeInMillis = milliSeconds
        return formatter.format(calendar.time)
    }

    @SuppressLint("SimpleDateFormat")
    fun convertDateToStringFromMillis(
        milliSeconds: Long,
        format: String = "yyyy-MM-dd",
    ): String {
        val formatter = SimpleDateFormat(format)

        val calendar = Calendar.getInstance()
        calendar.timeInMillis = milliSeconds
        return formatter.format(calendar.time)
    }

    fun mapDanbamitaleResponseToResponseX(input: TransactionResponse): TransactionResponseX {
        with(input) {
            return TransactionResponseX(
                AID = AID,
                rrn = RRN,
                STAN = STAN,
                TSI = TSI,
                TVR = TVR,
                accountType = accountType.name,
                acquiringInstCode = acquiringInstCode,
                additionalAmount_54 = additionalAmount_54,
                amount = amount.toInt(),
                appCryptogram = appCryptogram,
                authCode = authCode,
                cardExpiry = cardExpiry,
                cardHolder = cardHolder,
                cardLabel = cardLabel,
                id = id.toInt(),
                localDate_13 = localDate_13,
                localTime_12 = localTime_12,
                maskedPan = maskedPan,
                merchantId = merchantId,
                originalForwardingInstCode = originalForwardingInstCode,
                otherAmount = otherAmount.toInt(),
                otherId = otherId,
                responseCode = responseCode,
                responseDE55 = responseDE55 ?: "",
                terminalId = terminalId,
                transactionTimeInMillis = transactionTimeInMillis,
                transactionType = transactionType.name,
                transmissionDateTime = getCurrentDateTime(),
                agentName = Singletons.getCurrentlyLoggedInUser()?.email!!,
            )
        }
    }

    fun getCustomRrn() = IsoTimeManager().fullDate.substring(2, 14)

    fun MakePaymentParams.getTransactionResponseToLog(
        cardScheme: String,
        requestData: TransactionRequestData,
        customerName: String,
        terminal_id: String,
        partnerId: String,
    ) = this.cardData.expiryDate.let {
        TransactionToLogBeforeConnectingToNibbs(
            status = "PENDING",
            TransactionResponseX(
                AID = "",
                rrn = getCustomRrn(),
                STAN = generateRandomRrn(6),
                TSI = "",
                TVR = "",
                accountType = accountType.name,
                acquiringInstCode = "",
                additionalAmount_54 = "",
                amount = amount.toInt(),
                appCryptogram = "",
                authCode = "",
                cardExpiry = it,
                cardHolder = customerName,
                cardLabel = cardScheme.toString(),
                id = 0,
                localDate_13 = getDate(),
                localTime_12 = formattedTime.replace(":", ""),
                maskedPan = cardData.pan,
                merchantId = partnerId,
                originalForwardingInstCode = "",
                otherAmount = requestData.otherAmount.toInt(),
                otherId = "",
                responseCode = "99",
                responseDE55 = "",
                terminalId = terminal_id,
                transactionTimeInMillis = 0,
                transactionType = requestData.transactionType.name,
                transmissionDateTime = getCurrentDateTime(),
                agentName = Singletons.getCurrentlyLoggedInUser()?.email!!,
            ),
        )
    }

    fun setField59(aid: String): String {
        val additionalTagManipulation =
            "<?xml version=\"1.0\" encoding=\"UTF-8\"?><AdditionalEmvTags><EmvTag><TagId>84</TagId><TagValue>$aid</TagValue></EmvTag></AdditionalEmvTags>"
        val len = additionalTagManipulation.length.toString()
        val lengthOfLength = len.length

        return "127.22:216MPOS_DEVICE_TYPE111217AdditionalEmvTags${lengthOfLength}${additionalTagManipulation.length}$additionalTagManipulation"
    }

    fun getDeviceId(context: Context): String {
        val deviceId: String =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)
            } else {
                val mTelephony = context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
                if (mTelephony.deviceId != null) {
                    mTelephony.deviceId
                } else {
                    Settings.Secure.getString(
                        context.contentResolver,
                        Settings.Secure.ANDROID_ID,
                    )
                }
            }
        return deviceId
    }

    private val bankList =
        mapOf(
            "konga" to "konga",
            "easypay" to "fcmb",
            "wemacashout" to "wemacashout",
            "tingopay" to "tingopay",
            "providus" to "providus",
            "heritage" to "heritage",
            "wema" to "wemabank",
            "wemampgs" to "wemabank",
            "zenith" to "zenith",
            "unitybank" to "unitybank",
            "aellacredit" to "aellacredit",
            "netpos" to "netpos",
        )

    fun getBankName(): String? = bankList[BuildConfig.FLAVOR]

    fun initPartnerId(): String {
        var partnerID = ""
        val bankList =
            mapOf(
                "firstbank" to "7FD43DF1-633F-4250-8C6F-B49DBB9650EA",
                "easypay" to "1B0E68FD-7676-4F2C-883D-3931C3564190",
                "stanbic" to "377F47E9-55F9-45E0-B77A-1BAA4BC88026",
                "providus" to "8B26F328-040F-4F27-A5BC-4414AB9D1EFA",
                "providus" to "8B26F328-040F-4F27-A5BC-4414AB9D1EFA",
                "wema" to "1E3D050B-6995-495F-982A-0511114959C8",
                "wemampgs" to "1E3D050B-6995-495F-982A-0511114959C8",
                "zenith" to "C936667C-0B02-4A34-80D0-0FC5B525256E",
                "tingopay" to "1EED19E0-9625-49AA-A0CF-2EFCD8F30036",
            )

        for (element in bankList) {
            if (element.key == BuildConfig.FLAVOR) {
                partnerID = element.value
            }
        }
        return partnerID
    }

    fun displayCurrency(): ArrayList<String> {
        return arrayListOf(
            "EUR",
            "USD",
            "NGN",
            "RUB",
            "QAR",
            "ZAR",
            "AED",
            "AUD",
            "CAD",
            "CDF",
            "CHF",
            "CNY",
            "MAD",
            "GBP",
            "GHS",
            "GMD",
            "HKD",
            "JPY",
        )
    }

    fun getNetPlusPayMid(): String = getCurrentlyLoggedInUser()?.netplusPayMid ?: STRING_MERCHANT_ID

    fun <T> observeServerResponseActivity(
        context: Context,
        lifecycle: LifecycleOwner,
        serverResponse: LiveData<Resource<T>>,
        loadingDialog: LoadingDialog,
        fragmentManager: FragmentManager,
        successAction: () -> Unit,
    ) {
        serverResponse.observe(lifecycle) {
            when (it.status) {
                Status.SUCCESS -> {
                    loadingDialog.dismiss()
                    if (it.data is MerchantDetailsResponse) {
                        successAction()
                    } else {
                        Toast.makeText(context, R.string.an_error_occurred, Toast.LENGTH_SHORT)
                            .show()
                    }
                }
                Status.LOADING -> {
                    loadingDialog.show(
                        fragmentManager,
                        STRING_LOADING_DIALOG_TAG,
                    )
                }
                Status.ERROR -> {
                    loadingDialog.dismiss()
                    Toast.makeText(context, R.string.an_error_occurred, Toast.LENGTH_SHORT).show()
                }
                Status.TIMEOUT -> {
                    loadingDialog.dismiss()
                    Toast.makeText(context, R.string.timeOut, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    fun <T> Fragment.observeServerResponse(
        serverResponse: Single<Resource<T>>,
        loadingDialog: AlertDialog,
        compositeDisposable: CompositeDisposable,
        ioScheduler: Scheduler,
        mainThreadSchedulers: Scheduler,
        successAction: () -> Unit,
    ) {
        compositeDisposable.add(
            serverResponse.subscribeOn(ioScheduler).observeOn(mainThreadSchedulers)
                .subscribe { data, error ->
                    data?.let {
                        when (it.status) {
                            Status.SUCCESS -> {
                                Log.d("NOWJUSTCHECKING", it.data.toString())
                                loadingDialog.dismiss()
                                if (
                                    it.data is String
                                ) {
                                    successAction()
                                } else {
                                    Log.d("JUSTCHECKING", it.toString())
//                                    showSnackBar(
//                                        this.requireView(),
//                                        getString(R.string.an_error_occurred),
//                                    )
                                }
                            }
                            Status.LOADING -> {
                                loadingDialog.show()
                            }
                            Status.ERROR -> {
                                loadingDialog.cancel()
                                loadingDialog.dismiss()
                                if (it.data is String) {
                                    showToast(it.data)
                                } else {
                                    showToast("An error occurred, please try again")
                                }
                            }
                            Status.TIMEOUT -> {
                                loadingDialog.cancel()
                                loadingDialog.dismiss()
                                showSnackBar(this.requireView(), getString(R.string.timeOut))
                            }
                            else -> {}
                        }
                    }
                    error?.let {
                        loadingDialog.cancel()
                        loadingDialog.dismiss()
                        showSnackBar(this.requireView(), getString(R.string.an_error_occurred))
                    }
                },
        )
    }

    fun <T> Fragment.observeServerResponse(
        serverResponse: LiveData<Resource<T>?>,
        loadingDialog: AlertDialog,
        fragmentManager: FragmentManager,
        successAction: () -> Unit,
    ) {
        serverResponse.observe(this.viewLifecycleOwner) {
            when (it?.status) {
                Status.SUCCESS -> {
                    loadingDialog.dismiss()
                    successAction()
                }
                Status.LOADING -> {
                    //  Log.d("LOADING", "LOADINGRESULT")
                    loadingDialog.show()
                }
                Status.ERROR -> {
                    loadingDialog.cancel()
                    loadingDialog.dismiss()
                }
                Status.TIMEOUT -> {
                    loadingDialog.cancel()
                    loadingDialog.dismiss()
                }
            }
        }
    }

    fun alertDialog(context: Context): AlertDialog {
        val dialogView: View =
            LayoutInflater.from(context)
                .inflate(R.layout.layout_loading_dialog, null)
        val dialogBuilder: AlertDialog.Builder = AlertDialog.Builder(context)
        dialogBuilder.setCancelable(false)
        dialogBuilder.setView(dialogView)

        return dialogBuilder.create()
    }

    fun alertDialog(
        context: Context,
        setCancelable: Boolean,
    ): AlertDialog {
        val dialogView: View =
            LayoutInflater.from(context).inflate(R.layout.layout_loading_dialog, null)
        val dialogBuilder: AlertDialog.Builder = AlertDialog.Builder(context)
        dialogBuilder.setCancelable(setCancelable)
        dialogBuilder.setView(dialogView)

        return dialogBuilder.create()
    }

    fun LifecycleOwner.showSnackBar(
        rootView: View,
        message: String,
    ) {
        Snackbar.make(rootView, message, Snackbar.LENGTH_LONG).show()
    }

    fun formatAmountToNaira(amount: Double): String {
        val format = NumberFormat.getCurrencyInstance(Locale("en", "NG"))
        return format.format(amount)
    }

    fun Fragment.showToast(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
    }

    fun getGUID() = UUID.randomUUID().toString().replace("-", "")

    fun createClientDataForNonVerveCard(
        transID: String,
        cardNumber: String,
        expiryDate: String,
        cvv: String,
    ): String = "$transID:LIVE:$cardNumber:$expiryDate:$cvv::NGN:QR"

    fun stringToBase64(text: String): String {
        val data: ByteArray = text.toByteArray()
        return Base64.encodeToString(data, Base64.DEFAULT)
    }

    fun Number.formatCurrency(currencySymbol: String = "\u20A6"): String {
        val format = DecimalFormat("#,###.00")
        return "$currencySymbol${format.format(this)}"
    }
}
