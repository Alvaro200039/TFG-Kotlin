package com.example.tfg_kotlin.repository

import com.example.tfg_kotlin.dao.EmpleadoDao
import com.example.tfg_kotlin.entities.Empleados

class EmpleadoRepository (private val empleadoDao: EmpleadoDao
) {
    suspend fun insertarEmpleado(empleado: Empleados) = empleadoDao.insertarEmpleado(empleado)
    suspend fun obtenerEmpleadoPorCorreo(correo: String) = empleadoDao.buscarEmpleadoPorCorreo(correo)
    suspend fun getAllEmpleados() = empleadoDao.getAllEmpleados()
}