package com.example.tfg_kotlin.entities

import androidx.room.*

@Entity(tableName = "empresas")
data class Empresa(
    @PrimaryKey(autoGenerate = true)
    val id: Int,   // id es PK aquí

    val cif: String,

    val nombre: String,

    val creadorId: Int
)