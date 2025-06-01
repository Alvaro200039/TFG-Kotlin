package com.example.tfg_kotlin.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.example.tfg_kotlin.entities.Empleados
import com.example.tfg_kotlin.entities.Empresa
import com.example.tfg_kotlin.dao.EmpleadoDao
import com.example.tfg_kotlin.dao.EmpresaDao
import com.example.tfg_kotlin.dao.LoginDao
import com.example.tfg_kotlin.dao.RecuperarContrasenaDao

@Database(
    entities = [Empleados::class,  Empresa::class],
    version = 1
)
abstract class BBDD : RoomDatabase() {
    abstract fun empleadoDao(): EmpleadoDao
    abstract fun empresaDao(): EmpresaDao
    abstract fun loginDao(): LoginDao
    abstract fun recuperarContrasenaDao(): RecuperarContrasenaDao
}
