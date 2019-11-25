package com.devssocial.localodge.models

data class CardDetails (
    var brand: String,
    var country: String,
    var exp_month: Int,
    var exp_year: Int,
    var last4: String
)