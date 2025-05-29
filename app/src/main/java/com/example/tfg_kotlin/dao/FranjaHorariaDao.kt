package com.example.tfg_kotlin.dao

import androidx.room.Dao
import androidx.room.Query
import com.example.tfg_kotlin.entities.FranjaHoraria

@Dao
interface FranjaHorariaDao {
    @Query("SELECT * FROM franjas_horarias")
    suspend fun getTodasFranjas(): List<FranjaHoraria>
}
