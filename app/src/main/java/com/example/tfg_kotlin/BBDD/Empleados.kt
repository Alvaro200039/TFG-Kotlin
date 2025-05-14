package com.example.tfg_kotlin.BBDD

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Index

@Entity(tableName = "Empleados",
        indices = [Index(value = ["correo"], unique = true)]
)
data class Empleados(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val nombre: String,
    val apellidos: String = "",
    val correo: String,
    val contrasena: String,
    val nif: String = "",
    val esJefe: Boolean
)