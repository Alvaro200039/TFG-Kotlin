package com.example.tfg_kotlin.BBDD

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query


@Dao
interface EmpleadoDao {

    @Insert
    fun insertarEmpleado(empleado: Empleados)

    @Query("SELECT * FROM Empleados WHERE correo = :correo")
    fun buscarEmpleadoPorCorreo(correo: String): Empleados?

    @Query("SELECT * FROM Empresa WHERE dominio = :dominio")
    fun getEmpresaPorDominioEnEmpresa(dominio: String): Empresa?

    @Query("SELECT * FROM Empresa WHERE cif = :cif")
    fun getEmpresaPorCIF(cif: String): Empresa?
}