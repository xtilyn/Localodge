package com.devssocial.localodge.utils

import android.content.Context
import android.text.format.DateUtils
import org.ocpsoft.prettytime.PrettyTime
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

    fun convertMessageTimestamp(timestamp: Long): String {
        return PrettyTime().format(Date(timestamp))
    }

}