package com.example.tfg_kotlin.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "reservas")
data class Reserva(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val nombreSala: String,
    val fechaHora: String,
    val nombreUsuario: String,
    val piso: String
)
