package com.example.tfg_kotlin.daoApp

import androidx.room.*
import com.example.tfg_kotlin.entitiesApp.Salas

@Dao
interface SalaDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertar(sala: Salas)

    @Query("SELECT * FROM salas WHERE pisoId = :pisoId")
    suspend fun obtenerPorPiso(pisoId: String): List<Salas>


    @Query("SELECT * FROM salas")
    suspend fun obtenerTodas(): List<Salas>

    @Update
    suspend fun actualizar(sala: Salas)

    @Query("SELECT * FROM salas WHERE nombre = :nombreSala AND pisoId = :pisoId")
    suspend fun obtenerSalaPorNombreYPiso(nombreSala: String, pisoId: Int): Salas?

    @Query("DELETE FROM salas WHERE pisoId = :pisoId")
    suspend fun eliminarPorPiso(pisoId: Int)
}
