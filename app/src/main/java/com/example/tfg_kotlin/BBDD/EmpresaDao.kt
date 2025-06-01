package com.example.tfg_kotlin.BBDD

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface EmpresaDao {

    @Insert
    fun insertarEmpresa(empresa: Empresa)

    @Query("SELECT * FROM Empresa WHERE dominio = :dominio")
    fun getEmpresaPorDominioEnEmpresa(dominio: String): Empresa?
}
