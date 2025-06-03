package com.example.tfg_kotlin.relations

import androidx.room.Embedded
import androidx.room.Relation

data class PisoConSalas(
    @Embedded val piso: Piso,
    @Relation(
        parentColumn = "id",
        entityColumn = "pisoId"
    )
    val salas: List<Salas>
)
