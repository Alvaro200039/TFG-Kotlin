package com.example.tfg_kotlin.BBDD

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.sql.Time
import java.util.Date

@Entity(tableName = "Salas_creadas")
data class TablaSalas(
    @PrimaryKey val nombre: String,
    val piso : String,
    val tamano: String,
    val proyector: Boolean,
    val wifi: Boolean,
    val pizarra: Boolean,
    val fecha: Date,
    val horario: Time
)
