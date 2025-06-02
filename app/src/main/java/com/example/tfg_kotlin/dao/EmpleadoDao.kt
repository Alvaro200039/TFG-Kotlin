package com.example.tfg_kotlin.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.example.tfg_kotlin.entities.Empleados
import com.example.tfg_kotlin.entities.Empresa


@Dao
interface EmpleadoDao {

    @Insert
    suspend fun insertarEmpleado(empleado: Empleados)

    @Query("SELECT * FROM Empleados WHERE correo = :correo")
    suspend fun buscarEmpleadoPorCorreo(correo: String): Empleados?

    @Query("SELECT * FROM Empresa WHERE dominio = :dominio")
    suspend fun getEmpresaPorDominioEnEmpresa(dominio: String): Empresa?


    @Query("SELECT * FROM Empleados")
    suspend fun getAllEmpleados(): List<Empleados>

}