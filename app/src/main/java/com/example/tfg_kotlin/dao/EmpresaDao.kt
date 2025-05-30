package com.example.tfg_kotlin.dao

import androidx.room.*
import com.example.tfg_kotlin.entities.Empresa


@Dao
interface EmpresaDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertarEmpresa(empresa: Empresa): Long

    @Update
    suspend fun actualizarEmpresa(empresa: Empresa)

    @Delete
    suspend fun eliminarEmpresa(empresa: Empresa)

    @Query("SELECT * FROM empresas WHERE id = :id")
    suspend fun obtenerEmpresaPorId(id: Int): Empresa?

    @Query("SELECT * FROM empresas WHERE creadorId = :creadorId")
    suspend fun obtenerEmpresasPorCreador(creadorId: Int): List<Empresa>

    @Query("SELECT * FROM empresas")
    suspend fun obtenerTodasLasEmpresas(): List<Empresa>
}