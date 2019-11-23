package com.devssocial.localodge.models

import com.google.firebase.Timestamp
import com.google.firebase.firestore.ServerTimestamp

data class User(
    var userId: String = "",
    var username: String = "",
    var profilePicUrl: String = "",
    var email: String = "",
    @ServerTimestamp var joinedDate: Timestamp? = null
)