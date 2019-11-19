package com.devssocial.localodge.daos

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.devssocial.localodge.room_models.UserRoom
import io.reactivex.Completable
import io.reactivex.Single

@Dao
interface UserDao {

    @Query("SELECT * from user WHERE userId = :userId")
    fun getUser(userId: String): Single<UserRoom>

    @Query("UPDATE user SET profilePicUrl = :url WHERE userId = :userId")
    fun updateProfilePic(userId: String, url: String): Completable

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(user: UserRoom): Completable

    @Query("DELETE FROM user")
    fun deleteAll(): Completable

    @Query("UPDATE user SET blockedPosts = :blockedPosts WHERE userId = :userId")
    fun updateBlockedPosts(userId: String, blockedPosts: HashSet<String>): Completable
}