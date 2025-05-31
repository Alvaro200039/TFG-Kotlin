package com.example.tfg_kotlin.BBDD_Global.Dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.tfg_kotlin.BBDD_Global.entities.FranjaHoraria

@Dao
interface Dao_FranjaHoraria {
    @Query("SELECT * FROM franjas_horarias")
    suspend fun getTodasFranjas(): List<FranjaHoraria> // sin Flow

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertarFranja(franjaHoraria: FranjaHoraria)

    @Delete
    suspend fun eliminarFranja(franjaHoraria: FranjaHoraria)
}