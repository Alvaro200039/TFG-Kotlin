package com.example.tfg_kotlin.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "Empleados")
data class Empleados(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val nombre: String,
    val apellidos: String = "",
    val correo: String,
    val contrasena: String,
    val cif: String = "",
    val esJefe: Boolean
)