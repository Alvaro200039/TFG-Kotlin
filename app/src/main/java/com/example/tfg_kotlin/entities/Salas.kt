package com.example.tfg_kotlin.entities

import androidx.room.*


@Entity(
    tableName = "salas",
    foreignKeys = [
        ForeignKey(
            entity = Piso::class,
            parentColumns = ["id"],
            childColumns = ["pisoId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class Salas(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    var nombre: String,
    var tamaño: String,
    var pisoId: Int,
    var x: Float,
    var y: Float,
    var ancho: Float,
    var alto: Float,
    var extras: List<String>
)
