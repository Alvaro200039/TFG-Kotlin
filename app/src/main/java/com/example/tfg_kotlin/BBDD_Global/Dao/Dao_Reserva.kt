package com.example.tfg_kotlin.BBDD_Global.Dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.tfg_kotlin.BBDD_Global.entities.Reserva

@Dao
interface Dao_Reserva {

    @Query("SELECT * FROM Salas_reservadas WHERE fechaHora = :fechaHora")
    suspend fun obtenerReservasPorFechaHora(fechaHora: String): List<Reserva>

    @Query("SELECT * FROM Salas_reservadas")
    suspend fun obtenerTodasReservas(): List<Reserva>

    @Query("SELECT * FROM Salas_reservadas WHERE idusuario = :idUsuario")
    suspend fun getReservasPorUsuario(idUsuario: Int): List<Reserva>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertarReserva(reserva: Reserva)

    @Delete
    suspend fun eliminarReserva(reserva: Reserva)

    @Query("SELECT * FROM Salas_reservadas WHERE nombreSala = :nombreSala AND piso = :piso AND fechaHora = :fechaHora LIMIT 1")
    suspend fun buscarReserva(nombreSala: String, piso: String, fechaHora: String): Reserva?

    @Query("SELECT * FROM Salas_reservadas WHERE fechaHora = :fechaHora AND nombreUsuario = :nombreUsuario LIMIT 1")
    suspend fun reservasUsuarioEnHora(fechaHora: String, nombreUsuario: String): Reserva?

    @Query("SELECT * FROM Salas_reservadas WHERE piso = :piso")
    suspend fun obtenerPorPiso(piso: String): List<Reserva>

    @Query("DELETE FROM Salas_reservadas WHERE fechaHora < :fechaActual")
    suspend fun eliminarPasadas(fechaActual: String)
}