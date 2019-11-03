package com.devssocial.localodge.models

import com.google.firebase.Timestamp
import com.google.firebase.firestore.ServerTimestamp

data class Feedback(
    var userId: String = "",
    var ratingPercent: Float = 0f,
    var comment: String = "",
    @ServerTimestamp var timestamp: Timestamp? = null
)