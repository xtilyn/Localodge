package com.devssocial.localodge.models

import com.google.firebase.Timestamp
import com.google.firebase.firestore.ServerTimestamp

data class User(
    var userId: String = "",
    var username: String = "",
    var profilePicUrl: String = "",
    var email: String = "",
    @ServerTimestamp var joinedDate: Timestamp? = null,
    var suspendedTillDate: Long = 0,
    var strikesCount: Int = 0 // maximum of 3 before user is banned permanently
)