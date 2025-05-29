package com.example.tfg_kotlin.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "salas")
data class Salas(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,

    val nombre: String,
    val tamaño: String,
    val piso: String,

    val x: Float,
    val y: Float,
    val ancho: Float,
    val alto: Float,

    val extras: List<String>  // <-- sin @TypeConverters aquí
)