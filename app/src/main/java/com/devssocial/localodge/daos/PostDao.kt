package com.devssocial.localodge.daos

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.devssocial.localodge.models.Post
import com.devssocial.localodge.room_models.PostRoom
import io.reactivex.Completable
import io.reactivex.Single

@Dao
interface PostDao {

    @Query("SELECT * from posts")
    fun getPosts(): Single<List<PostRoom>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertAll(posts: List<PostRoom>): Completable

    @Query("DELETE FROM posts")
    fun deleteAll(): Completable

}