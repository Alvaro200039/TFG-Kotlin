package com.example.tfg_kotlin.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

@Entity(tableName = "pisos",
    foreignKeys = [
        ForeignKey(
            entity = Empresa::class,
            parentColumns = ["id"],
            childColumns = ["empresaId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class Piso(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,

    val nombre: String,
    val uriFondo: String,

    val empresaId: Int
)
