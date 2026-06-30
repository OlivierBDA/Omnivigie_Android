package com.olivierbda.omnivigie.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.olivierbda.omnivigie.data.local.dao.ArticleDao
import com.olivierbda.omnivigie.data.local.dao.EmailDao
import com.olivierbda.omnivigie.data.local.dao.SettingDao
import com.olivierbda.omnivigie.data.local.entities.ArticleEntity
import com.olivierbda.omnivigie.data.local.entities.EmailEntity
import com.olivierbda.omnivigie.data.local.entities.SettingEntity

@Database(
    entities = [EmailEntity::class, ArticleEntity::class, SettingEntity::class],
    version = 2,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class OmnivigieDatabase : RoomDatabase() {
    abstract fun emailDao(): EmailDao
    abstract fun articleDao(): ArticleDao
    abstract fun settingDao(): SettingDao

    companion object {
        @Volatile
        private var INSTANCE: OmnivigieDatabase? = null

        fun getDatabase(context: Context): OmnivigieDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    OmnivigieDatabase::class.java,
                    "omnivigie_database"
                )
                .fallbackToDestructiveMigration() // Simple for development
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
