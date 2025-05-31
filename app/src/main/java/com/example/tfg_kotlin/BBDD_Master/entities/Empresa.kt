package com.example.tfg_kotlin.BBDD_Master.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "Empresas")
data class Empresa(
    @PrimaryKey val cif: String,        // Ej: "A12345678"
    val nombre: String,                 // Ej: "CocaCola"
    val dominio: String                 // Ej: "@cocacola.com"
)

