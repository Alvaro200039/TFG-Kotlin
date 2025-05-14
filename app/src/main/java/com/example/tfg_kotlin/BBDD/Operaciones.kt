package com.example.tfg_kotlin.BBDD

import androidx.room.*

@Dao
interface Operaciones {

    @Query("SELECT * FROM Empleados WHERE nombre = :nombre AND contrasena = :contrasena")
    fun login(nombre: String, contrasena: String): Empleados?

    // Empleados
    @Insert
    fun insertarEmpleado(empleado: Empleados)

    @Query("SELECT * FROM Empleados WHERE nombre = :nombre AND apellidos = :apellidos")
    fun buscarEmpleado(nombre: String, apellidos: String): Empleados?

    @Query("SELECT * FROM Empleados WHERE esJefe = 1")
    fun obtenerJefes(): List<Empleados>

    @Query("SELECT * FROM Empleados WHERE correo = :correo AND contrasena = :contrasena AND esJefe = 0")
    fun loginEmpleado(correo: String, contrasena: String): Empleados?

    @Query("SELECT * FROM Empleados WHERE nif = :nif AND esJefe = 1")
    fun existeEmpresaConNif(nif: String): Empleados?

    @Query("SELECT * FROM Empleados WHERE correo = :correo AND contrasena = :contrasena AND nif = :nif AND esJefe = 1")
    fun loginJefe(correo: String, contrasena: String, nif: String): Empleados?

    @Query("SELECT * FROM Empleados WHERE correo = :correo")
    fun buscarEmpleadoPorCorreo(correo: String): Empleados?

   
}
