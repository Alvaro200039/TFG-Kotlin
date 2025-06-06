package com.example.tfg_kotlin.BBDD_Global.Entities

data class UsuarioSesion(
    val empresa: Empresa,
    val usuario: Usuario,
    val pisos: List<Piso> = listOf(),
    val franjasHorarias: List<FranjaHoraria> = listOf()
)