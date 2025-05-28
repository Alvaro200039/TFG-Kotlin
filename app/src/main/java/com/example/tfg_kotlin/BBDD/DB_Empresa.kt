package com.example.tfg_kotlin.BBDD

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [TablaEmpresa::class], version = 1)

abstract class DB_Empresa : RoomDatabase() {
    abstract fun appDao(): Operaciones

    companion object {
        @Volatile
        private var instancia: DB_Empresa? = null

        fun BDMaestra_creacion(context: Context): DB_Empresa {
            return instancia ?: synchronized(this) {
                instancia ?: Room.databaseBuilder(
                    context.applicationContext,
                    DB_Empresa::class.java,
                    "empresas.db"
                ).fallbackToDestructiveMigration(true)
                    .allowMainThreadQueries() // Solo para pruebas, reemplazar por corrutinas en producción
                    .build().also { instancia = it }
            }
        }
    }
}