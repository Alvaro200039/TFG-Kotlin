package com.example.tfg_kotlin.data.model

data class Reserva(
    var id: String? = null,
    val nombreSala: String = "",
    val idSala: String = "",
    val fechaHora: String = "",
    val nombreUsuario: String = "",
    val idusuario: String = "",
    val piso: String = ""
)
