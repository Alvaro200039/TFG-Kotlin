package com.example.tfg_kotlin.BBDD

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "Empleados")
data class TablaEmpleados(
    @PrimaryKey val correo: String,
    val nombre: String,
    val apellidos: String,
    val contrasena: String,
    val cif: String,
    val esJefe: Boolean
)