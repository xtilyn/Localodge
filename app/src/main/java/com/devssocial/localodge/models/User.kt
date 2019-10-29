package com.devssocial.localodge.models

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.firebase.Timestamp
import com.google.firebase.firestore.ServerTimestamp

@Entity(tableName = "user")
data class User(
    @PrimaryKey @ColumnInfo(name = "userId") var userId: String = "",
    @ColumnInfo(name = "username") var username: String = "",
    @ColumnInfo(name = "profilePicUrl") var profilePicUrl: String = "",
    @ColumnInfo(name = "email") var email: String = "",
    @ServerTimestamp @ColumnInfo(name = "joinedDate") var joinedDate: Timestamp? = null
)