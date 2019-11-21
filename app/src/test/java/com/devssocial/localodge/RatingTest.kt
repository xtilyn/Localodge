package com.devssocial.localodge

import com.devssocial.localodge.data_objects.Rating
import org.junit.Test

class RatingTest {

    @Test
    fun shouldFormatAsCurrencyWith2Decimals() {
        val result = Rating.getPriceInFormattedString(221231400)
        println(result)
        assert(result == "2 212 314.00")
    }

}