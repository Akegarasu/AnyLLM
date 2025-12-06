package com.anzu.anyllm.data.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.anzu.anyllm.data.entity.MessageEntity
import com.anzu.anyllm.data.entity.ProfileEntity
import com.anzu.anyllm.data.entity.SessionEntity

@Database(
    entities = [
        ProfileEntity::class,
        SessionEntity::class,
        MessageEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun profileDao(): ProfileDao
    abstract fun sessionDao(): SessionDao
    abstract fun messageDao(): MessageDao

    companion object {
        const val DATABASE_NAME = "anyllm_database"
    }
}

