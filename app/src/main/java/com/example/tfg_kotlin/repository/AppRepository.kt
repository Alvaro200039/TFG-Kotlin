package com.example.tfg_kotlin.repository

import com.example.tfg_kotlin.dao.EmpleadoDao
import com.example.tfg_kotlin.dao.LoginDao
import com.example.tfg_kotlin.entities.Empleados

class AppRepository (private val empleadoDao: EmpleadoDao,
                     private val loginDao: LoginDao
) {
    suspend fun insertarEmpleado(empleado: Empleados) =
        empleadoDao.insertarEmpleado(empleado)

    suspend fun obtenerEmpleadoPorCorreo(correo: String): Empleados? =
        empleadoDao.buscarEmpleadoPorCorreo(correo)

    suspend fun getAllEmpleados(): List<Empleados> =
        empleadoDao.getAllEmpleados()

    fun loginUsuario(correo: String, contrasena: String): Empleados? {
        return loginDao.loginUsuario(correo, contrasena)
    }
}