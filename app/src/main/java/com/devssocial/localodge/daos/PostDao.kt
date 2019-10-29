package com.devssocial.localodge.daos

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.devssocial.localodge.models.Post
import io.reactivex.Completable
import io.reactivex.Single

@Dao
interface PostDao {

    @Query("SELECT * from posts")
    fun getAlphabetizedWords(): Single<List<Post>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(post: Post): Completable

    @Query("DELETE FROM posts")
    suspend fun deleteAll(): Completable

}