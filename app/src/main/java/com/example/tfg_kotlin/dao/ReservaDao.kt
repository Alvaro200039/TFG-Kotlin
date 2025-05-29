package com.example.tfg_kotlin.dao

import androidx.room.*
import com.example.tfg_kotlin.entities.Reserva

@Dao
interface ReservaDao {

    @Query("SELECT * FROM reservas WHERE fechaHora = :fechaHora")
    suspend fun obtenerReservasPorFechaHora(fechaHora: String): List<Reserva>

    @Query("SELECT * FROM reservas")
    suspend fun obtenerTodasReservas(): List<Reserva>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertarReserva(reserva: Reserva)

    @Delete
    suspend fun eliminarReserva(reserva: Reserva)

    @Query("SELECT * FROM reservas WHERE nombreSala = :nombreSala AND piso = :piso AND fechaHora = :fechaHora LIMIT 1")
    suspend fun buscarReserva(nombreSala: String, piso: String, fechaHora: String): Reserva?

    @Query("SELECT * FROM reservas WHERE fechaHora = :fechaHora AND nombreUsuario = :nombreUsuario LIMIT 1")
    suspend fun reservasUsuarioEnHora(fechaHora: String, nombreUsuario: String): Reserva?

    @Query("SELECT * FROM reservas WHERE piso = :piso")
    suspend fun obtenerPorPiso(piso: String): List<Reserva>

    @Query("DELETE FROM reservas WHERE fechaHora < :fechaActual")
    suspend fun eliminarPasadas(fechaActual: String)
}
