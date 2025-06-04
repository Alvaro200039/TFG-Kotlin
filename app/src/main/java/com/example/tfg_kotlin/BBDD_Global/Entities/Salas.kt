package com.example.tfg_kotlin.BBDD_Global.Entities


data class Salas(
    val id: String? = null,
    var nombre: String,
    var tamaño: String,
    var x: Float,
    var y: Float,
    var ancho: Float,
    var alto: Float,
    var extras: List<String>
)
