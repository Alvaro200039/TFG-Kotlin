package com.example.tfg_kotlin.BBDD.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "Empresas")
data class TablaEmpresa(
    @PrimaryKey val cif: String,        // Ej: "A12345678"
    val nombre: String,                 // Ej: "CocaCola"
    val dominio: String                 // Ej: "@cocacola.com"
)

