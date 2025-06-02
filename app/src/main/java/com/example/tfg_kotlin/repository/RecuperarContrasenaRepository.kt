package com.example.tfg_kotlin.repository

import com.example.tfg_kotlin.dao.RecuperarContrasenaDao

class RecuperarContrasenaRepository (private val RecuperarPassDao: RecuperarContrasenaDao) {
    suspend fun buscarPorCorreo(correo: String) = RecuperarPassDao.buscarEmpleadoPorCorreo(correo)
    suspend fun actualizarContrasena(correo: String, nueva: String) = RecuperarPassDao.actualizarContrasena(correo, nueva)
}