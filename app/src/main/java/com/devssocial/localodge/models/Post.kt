package com.devssocial.localodge.models

import com.google.firebase.Timestamp
import com.google.firebase.firestore.ServerTimestamp

data class Post(
    var posterUserId: String = "",
    var objectID: String = "",
    var postDescription: String = "",
    var photoUrl: String? = null,
    var videoUrl: String? = null,
    @ServerTimestamp var timestamp: Timestamp? = null,
    var _geoloc: Location = Location(),
    var rating: Int = 0, // range: [0,5]
    var likes: HashMap<String, Boolean> = hashMapOf()
)

data class PostViewItem(
    var posterUserId: String = "",
    var objectID: String = "",
    var postDescription: String = "",
    var photoUrl: String? = null,
    var videoUrl: String? = null,
    @ServerTimestamp var timestamp: Timestamp? = null,
    var _geoloc: Location = Location(),
    var rating: Int = 0, // range: [0,5]
    var likes: HashMap<String, Boolean> = hashMapOf(),

    // adapter fields
    var posterUsername: String = "",
    var posterProfilePic: String = "",
    var commentsCount: Long = 0
)