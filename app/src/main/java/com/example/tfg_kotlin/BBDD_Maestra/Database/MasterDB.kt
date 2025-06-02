package com.example.tfg_kotlin.BBDD_Maestra.Database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.tfg_kotlin.BBDD_Maestra.Dao.MasterDao
import com.example.tfg_kotlin.BBDD_Maestra.Entities.Empresa

@Database(
    entities = [Empresa::class], version = 1
)
abstract class MasterDB : RoomDatabase() {
    abstract fun empresaDao(): MasterDao

    companion object {
        @Volatile
        private var INSTANCE: MasterDB? = null

        fun getDatabase(context: Context): MasterDB {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    MasterDB::class.java,
                    "master_database"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}