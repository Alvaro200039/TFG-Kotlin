package com.example.tfg_kotlin.repository

import com.example.tfg_kotlin.dao.LoginDao

class LoginRepository (private val loginDao: LoginDao) {

    suspend fun loginUsuario(correo: String, contrasena: String) = loginDao.loginUsuario(correo, contrasena)
}