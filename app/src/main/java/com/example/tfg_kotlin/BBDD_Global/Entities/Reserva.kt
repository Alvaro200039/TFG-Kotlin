package com.example.tfg_kotlin.BBDD_Global.Entities


data class Reserva(
    var id: String? = null,
    val nombreSala: String = "",
    val idSala: String = "",       // si es entero en Room, aquí puedes dejar String y luego convertir
    val fechaHora: String = "",
    val nombreUsuario: String = "",
    val idusuario: String = "",    // Firebase UID es String
    val piso: String = ""
)
