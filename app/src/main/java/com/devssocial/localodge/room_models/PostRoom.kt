package com.devssocial.localodge.room_models

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.devssocial.localodge.converters.CollectionConverter

@TypeConverters(CollectionConverter::class)
@Entity(tableName = "posts")
data class PostRoom(
    @ColumnInfo(name = "posterUserId") var posterUserId: String = "",
    @PrimaryKey @ColumnInfo(name = "objectID") var objectID: String = "",
    @ColumnInfo(name = "postDescription") var postDescription: String = "",
    @ColumnInfo(name = "photoUrl") var photoUrl: String? = null,
    @ColumnInfo(name = "videoUrl") var videoUrl: String? = null,
    @ColumnInfo(name = "createdDate") var createdDate: Long? = null,
    @ColumnInfo(name = "lat") var lat: Long? = null,
    @ColumnInfo(name = "lng") var lng: Long? = null,
    @ColumnInfo(name = "rating") var rating: Int = 0, // range: [0,5]
    @ColumnInfo(name = "likes") var likes: Set<String> = hashSetOf()
)