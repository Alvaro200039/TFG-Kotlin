package com.example.tfg_kotlin.BBDD.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "Salas_creadas")
data class TablaSalas(
    @PrimaryKey val nombre: String,
    val pisoId : String,
    val tamano: String,
    val proyector: Boolean,
    val wifi: Boolean,
    val pizarra: Boolean
)
