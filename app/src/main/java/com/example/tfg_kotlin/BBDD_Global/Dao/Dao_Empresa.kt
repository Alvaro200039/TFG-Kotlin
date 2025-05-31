package com.example.tfg_kotlin.BBDD_Global.Dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.example.tfg_kotlin.BBDD_Global.entities.Empleados


@Dao
interface Dao_Empresa {

    // Empleados
    @Insert
    suspend fun insertarEmpleado(empleado: Empleados)

    @Query("SELECT * FROM Empleados WHERE nombre = :nombre AND apellidos = :apellidos")
    suspend fun buscarEmpleado(nombre: String, apellidos: String): Empleados?

    @Query("SELECT * FROM Empleados WHERE esJefe = 1")
    suspend fun obtenerJefes(): List<Empleados>

    @Query("SELECT * FROM Empleados WHERE correo = :correo LIMIT 1")
    suspend fun obtenerPorCorreo(correo: String): Empleados?

}
