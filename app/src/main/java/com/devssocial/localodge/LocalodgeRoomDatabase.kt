package com.devssocial.localodge

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.devssocial.localodge.daos.UserDao
import com.devssocial.localodge.room_models.UserRoom

@Database(entities = [UserRoom::class], version = 1)
public abstract class LocalodgeRoomDatabase : RoomDatabase() {

    abstract fun userDao(): UserDao

    companion object {
        // Singleton prevents multiple instances of database opening at the
        // same time.
        @Volatile
        private var INSTANCE: LocalodgeRoomDatabase? = null

        fun getDatabase(context: Context): LocalodgeRoomDatabase {
            val tempInstance = INSTANCE
            if (tempInstance != null) {
                return tempInstance
            }
            synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    LocalodgeRoomDatabase::class.java,
                    "localodge_database"
                ).build()
                INSTANCE = instance
                return instance
            }
        }
    }
}