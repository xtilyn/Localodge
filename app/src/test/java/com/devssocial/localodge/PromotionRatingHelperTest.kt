package com.devssocial.localodge

import com.devssocial.localodge.utils.helpers.PromotionRatingHelper
import org.junit.Test

class PromotionRatingHelperTest {

    @Test
    fun shouldFormatAsCurrencyWith2Decimals() {
        val result = PromotionRatingHelper.getPriceInFormattedString(221231400)
        println(result)
        assert(result == "2 212 314.00")
    }

}