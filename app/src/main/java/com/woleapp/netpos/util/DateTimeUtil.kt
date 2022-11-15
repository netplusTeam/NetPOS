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

    fun getCurrentDateTimeAsFormattedString(): String {
        val formattedTime =
            SimpleDateFormat(
                "yyyy-MM-dd hh:mm a",
                Locale.getDefault()
            ).format(System.currentTimeMillis())
                .format(Date())

        return formattedTime.replace(
            formattedTime.takeLast(3),
            "_${formattedTime.takeLast(3).trim()}"
        ).replace(":", "_")
            .replace("-", "_").replace(" ", "_at_")
    }
}
