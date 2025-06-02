package com.example.tfg_kotlin.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.tfg_kotlin.dao.EmpleadoDao
import com.example.tfg_kotlin.dao.LoginDao
import com.example.tfg_kotlin.entities.Empleados

@Database(entities = [Empleados::class], version = 1)
abstract class AppDatabase : RoomDatabase() {
    abstract fun empleadoDao(): EmpleadoDao
    abstract fun loginDao(): LoginDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context, dbName: String): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    dbName
                )
                    .fallbackToDestructiveMigration()
                    .allowMainThreadQueries()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}