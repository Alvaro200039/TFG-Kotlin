package com.example.tfg_kotlin.BBDD_Master.Dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.example.tfg_kotlin.BBDD_Master.entities.Empresa

@Dao
interface Dao_Maestra {
    @Insert
    suspend fun insertarEmpresa(empresa: Empresa)

    @Query("SELECT * FROM Empresas WHERE dominio = :dominio")
    suspend fun buscarPorDominio(dominio: String): Empresa?

    @Query("SELECT * FROM Empresas WHERE cif = :cif")
    suspend fun buscarPorCif(cif: String): Empresa?

    @Query("SELECT * FROM Empresas")
    suspend fun obtenerTodas(): List<Empresa>

}