package com.example.tfg_kotlin.dao

import androidx.room.*
import com.example.tfg_kotlin.entities.Piso
import com.example.tfg_kotlin.relations.PisoConSalas
import kotlinx.coroutines.flow.Flow

@Dao
interface PisoDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertarPiso(piso: Piso): Long

    @Update
    suspend fun actualizarPiso(piso: Piso)

    @Delete
    suspend fun eliminarPiso(piso: Piso)

    @Query("SELECT * FROM pisos")
    fun obtenerTodosLosPisos(): Flow<List<Piso>>

    @Query("SELECT * FROM pisos WHERE id = :id")
    suspend fun obtenerPisoPorId(id: Int): Piso?

    @Query("SELECT * FROM pisos WHERE nombre = :nombre LIMIT 1")
    suspend fun obtenerPisoPorNombre(nombre: String): Piso?

    @Query("""
    SELECT * FROM pisos
    WHERE nombre = :nombre
    AND (:empresaId IS NULL AND empresaId IS NULL OR empresaId = :empresaId)
    LIMIT 1
""")
    suspend fun obtenerPisoPorNombreYEmpresa(nombre: String, empresaId: Int?): Piso?

    @Transaction
    @Query("SELECT * FROM pisos WHERE id = :pisoId")
    suspend fun obtenerPisoConSalas(pisoId: Int): PisoConSalas?

}
