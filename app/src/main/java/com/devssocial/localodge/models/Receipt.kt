package com.devssocial.localodge.models

data class Receipt(
    var amount: Int, // charge amount
    var date: Long,
    var currency: String,
    var customerId: String,
    var description: String,
    var receiptEmail: String
)