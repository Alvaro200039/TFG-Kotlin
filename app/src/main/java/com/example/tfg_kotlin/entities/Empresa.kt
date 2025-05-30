package com.example.tfg_kotlin.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "empresas")
data class Empresa(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,

    val nombre: String,

    val creadorId: Int  // Usuario jefe que creó la empresa
)
