package com.example.tfg_kotlin.BBDD

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "Salas_creadas")
data class Salas(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val tamano: String,
    val proyector: Boolean,
    val altavoces: Boolean,
    val sillasDisponibles: Int,
    val idJefe: Int // FK → Empleado(id), si esJefe = true
)
