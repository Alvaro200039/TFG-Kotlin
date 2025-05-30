package com.example.tfg_kotlin.BBDD

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [TablaEmpresa::class], version = 1)
abstract class DB_Maestra : RoomDatabase() {
    abstract fun userDao(): Dao_Maestra
}