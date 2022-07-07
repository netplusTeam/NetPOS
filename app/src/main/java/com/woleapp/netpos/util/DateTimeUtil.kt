package com.woleapp.netpos.util

import android.annotation.SuppressLint
import java.text.SimpleDateFormat
import java.util.* // ktlint-disable no-wildcard-imports

object DateTimeUtil {

    @SuppressLint("SimpleDateFormat")
    fun getDateFromMilliseconds(milliSecondsString: Long): String {
        val formatter = SimpleDateFormat("yyyy-MM-dd'T'HH:mm")

        // Create a calendar object that will convert the date and time value in milliseconds to date.
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = milliSecondsString
        return formatter.format(calendar.time)
    }
}
