package com.example.tfg_kotlin.BBDD_Global.Entities


data class Piso(
    var id: String? = null,           // Para guardar el ID del documento Firestore (opcional)
    var nombre: String = "",
    var empresaCif: String = "",
    var imagenUrl: String? = null     // URL de la imagen almacenada en Firebase Storage
)

