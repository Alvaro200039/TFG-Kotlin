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

    @Query("SELECT * FROM pisos WHERE nombre = :nombrePiso LIMIT 1")
    suspend fun obtenerPisoPorNombre(nombrePiso: String): Piso?

    @Transaction
    @Query("SELECT * FROM pisos WHERE id = :pisoId")
    suspend fun obtenerPisoConSalas(pisoId: Int): PisoConSalas?
}
