package com.example.tfg_kotlin.repository

import com.example.tfg_kotlin.dao.EmpresaDao
import com.example.tfg_kotlin.dao.FranjaHorariaDao
import com.example.tfg_kotlin.dao.PisoDao
import com.example.tfg_kotlin.dao.UsuarioDao
import com.example.tfg_kotlin.dao.ReservaDao
import com.example.tfg_kotlin.dao.SalaDao
import com.example.tfg_kotlin.entities.FranjaHoraria
import com.example.tfg_kotlin.entities.Usuario
import com.example.tfg_kotlin.entities.Reserva
import com.example.tfg_kotlin.entities.Salas

class AppRepository(
    internal val usuarioDao: UsuarioDao,
    internal val salaDao: SalaDao,
    private val reservaDao: ReservaDao,
    internal val franjaHorariaDao: FranjaHorariaDao,
    internal val pisoDao: PisoDao,
    internal val empresaDao: EmpresaDao
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