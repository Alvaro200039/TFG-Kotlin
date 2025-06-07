package com.example.tfg_kotlin.BBDD_Global.Entities

data class Usuario(
    var id: Int = 0,
    var email: String = "",
    var nombre: String = "",
    var apellidos: String = "",
    var contrasena: String = "",
    var cif: String = "",
    var esJefe: Boolean = false
){
    constructor() : this(0,"","","","", "", false)
}


