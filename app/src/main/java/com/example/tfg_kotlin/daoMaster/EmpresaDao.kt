package com.example.tfg_kotlin.daoMaster

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.tfg_kotlin.entitiesMaster.Empresa

@Dao
interface EmpresaDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertarEmpresa(empresa: Empresa): Long

    @Update
    suspend fun actualizarEmpresa(empresa: Empresa)

    @Delete
    suspend fun eliminarEmpresa(empresa: Empresa)

    @Query("SELECT * FROM empresas WHERE cif = :cif")
    suspend fun obtenerEmpresaPorCif(cif: String): Empresa?

    @Query("SELECT * FROM empresas")
    suspend fun obtenerTodasLasEmpresas(): List<Empresa>
}