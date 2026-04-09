package com.example.tfg_kotlin.data.model

data class Sala(
    var id: String? = null,
    var idPiso: String = "",
    var nombre: String = "",
    var x: Float = 0f,
    var y: Float = 0f,
    var ancho: Float = 100f,
    var alto: Float = 100f,
    var tamano: String = "Mediana",
    var extras: List<String> = emptyList(),
    var tipo: String = "SALA", // "SALA" o "PUESTO"
    var rotacion: Float = 0f,
    var vertices: MutableList<Vertex> = mutableListOf() // Relativos a (x,y)
)
