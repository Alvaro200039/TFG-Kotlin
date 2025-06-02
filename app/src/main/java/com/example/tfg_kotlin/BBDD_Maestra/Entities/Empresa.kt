package com.example.tfg_kotlin.BBDD_Maestra.Entities

import androidx.room.*

@Entity(tableName = "empresas")
data class Empresa(
    @PrimaryKey()
    val cif: String,

    val dominio: String,

    val nombre: String,

)