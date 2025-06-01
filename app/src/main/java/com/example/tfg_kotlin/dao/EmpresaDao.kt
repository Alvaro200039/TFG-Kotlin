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

    @Query("SELECT * FROM Empresa WHERE cif = :cif LIMIT 1")
    fun getEmpresaPorCif(cif: String): Empresa?

}
