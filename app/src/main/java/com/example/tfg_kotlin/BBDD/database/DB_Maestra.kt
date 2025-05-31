package com.example.tfg_kotlin.BBDD.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.example.tfg_kotlin.BBDD.Dao.Dao_Maestra
import com.example.tfg_kotlin.BBDD.entities.TablaEmpresa

@Database(entities = [TablaEmpresa::class], version = 1)
abstract class DB_Maestra : RoomDatabase() {
    abstract fun userDao(): Dao_Maestra
}