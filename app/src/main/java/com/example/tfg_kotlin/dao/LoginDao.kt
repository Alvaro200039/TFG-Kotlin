package com.example.tfg_kotlin.dao


import androidx.room.Dao
import androidx.room.Query
import com.example.tfg_kotlin.entities.Empleados
import com.example.tfg_kotlin.entities.Empresa

@Dao
interface LoginDao {

    @Query("SELECT * FROM Empresa WHERE dominio = :dominio")
    fun getEmpresaPorDominioEnEmpresa(dominio: String): Empresa?

    @Query("SELECT * FROM Empleados WHERE correo = :correo AND contrasena = :contrasena")
    fun loginUsuario(correo: String, contrasena: String): Empleados?
}