package com.devssocial.localodge.utils.helpers

import java.lang.StringBuilder

object PromotionRatingHelper {

    val prices = hashMapOf(
        0 to 0,
        1 to 395,
        2 to 995,
        3 to 1095,
        4 to 2395,
        5 to 9895
    )

    val titles = hashMapOf(
        0 to "Free",
        1 to "Highlighted",
        2 to "Urgent",
        3 to "Featured: 3 days",
        4 to "Featured: 7 days",
        5 to "Featured: 30 days"
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