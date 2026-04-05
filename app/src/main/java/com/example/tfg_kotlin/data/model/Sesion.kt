package com.example.tfg_kotlin.data.model

object Sesion {
    var datos: UserSession? = null

    fun cerrarSesion() {
        datos = null
    }
}
