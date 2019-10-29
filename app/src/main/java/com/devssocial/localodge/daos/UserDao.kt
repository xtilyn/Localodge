package com.devssocial.localodge.daos

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.devssocial.localodge.models.User
import io.reactivex.Completable
import io.reactivex.Single

@Dao
interface UserDao {

    @Query("SELECT * from user")
    fun getAlphabetizedWords(): Single<List<User>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(user: User): Completable

    @Query("DELETE FROM user")
    suspend fun deleteAll(): Completable

}