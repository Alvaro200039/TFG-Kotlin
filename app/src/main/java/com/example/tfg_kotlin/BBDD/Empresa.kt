package com.example.tfg_kotlin.BBDD

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "Empresa")
data class Empresa(
    @PrimaryKey val cif: String,
    val nombre: String,
    val dominio: String
)
