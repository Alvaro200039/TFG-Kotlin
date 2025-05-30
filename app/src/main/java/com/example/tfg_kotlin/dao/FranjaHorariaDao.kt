package com.example.tfg_kotlin.dao

import androidx.room.*
import com.example.tfg_kotlin.entities.FranjaHoraria
import kotlinx.coroutines.flow.Flow

@Dao
interface FranjaHorariaDao {

    @Query("SELECT * FROM franjas_horarias ORDER BY hora")
    fun getTodasFranjas(): Flow<List<FranjaHoraria>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertarFranja(franjaHoraria: FranjaHoraria)

    @Delete
    suspend fun eliminarFranja(franjaHoraria: FranjaHoraria)
}