package com.example.tfg_kotlin.BBDD_Maestra.Dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.tfg_kotlin.BBDD_Maestra.Entities.Empresa

@Dao
interface MasterDao {

    @Insert
    suspend fun insertarEmpresa(empresa: Empresa): Long

    @Update
    suspend fun actualizarEmpresa(empresa: Empresa)

    @Delete
    suspend fun eliminarEmpresa(empresa: Empresa)

    @Query("SELECT * FROM Empresas WHERE dominio = :dominio")
    suspend fun buscarPorDominio(dominio: String): Empresa?

    @Query("SELECT * FROM empresas WHERE cif = :cif")
    suspend fun buscarPorCif(cif: String): Empresa?

    @Query("SELECT * FROM empresas")
    suspend fun obtenerTodasLasEmpresas(): List<Empresa>
}