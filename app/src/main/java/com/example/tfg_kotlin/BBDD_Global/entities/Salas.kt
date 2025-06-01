package com.example.tfg_kotlin.BBDD_Global.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

@Entity(tableName = "salas",
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
    val nombre: String,
    val tamano: String,
    val pisoId : String,
    var x: Float,
    var y: Float,
    var ancho: Float,
    var alto: Float,
   // var extras: List<String>
)
