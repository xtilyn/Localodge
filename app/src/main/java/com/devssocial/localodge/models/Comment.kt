package com.devssocial.localodge.models

import com.google.firebase.Timestamp
import com.google.firebase.firestore.ServerTimestamp

data class Comment(
    var objectID: String = "",
    var postedBy: String = "",
    var body: String = "",
    var photoUrl: String? = null,
    @ServerTimestamp var timestamp: Timestamp? = null
)

data class CommentViewItem(
    var objectID: String = "",
    var postedBy: String = "",
    var photoUrl: String? = null,
    @ServerTimestamp var timestamp: Timestamp? = null,
    var body: String = "",

    // adapter fields
    var postedByProfilePic: String = "",
    var postedByUsername: String = "",
    var isCommentTooLong: Boolean = false,
    var isExpanded: Boolean = false
)