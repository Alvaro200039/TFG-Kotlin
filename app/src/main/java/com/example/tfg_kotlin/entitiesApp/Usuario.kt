package com.example.tfg_kotlin.entitiesApp

import androidx.room.*

@Entity(tableName = "usuarios")
data class Usuario(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,

    val nombre: String,
    val email: String,
    val contrasena: String,
    val esJefe: Boolean = false
)

