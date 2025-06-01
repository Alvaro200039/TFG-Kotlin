package com.example.tfg_kotlin.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.tfg_kotlin.daoMaster.EmpresaDao
import com.example.tfg_kotlin.entitiesMaster.Empresa

@Database(
    entities = [Empresa::class],
    version = 1,
    exportSchema = false
)
abstract class MasterDatabase : RoomDatabase() {
    abstract fun empresaDao(): EmpresaDao

    companion object {
        @Volatile
        private var INSTANCE: MasterDatabase? = null

        fun getDatabase(context: Context): MasterDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    MasterDatabase::class.java,
                    "master_database"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}