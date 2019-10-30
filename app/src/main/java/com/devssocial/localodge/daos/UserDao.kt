package com.devssocial.localodge.daos

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.devssocial.localodge.models.User
import com.devssocial.localodge.room_models.UserRoom
import io.reactivex.Completable
import io.reactivex.Single

@Dao
interface UserDao {

    @Query("SELECT * from user")
    fun getAlphabetizedWords(): Single<List<UserRoom>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(user: UserRoom): Completable

    @Query("DELETE FROM user")
    fun deleteAll(): Completable

}