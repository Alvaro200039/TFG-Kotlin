package com.example.tfg_kotlin.data.model

data class Empresa(
    var cif: String = "",
    var dominio: String = "",
    var nombre: String = "",
    var apertura: String = "08:00",
    var cierre: String = "20:00",
    var diasApertura: List<Int> = listOf(1, 2, 3, 4, 5), // L, M, X, J, V
    var diasBloqueados: List<String> = emptyList(), // "dd/MM/yyyy"
    var extrasSalas: List<String> = listOf("WiFi", "Proyector", "Pizarra"),
    var extrasPuestos: List<String> = listOf("Monitor Dual", "Teclado", "Ratón")
)
