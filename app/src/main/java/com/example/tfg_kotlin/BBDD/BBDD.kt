package com.example.tfg_kotlin.BBDD

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [Empleados::class,  Empresa::class],
    version = 1
)
abstract class BBDD : RoomDatabase() {
    abstract fun appDao(): Operaciones
}


// Para conectar a la bases de datos en las activity debemos pegar el siguiente código

// Antes del Override fun OnCreate
//  lateinit var database: Operaciones

// dentro del OnCreate
/*
database = Room.databaseBuilder(
            applicationContext, BBDD::class.java, "reservas_db"
            ).allowMainThreadQueries().build().appDao()
*/
