package com.example.tfg_kotlin.relations

import androidx.room.Embedded
import androidx.room.Relation
import com.example.tfg_kotlin.BBDD_Global.Entities.Piso
import com.example.tfg_kotlin.BBDD_Global.Entities.Salas

data class PisoConSalas(
    @Embedded val piso: Piso,
    @Relation(
        parentColumn = "id",
        entityColumn = "pisoId"
    )
    val salas: List<Salas>
)
