package com.example.tfg_kotlin.BBDD_Global.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.example.tfg_kotlin.BBDD_Global.Dao.Dao_Empresa
import com.example.tfg_kotlin.BBDD_Global.Dao.Dao_FranjaHoraria
import com.example.tfg_kotlin.BBDD_Global.Dao.Dao_Piso
import com.example.tfg_kotlin.BBDD_Global.Dao.Dao_Reserva
import com.example.tfg_kotlin.BBDD_Global.Dao.Dao_Salas
import com.example.tfg_kotlin.BBDD_Global.entities.Empleados
import com.example.tfg_kotlin.BBDD_Global.entities.FranjaHoraria
import com.example.tfg_kotlin.BBDD_Global.entities.Piso
import com.example.tfg_kotlin.BBDD_Global.entities.Reserva
import com.example.tfg_kotlin.BBDD_Global.entities.Salas

@Database(
    entities = [Empleados::class, FranjaHoraria::class, Piso::class, Reserva::class, Salas::class], version = 1
)
abstract class DB_Empresa : RoomDatabase() {
    abstract fun empresaDao(): Dao_Empresa
    abstract fun salaDao(): Dao_Salas
    abstract fun reservaDao(): Dao_Reserva
    abstract fun franjahorariaDao(): Dao_FranjaHoraria
    abstract fun pisoDao(): Dao_Piso
}
