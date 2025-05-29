package com.example.tfg_kotlin.dao

import androidx.room.*
import com.example.tfg_kotlin.entities.Salas

@Dao
interface SalaDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertar(sala: Salas)

    @Query("SELECT * FROM salas WHERE piso = :piso")
    suspend fun obtenerPorPiso(piso: String): List<Salas>

    @Query("SELECT * FROM salas")
    suspend fun obtenerTodas(): List<Salas>

    @Update
    suspend fun actualizar(sala: Salas)

    @Delete
    suspend fun eliminar(sala: Salas)

    @Query("DELETE FROM salas WHERE piso = :piso")
    suspend fun eliminarPorPiso(piso: String)
}