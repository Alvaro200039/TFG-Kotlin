package com.example.tfg_kotlin.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.tfg_kotlin.dao.EmpleadoDao
import com.example.tfg_kotlin.dao.LoginDao
import com.example.tfg_kotlin.dao.RecuperarContrasenaDao
import com.example.tfg_kotlin.entities.Empleados

@Database(entities = [Empleados::class], version = 1)
abstract class AppDatabase : RoomDatabase() {
    abstract fun empleadoDao(): EmpleadoDao
    abstract fun loginDao(): LoginDao
    abstract fun recuperarContrasenaDao(): RecuperarContrasenaDao

    companion object {
        private val instances: MutableMap<String, AppDatabase> = mutableMapOf()

        fun getInstance(context: Context, nombreEmpresa: String): AppDatabase {
            val nombreLimpio = "empresa_${nombreEmpresa.replace(".", "-")}"
            return instances.getOrPut(nombreLimpio) {
                Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    nombreLimpio
                )
                    .fallbackToDestructiveMigration()
                    .build()
            }
        }
    }
}