package com.example.tfg_kotlin.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "franjas_horarias")
data class FranjaHoraria(
    @PrimaryKey val hora: String // por ejemplo "08:30"
)
