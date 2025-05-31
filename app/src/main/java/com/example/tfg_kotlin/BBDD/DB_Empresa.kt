package com.example.tfg_kotlin.BBDD

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [TablaEmpleados::class, TablaSalas::class, TablaSalaReservada::class], version = 1
)
abstract class DB_Empresa : RoomDatabase() {
    abstract fun appDao(): Dao_Empresa
}
