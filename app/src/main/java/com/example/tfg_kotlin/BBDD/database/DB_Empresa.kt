package com.example.tfg_kotlin.BBDD.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.example.tfg_kotlin.BBDD.Dao.Dao_Empresa
import com.example.tfg_kotlin.BBDD.entities.TablaEmpleados
import com.example.tfg_kotlin.BBDD.entities.TablaSalaReservada
import com.example.tfg_kotlin.BBDD.entities.TablaSalas

@Database(
    entities = [TablaEmpleados::class, TablaSalas::class, TablaSalaReservada::class], version = 1
)
abstract class DB_Empresa : RoomDatabase() {
    abstract fun appDao(): Dao_Empresa
}
