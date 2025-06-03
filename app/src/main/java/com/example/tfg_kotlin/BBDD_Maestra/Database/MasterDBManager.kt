package com.example.tfg_kotlin.BBDD_Maestra.Database

import android.content.Context
import androidx.room.Room

object MasterDBManager {
    @Volatile
    private var instance: MasterDB? = null

    fun getDatabase(context: Context): MasterDB {
        return instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(
                context.applicationContext,
                MasterDB::class.java,
                "master_database"
            ).build().also { instance = it }
        }
    }
}