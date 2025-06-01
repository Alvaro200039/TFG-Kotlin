package com.example.tfg_kotlin.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.example.tfg_kotlin.entities.Empleados
import com.example.tfg_kotlin.entities.Empresa


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