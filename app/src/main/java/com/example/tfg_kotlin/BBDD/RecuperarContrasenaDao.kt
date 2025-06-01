package com.example.tfg_kotlin.BBDD

import androidx.room.Dao
import androidx.room.Query

@Dao
interface RecuperarContrasenaDao {

    @Query("SELECT * FROM Empleados WHERE correo = :correo")
    fun buscarEmpleadoPorCorreo(correo: String): Empleados?

    @Query("UPDATE Empleados SET contrasena = :nuevaContrasena WHERE correo = :correo")
    fun actualizarContrasena(correo: String, nuevaContrasena: String)
}