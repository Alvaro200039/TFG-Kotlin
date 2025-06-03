package com.example.tfg_kotlin.entities

import androidx.room.*

@Entity(tableName = "reservas")
data class Reserva(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val nombreSala: String,
    val idSala: Int,
    val fechaHora: String,
    val nombreUsuario: String,
    val idusuario: Int,
    val piso: String
)
