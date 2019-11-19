package com.devssocial.localodge.models

data class Report(
    var reportedByUserId: String,
    var reason: String,
    var description: String
)
