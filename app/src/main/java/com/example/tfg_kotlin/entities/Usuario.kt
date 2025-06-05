package com.example.tfg_kotlin.entities


data class Usuario(
    var id: String= "",
    var email: String = "",
    var nombre: String = "",
    var apellidos: String = "",
    var contrasena: String = "",
    var cif: String = "",
    var esJefe: Boolean = false
){
    constructor() : this("","","","","", "", false)
}
