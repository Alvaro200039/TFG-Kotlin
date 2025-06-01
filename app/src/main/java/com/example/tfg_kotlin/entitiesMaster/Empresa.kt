package com.example.tfg_kotlin.entitiesMaster

import androidx.room.*

@Entity(tableName = "empresas")
data class Empresa(
    @PrimaryKey()
    val cif: String,

    val dominio: String,

    val nombre: String,

)