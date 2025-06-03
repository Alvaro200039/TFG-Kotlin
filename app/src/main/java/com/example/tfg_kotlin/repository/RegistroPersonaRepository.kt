package com.example.tfg_kotlin.repository

import com.example.tfg_kotlin.dao.UsuarioDao
import com.example.tfg_kotlin.entities.Usuario

class RegistroPersonaRepository (private val usuarioDao: UsuarioDao
) {
    suspend fun insertarUsuario(usuario: Usuario) {
        usuarioDao.insertarUsuario(usuario)
    }

    suspend fun obtenerPorCorreo(correo: String): Usuario? {
        return usuarioDao.obtenerPorCorreo(correo)
    }

    suspend fun getAllUsuarios(): List<Usuario> {
        return usuarioDao.getAllUsuarios()
    }
}