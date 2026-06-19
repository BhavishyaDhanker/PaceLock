package com.example.pacelock.RoomDB

import android.content.Context
import android.provider.CalendarContract
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(
    entities = [RunEntity::class],
    version = 1,
    exportSchema = false
)

@TypeConverters(GeoPointTypeConverter::class)
abstract class RunDatabase: RoomDatabase() {

    abstract fun dao(): RunDAO

    companion object{

        @Volatile
        private var INSTANCE : RunDatabase? = null

        fun getInstance(context: Context) : RunDatabase{
            return INSTANCE ?: synchronized(this){
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    RunDatabase::class.java,
                    "run_database"
                ).build()
                INSTANCE = instance
                return instance
            }
        }
    }
}