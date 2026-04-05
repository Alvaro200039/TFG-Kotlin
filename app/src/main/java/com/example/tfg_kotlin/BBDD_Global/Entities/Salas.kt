package com.example.tfg_kotlin.BBDD_Global.Entities


data class Salas(
    val id: String? = null,
    var nombre: String = "",
    var tamaño: String = "",
    var x: Float = 0f,
    var y: Float = 0f,
    var ancho: Float = 100f,
    var alto: Float = 100f,
    var extras: List<String> = emptyList()
)
