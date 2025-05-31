package com.example.tfg_kotlin.BBDD_Global.relations

import androidx.room.Embedded
import androidx.room.Relation
import com.example.tfg_kotlin.BBDD_Global.entities.Piso
import com.example.tfg_kotlin.BBDD_Global.entities.Salas

data class PisoSalas(
    @Embedded val piso: Piso,
    @Relation(
        parentColumn = "id",
        entityColumn = "pisoId"
    )
    val salas: List<Salas>
)