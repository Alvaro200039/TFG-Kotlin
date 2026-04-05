package com.example.tfg_kotlin.data.model

data class Usuario(
    var uid: String = "",
    var email: String = "",
    var nombre: String = "",
    var apellidos: String = "",
    var cif: String = "",
    var esJefe: Boolean = false
)
