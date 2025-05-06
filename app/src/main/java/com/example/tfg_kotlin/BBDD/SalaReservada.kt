package com.example.tfg_kotlin.BBDD

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "Salas_reservadas")
data class SalaReservada(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val idPersona: Int, // FK → Empleado(id)
    val idSala: Int     // FK → SalaCreada(id)
)