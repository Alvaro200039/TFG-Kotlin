package com.example.tfg_kotlin.BBDD_Global.Dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.example.tfg_kotlin.BBDD_Global.entities.Piso
import com.example.tfg_kotlin.BBDD_Global.relations.PisoSalas
import kotlinx.coroutines.flow.Flow

@Dao
interface Dao_Piso {
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
    suspend fun obtenerPisoConSalas(pisoId: Int): PisoSalas?

}