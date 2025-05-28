package com.example.tfg_kotlin.BBDD

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "Salas_reservadas")
data class TablaSalaReservada(
    @PrimaryKey(autoGenerate = true) val IDSala: Int = 0,
    val nombreSala: String,    // FK → TablaSalas(Nombre)
    val empleado: String     // FK → TablaEmpleados(Nomobre, Apellidos)
)