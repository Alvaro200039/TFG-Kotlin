package com.example.tfg_kotlin.entities

import androidx.room.*

@Entity(
    tableName = "reservas",
    foreignKeys = [
        ForeignKey(
            entity = Salas::class,
            parentColumns = ["id"],
            childColumns = ["idSala"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class Reserva(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val nombreSala: String,
    val idSala: Int,                 // 🔑 Enlace real con la sala
    val fechaHora: String,
    val nombreUsuario: String,
    val idusuario: Int,
    val piso: String
)