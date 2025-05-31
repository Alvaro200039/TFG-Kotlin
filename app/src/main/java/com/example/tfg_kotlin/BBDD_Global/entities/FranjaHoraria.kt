package com.example.tfg_kotlin.BBDD_Global.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "franjas_horarias")
data class FranjaHoraria(
    @PrimaryKey val hora: String // por ejemplo "08:30"
)
