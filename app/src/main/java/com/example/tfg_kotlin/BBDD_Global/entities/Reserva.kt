package com.example.tfg_kotlin.BBDD_Global.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "Salas_reservadas")
data class Reserva(
    @PrimaryKey(autoGenerate = true) val IDSala: Int = 0,
    val nombreSala: String,
    val fechaHora: String,
    val nombreUsuario: String,
    val IdUsuario:Int,
    val piso: String
)