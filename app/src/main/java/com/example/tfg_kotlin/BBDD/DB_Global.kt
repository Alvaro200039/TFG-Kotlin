package com.example.tfg_kotlin.BBDD

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [TablaEmpleados::class, TablaSalas::class, TablaSalaReservada::class],
    version = 1
)
abstract class DB_Global : RoomDatabase() {
    abstract fun appDao(): Operaciones

    companion object {
        fun BDEmpresa_creacion(context: Context, nombreEmpresa: String): DB_Global {
            val dbName = "BD_${nombreEmpresa.replace(" ", "_")}.db"
            return Room.databaseBuilder(
                context.applicationContext,
                DB_Global::class.java,
                dbName
            ).fallbackToDestructiveMigration(true)
                .allowMainThreadQueries()
                .build()
        }
    }
}
