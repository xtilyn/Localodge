package com.devssocial.localodge.room_models

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.devssocial.localodge.converters.RoomConverter

@TypeConverters(RoomConverter::class)
@Entity(tableName = "user")
data class UserRoom(
    @PrimaryKey @ColumnInfo(name = "userId") var userId: String = "",
    @ColumnInfo(name = "username") var username: String = "",
    @ColumnInfo(name = "profilePicUrl") var profilePicUrl: String = "",
    @ColumnInfo(name = "email") var email: String = "",
    @ColumnInfo(name = "joinedDate") var joinedDate: Long? = null,
    @ColumnInfo(name = "blocking") var blocking: Set<String> = hashSetOf(),
    @ColumnInfo(name = "blockedPosts") var blockedPosts: Set<String> = hashSetOf()
)