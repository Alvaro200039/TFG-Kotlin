package com.example.tfg_kotlin.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.example.tfg_kotlin.entities.Empresa

@Dao
interface EmpresaDao {

    @Insert
    suspend fun insertarEmpresa(empresa: Empresa): Long

    @Query("SELECT * FROM Empresa WHERE dominio = :dominio")
    suspend fun getEmpresaPorDominio(dominio: String): Empresa?

    @Query("SELECT * FROM Empresa WHERE cif = :cif LIMIT 1")
    suspend fun getEmpresaPorCif(cif: String): Empresa?

    @Query("SELECT * FROM Empresa")
    suspend fun obtenerTodasLasEmpresas(): List<Empresa> // agregado


}
