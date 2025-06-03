package com.example.tfg_kotlin.repository

import com.example.tfg_kotlin.dao.UsuarioDao

class RecuperarContrasenaRepository (private val usuarioDao: UsuarioDao) {
    suspend fun buscarPorCorreo(correo: String) = usuarioDao.obtenerPorCorreo(correo)

    suspend fun actualizarContrasena(correo: String, nueva: String) {
        usuarioDao.actualizarContrasena(correo, nueva)
    }
}