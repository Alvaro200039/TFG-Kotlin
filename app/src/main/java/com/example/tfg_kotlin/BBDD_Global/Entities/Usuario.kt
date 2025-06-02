package com.example.tfg_kotlin.BBDD_Global.Entities

import androidx.room.*

@Entity(tableName = "usuarios")
data class Usuario(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val email: String,
    val nombre: String,
    val apellidos: String,
    val contrasena: String,
    val cif: String,
    val esJefe: Boolean
)

