package com.example.tfg_kotlin.BBDD_Global.Entities


object Sesion {
    var datos: UsuarioSesion? = null

    fun cerrarSesion() {
        datos = null
    }

    fun estaIniciada(): Boolean = datos != null
}
