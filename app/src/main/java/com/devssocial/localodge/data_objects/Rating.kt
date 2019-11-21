package com.devssocial.localodge.data_objects

import java.lang.Math.floor
import java.lang.StringBuilder
import kotlin.math.floor

object Rating {

    val prices = hashMapOf(
        0 to 0,
        1 to 99,
        2 to 285,
        3 to 525,
        4 to 899
    )

    fun getPriceInFormattedString(rating: Int): String {
        var count = 0
        val builder = StringBuilder()
        var lastDig = rating % 10
        builder.append(lastDig)
        var temp = rating / 10
        builder.insert(0, ".${temp % 10}")
        temp /= 10
        while (temp > 0) {
            if (count == 3) {
                builder.insert(0, " ")
                count = 0
            }
            lastDig = temp % 10
            builder.insert(0, lastDig)
            count++
            temp /= 10
        }
        return builder.toString()
    }
}