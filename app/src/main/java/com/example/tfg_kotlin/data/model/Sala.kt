package com.example.tfg_kotlin.data.model

data class Sala(
    var id: String? = null,
    var idPiso: String? = null,
    var nombre: String = "",
    var tamano: String = "",
    var x: Float = 0f,
    var y: Float = 0f,
    var ancho: Float = 100f,
    var alto: Float = 100f,
    var extras: List<String> = emptyList()
)
