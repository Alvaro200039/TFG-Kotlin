package com.example.tfg_kotlin.repository

import com.example.tfg_kotlin.daoApp.FranjaHorariaDao
import com.example.tfg_kotlin.daoApp.PisoDao
import com.example.tfg_kotlin.daoApp.ReservaDao
import com.example.tfg_kotlin.daoApp.SalaDao
import com.example.tfg_kotlin.daoApp.UsuarioDao
import com.example.tfg_kotlin.entitiesApp.FranjaHoraria
import com.example.tfg_kotlin.entitiesApp.Reserva
import com.example.tfg_kotlin.entitiesApp.Salas
import com.example.tfg_kotlin.entitiesApp.Usuario

class AppRepository(
    internal val usuarioDao: UsuarioDao,
    internal val salaDao: SalaDao,
    private val reservaDao: ReservaDao,
    internal val franjaHorariaDao: FranjaHorariaDao,
    internal val pisoDao: PisoDao,
) {

    suspend fun getAllUsuarios() = usuarioDao.obtenerTodos()

    suspend fun insertUsario(usuario: Usuario) {
        usuarioDao.insertar(usuario)
    }

    suspend fun getAllSalas() = salaDao.obtenerTodas()

    suspend fun insertSala(sala: Salas) {
        salaDao.insertar(sala)
    }

    suspend fun getSalasPorPiso(piso: String) = salaDao.obtenerPorPiso(piso)

    suspend fun getAllReservas() = reservaDao.obtenerTodasReservas()

    suspend fun getReservasPorUsuario(idUsuario: Int): List<Reserva> {
        return reservaDao.getReservasPorUsuario(idUsuario)
    }

    suspend fun getReservasPorPiso(piso: String) = reservaDao.obtenerPorPiso(piso)

    suspend fun getReservasPorFechaHora(fechaHora: String) = reservaDao.obtenerReservasPorFechaHora(fechaHora)

    suspend fun buscarReserva(nombreSala: String, piso: String, fechaHora: String) = reservaDao.buscarReserva(nombreSala, piso, fechaHora)

    suspend fun reservasUsuarioEnHora(fechaHora: String, nombreUsuario: String) = reservaDao.reservasUsuarioEnHora(fechaHora, nombreUsuario)

    suspend fun insertarReserva(reserva: Reserva) {
        reservaDao.insertarReserva(reserva)
    }

    suspend fun eliminarReserva(reserva: Reserva) {
        reservaDao.eliminarReserva(reserva)
    }

    suspend fun limpiarReservasAntiguas(fechaActual: String) {
        reservaDao.eliminarPasadas(fechaActual)
    }

    suspend fun getSalasPorNombrePiso(nombrePiso: String): List<Salas> {
        val piso = pisoDao.obtenerPisoPorNombre(nombrePiso)
        return if (piso != null) {
            salaDao.obtenerPorPiso(piso.id.toString())
        } else {
            emptyList()
        }
    }

    // Obtener todas las franjas horarias de la BD
    suspend fun getFranjasHorarias(): List<FranjaHoraria> {
        return franjaHorariaDao.getTodasFranjas()
    }
}