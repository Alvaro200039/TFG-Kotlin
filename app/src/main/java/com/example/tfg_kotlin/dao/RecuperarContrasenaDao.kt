package com.example.tfg_kotlin.dao

import androidx.room.Dao
import androidx.room.Query
import com.example.tfg_kotlin.entities.Empleados

@Dao
interface RecuperarContrasenaDao {

    @Query("SELECT * FROM Empleados WHERE correo = :correo")
    suspend fun buscarEmpleadoPorCorreo(correo: String): Empleados?

    @Query("UPDATE Empleados SET contrasena = :nuevaContrasena WHERE correo = :correo")
    suspend fun actualizarContrasena(correo: String, nuevaContrasena: String)
}