package com.example.tfg_kotlin.BBDD_Global.Entities

data class UsuarioSesion(
    var empresa: Empresa,
    val usuario: Usuario,
    var pisos: List<Piso> = listOf(),
    val franjasHorarias: List<FranjaHoraria> = listOf()
)