package com.example.tfg_kotlin.BBDD

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [TablaEmpleados::class, TablaSalas::class, TablaSalaReservada::class],
    version = 1
)
abstract class DB_Global : RoomDatabase() {
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
