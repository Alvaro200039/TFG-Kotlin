package com.example.tfg_kotlin.database

import android.content.Context
import androidx.room.Room

object GlobalDBManager {
    private val instances = mutableMapOf<String, GlobalDB>()

    fun getDatabase(context: Context, dbName: String): GlobalDB {
        return instances[dbName] ?: synchronized(this) {
            instances[dbName] ?: Room.databaseBuilder(
                context.applicationContext,
                GlobalDB::class.java,
                "$dbName.db"
            ).build().also { instances[dbName] = it }
        }
    }
}
