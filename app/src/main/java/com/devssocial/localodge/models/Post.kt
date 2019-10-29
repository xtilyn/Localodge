package com.devssocial.localodge.models

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.firebase.Timestamp
import com.google.firebase.firestore.ServerTimestamp

@Entity(tableName = "posts")
data class Post(
    @ColumnInfo(name = "posterUserId") var posterUserId: String = "",
    @PrimaryKey @ColumnInfo(name = "objectID") var objectID: String = "",
    @ColumnInfo(name = "postDescription") var postDescription: String = "",
    @ColumnInfo(name = "photoUrl") var photoUrl: String? = null,
    @ColumnInfo(name = "videoUrl") var videoUrl: String? = null,
    @ServerTimestamp @ColumnInfo(name = "createdDate") var createdDate: Timestamp? = null,
    @ColumnInfo(name = "_geoloc") var _geoloc: Location = Location(),
    @ColumnInfo(name = "rating") var rating: Int = 0, // range: [0,5]
    @ColumnInfo(name = "likes") var likes: Map<String, Boolean> = hashMapOf()
)

data class PostViewItem(
    var posterUserId: String = "",
    var objectID: String = "",
    var postDescription: String = "",
    var photoUrl: String? = null,
    var videoUrl: String? = null,
    @ServerTimestamp var createdDate: Timestamp? = null,
    var _geoloc: Location = Location(),
    var rating: Int = 0, // range: [0,5]
    var likes: HashSet<String> = hashSetOf(),

    // adapter fields
    var posterUsername: String = "",
    var posterProfilePic: String = "",
    var comments: Set<String> = hashSetOf()
)