package com.devssocial.localodge.room_models

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.devssocial.localodge.converters.RoomConverter

@TypeConverters(RoomConverter::class)
@Entity(tableName = "posts")
data class PostRoom(
    @ColumnInfo(name = "posterUserId") var posterUserId: String = "",
    @PrimaryKey @ColumnInfo(name = "objectID") var objectID: String = "",
    @ColumnInfo(name = "postDescription") var postDescription: String = "",
    @ColumnInfo(name = "photoUrl") var photoUrl: String? = null,
    @ColumnInfo(name = "videoUrl") var videoUrl: String? = null,
    @ColumnInfo(name = "timestamp") var timestamp: Long? = null,
    @ColumnInfo(name = "lat") var lat: Double? = null,
    @ColumnInfo(name = "lng") var lng: Double? = null,
    @ColumnInfo(name = "rating") var rating: Int = 0, // range: [0,5]
    @ColumnInfo(name = "likes") var likes: Set<String> = hashSetOf(),
    @ColumnInfo(name = "posterUsername") var posterUsername: String = "",
    @ColumnInfo(name = "posterProfilePic") var posterProfilePic: String = "",
    @ColumnInfo(name = "comments") var comments: Set<String> = hashSetOf()
)