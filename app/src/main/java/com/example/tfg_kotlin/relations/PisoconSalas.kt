package com.example.tfg_kotlin.relations

import androidx.room.Embedded
import androidx.room.Relation
import com.example.tfg_kotlin.entities.Piso
import com.example.tfg_kotlin.entities.Salas

data class PisoConSalas(
    @Embedded val piso: Piso,
    @Relation(
        parentColumn = "id",
        entityColumn = "pisoId"
    )
    val salas: List<Salas>
)
