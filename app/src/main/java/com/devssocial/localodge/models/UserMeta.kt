package com.devssocial.localodge.models

data class Meta (
    var suspendedTillDate: Long = 0,
    var strikesCount: Int = 0 // maximum of 3 before user is banned permanently
)

data class CustomerInfo (
    var address: String = "",
    var cardType: String = "",
    var customerId: String = "",
    var expDate: String = "",
    var las4Digits: String = "",
    var postal: String = "",
    var sourceId: String = ""
)