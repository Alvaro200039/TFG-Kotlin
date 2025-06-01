package com.example.tfg_kotlin.BBDD


import androidx.room.Dao
import androidx.room.Query

@Dao
interface LoginDao {

    @Query("SELECT * FROM Empresa WHERE dominio = :dominio")
    fun getEmpresaPorDominioEnEmpresa(dominio: String): Empresa?

    @Query("SELECT * FROM Empleados WHERE correo = :correo AND contrasena = :contrasena")
    fun loginUsuario(correo: String, contrasena: String): Empleados?
}