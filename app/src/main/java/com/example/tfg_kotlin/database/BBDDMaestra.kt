package com.example.tfg_kotlin.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.tfg_kotlin.entities.Empleados
import com.example.tfg_kotlin.entities.Empresa
import com.example.tfg_kotlin.dao.EmpresaDao

@Database(
    entities = [ Empresa::class],
    version = 1
)
abstract class BBDDMaestra : RoomDatabase() {
    abstract fun empresaDao(): EmpresaDao


    companion object {
        @Volatile
        private var INSTANCE: BBDDMaestra? = null

        fun getInstance(context: Context): BBDDMaestra {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    BBDDMaestra::class.java,
                    "maestra_db"
                )
                    .fallbackToDestructiveMigration() // opcional si quieres resetear si hay error de migración
                    .allowMainThreadQueries() // opcional, solo para pruebas
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
