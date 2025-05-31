package com.example.tfg_kotlin.BBDD.Dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.example.tfg_kotlin.BBDD.entities.TablaEmpresa

@Dao
interface Dao_Maestra {
    @Insert
    suspend fun insertarEmpresa(empresa: TablaEmpresa)

    @Query("SELECT * FROM Empresas WHERE dominio = :dominio")
    suspend fun buscarPorDominio(dominio: String): TablaEmpresa?

    @Query("SELECT * FROM Empresas WHERE cif = :cif")
    suspend fun buscarPorCif(cif: String): TablaEmpresa?

    @Query("SELECT * FROM Empresas")
    suspend fun obtenerTodas(): List<TablaEmpresa>

}