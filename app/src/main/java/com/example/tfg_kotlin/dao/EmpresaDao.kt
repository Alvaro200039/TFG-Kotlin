package com.example.tfg_kotlin.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.example.tfg_kotlin.entities.Empresa

@Dao
interface EmpresaDao {

    @Insert
    fun insertarEmpresa(empresa: Empresa)

    @Query("SELECT * FROM Empresa WHERE dominio = :dominio")
    fun getEmpresaPorDominioEnEmpresa(dominio: String): Empresa?
}
