package com.woleapp.netpos.util

import android.os.Build
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import androidx.databinding.BindingAdapter
import com.danbamitale.epmslib.extensions.formatCurrencyAmount
import com.woleapp.netpos.R

@BindingAdapter("processButtonState")
fun Button.processButtonState(transactionState: Int) {
    when (transactionState) {
        STATE_PAYMENT_STAND_BY -> {
            text = context.getString(R.string.proceess)
            isEnabled = true
        }
        STATE_PAYMENT_STARTED -> {
            text = context.getString(R.string.processing_transaction)
            isEnabled = false
        }
        STATE_PAYMENT_APPROVED -> {
            text = context.getString(R.string.payment_approved)
            isEnabled = false
        }
    }
}

@BindingAdapter("quickCashOutButtonState")
fun Button.processQuickCashOutButtonState(transactionState: Int) {
    when (transactionState) {
        STATE_PAYMENT_STAND_BY -> {
            text = context.getString(R.string.process_transaction)
            isEnabled = true
        }
        STATE_PAYMENT_STARTED -> {
            text = context.getString(R.string.processing_transaction)
            isEnabled = false
        }
        STATE_PAYMENT_APPROVED -> {
            text = context.getString(R.string.payment_approved)
            isEnabled = false
        }
    }
}

//@BindingAdapter("setPrintButtonLabelText")
//fun Button.setPrintButtonLabelText(dormantInput: String) {
//    text = if (Build.MODEL.equals("Pro", true) || Build.MODEL.equals("P3", true)) {
//        resources.getString(R.string.print)
//    } else {
//        resources.getString(R.string.download_or_share)
//    }
//}

@BindingAdapter("setPrintButtonLabelText")
fun Button.setPrintButtonLabelText(dormantInput: String?) {

    val isProDevice = Build.MODEL.equals("Pro", true) ||
            Build.MODEL.equals("P3", true) ||
            Build.MODEL.contains("K11", ignoreCase = true)

    val isForcedPrint = dormantInput == "PRINT_ONLY"

    text = when {
        isForcedPrint -> resources.getString(R.string.print)
        isProDevice -> resources.getString(R.string.print)
        else -> resources.getString(R.string.download_or_share)
    }
}

@BindingAdapter("paymentProgress")
fun ProgressBar.paymentProgress(transactionState: Int) {
    visibility = if (transactionState == STATE_PAYMENT_STAND_BY) {
        View.GONE
    } else View.VISIBLE
}

@BindingAdapter("formatAmount")
fun EditText.formatAmount(amount: Long) {
    setText(amount.div(100).formatCurrencyAmount("\u20A6"))
}

@BindingAdapter("buttonInProgress")
fun Button.buttonInProgress(inpProgress: Boolean) {
    isEnabled = inpProgress.not()
}

@BindingAdapter("progressBarInProgress")
fun ProgressBar.progressBarInProgress(boolean: Boolean) {
    visibility = if (boolean) {
        View.VISIBLE
    } else View.GONE
}

@BindingAdapter("widgetVisibility")
fun View.widgetVisibility(boolean: Boolean) {
    visibility = if (boolean) {
        View.VISIBLE
    } else View.GONE
}
