package com.devssocial.localodge.models

import com.google.firebase.Timestamp
import com.google.firebase.firestore.ServerTimestamp

data class Post (
    var posterUserId: String = "",
    var objectID: String = "",
    var postDescription: String = "",
    var photoUrl: String? = null,
    var videoUrl: String? = null,
    @ServerTimestamp var createdDate: Timestamp? = null,
    var _geoloc: Location = Location(),
    var rating: Int = 0 // range: [0,5]
)