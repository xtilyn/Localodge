package com.devssocial.localodge.utils

import android.content.Context
import android.text.format.DateUtils
import java.text.SimpleDateFormat
import java.util.*

object DateUtils {

    private fun convertTimestampToDateFormat(timestamp: Long): String {
        val dayMonthYear = "yyyy-MM-dd"
        val sdf = SimpleDateFormat(dayMonthYear, Locale.ENGLISH)

        return sdf.format(Date(timestamp))
    }

    private fun convertTimestampToHourFormat(timestamp: Long): String {
        val dayMonthYear = "HH:mm a"
        val sdf = SimpleDateFormat(dayMonthYear, Locale.ENGLISH)

        return sdf.format(Date(timestamp))
    }

    // TODO CONTINUE HERE TEST THIS
    fun convertMessageTimestamp(timestamp: Long): String {
        val timestampCalendar = Calendar.getInstance()
        timestampCalendar.timeInMillis = timestamp
        val nowCalendar = Calendar.getInstance()
        return if (DateUtils.isToday(timestamp)) {
            convertTimestampToHourFormat(timestamp)
        } else if (nowCalendar.get(Calendar.DATE) - timestampCalendar.get(Calendar.DATE) == 1
            &&
            nowCalendar.get(Calendar.MONTH) == timestampCalendar.get(Calendar.MONTH)
            &&
            nowCalendar.get(Calendar.YEAR) == timestampCalendar.get(Calendar.YEAR)
        ) {
            "Yesterday"
        } else if (nowCalendar.get(Calendar.DATE) - timestampCalendar.get(Calendar.DATE) == 2
            &&
            nowCalendar.get(Calendar.MONTH) == timestampCalendar.get(Calendar.MONTH)
            &&
            nowCalendar.get(Calendar.YEAR) == timestampCalendar.get(Calendar.YEAR)
        ) {
            "2 days ago"
        } else if (nowCalendar.get(Calendar.DATE) - timestampCalendar.get(Calendar.DATE) >= 0
            &&
            nowCalendar.get(Calendar.WEEK_OF_MONTH) - timestampCalendar.get(Calendar.WEEK_OF_MONTH) == 1
            &&
            nowCalendar.get(Calendar.MONTH) == timestampCalendar.get(Calendar.MONTH)
            &&
            nowCalendar.get(Calendar.YEAR) == timestampCalendar.get(Calendar.YEAR)
        ) {
            "Last Week"
        } else {
            convertTimestampToDateFormat(timestamp)
        }
    }

}