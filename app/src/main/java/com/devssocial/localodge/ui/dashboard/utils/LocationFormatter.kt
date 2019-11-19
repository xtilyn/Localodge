package com.devssocial.localodge.ui.dashboard.utils

import java.math.BigDecimal
import java.math.RoundingMode
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

object LocationFormatter {

    fun distFrom(lat1: Float, lng1: Float, lat2: Float?, lng2: Float?): String {
        if (lat2 == null || lng2 == null) return ""
        val earthRadius = 6371.0 //km
        val dLat = Math.toRadians((lat2 - lat1).toDouble())
        val dLng = Math.toRadians((lng2 - lng1).toDouble())
        val a = sin(dLat / 2) * sin(dLat / 2) + cos(Math.toRadians(lat1.toDouble())) * cos(Math.toRadians(lat2.toDouble())) *
                sin(dLng / 2) * sin(dLng / 2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))

        return BigDecimal(earthRadius * c).setScale(2, RoundingMode.HALF_EVEN).toString()
    }

}