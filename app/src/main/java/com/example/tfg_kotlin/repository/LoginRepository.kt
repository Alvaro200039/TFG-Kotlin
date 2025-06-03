package com.example.tfg_kotlin.repository

import com.example.tfg_kotlin.dao.UsuarioDao
import com.example.tfg_kotlin.entities.Usuario

class LoginRepository (private val usuarioDao: UsuarioDao) {


    suspend fun loginUsuario(correo: String, contrasena: String): Usuario? {
        return usuarioDao.obtenerPorCorreo(correo)?.takeIf { it.contrasena == contrasena }
    }
}