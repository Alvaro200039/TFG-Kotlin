package com.example.tfg_kotlin.data.model

data class UserSession(
    var empresa: Empresa,
    val usuario: Usuario,
    var pisos: List<Piso> = listOf(),
    val franjasHorarias: List<FranjaHoraria> = listOf()
)
