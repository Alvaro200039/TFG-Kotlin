package com.example.tfg_kotlin.BBDD_Global.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "Empleados")
data class Empleados(
    @PrimaryKey (autoGenerate = true) val id: Int = 0,
    val correo: String,
    val nombre: String,
    val apellidos: String,
    val contrasena: String,
    val cif: String,
    val esJefe: Boolean
)