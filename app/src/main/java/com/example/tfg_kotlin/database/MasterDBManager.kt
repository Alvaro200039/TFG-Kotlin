package com.example.tfg_kotlin.database

import android.content.Context
import androidx.room.Room

object MasterDBManager {
    @Volatile
    private var instance: BBDDMaestra? = null

    fun getDatabase(context: Context): BBDDMaestra {
        return instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(
                context.applicationContext,
                BBDDMaestra::class.java,
                "master_database"
            ).build().also { instance = it }
        }
    }
}