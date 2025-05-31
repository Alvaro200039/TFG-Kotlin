package com.example.tfg_kotlin.BBDD_Master.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.example.tfg_kotlin.BBDD_Master.Dao.Dao_Maestra
import com.example.tfg_kotlin.BBDD_Master.entities.Empresa

@Database(entities = [Empresa::class], version = 1)
abstract class DB_Maestra : RoomDatabase() {
    abstract fun userDao(): Dao_Maestra
}