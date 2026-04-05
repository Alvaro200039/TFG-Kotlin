package com.example.tfg_kotlin.BBDD_Global.Entities

data class Usuario(
    var uid: String = "",
    var email: String = "",
    var nombre: String = "",
    var apellidos: String = "",
    var cif: String = "",
    var esJefe: Boolean = false
)
