package com.example.tfg_kotlin.ui.viewmodel

import android.graphics.drawable.Drawable
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.tfg_kotlin.data.model.Piso
import com.example.tfg_kotlin.data.model.Reserva
import com.example.tfg_kotlin.data.model.Sala
import com.example.tfg_kotlin.data.model.Sesion
import com.example.tfg_kotlin.data.repository.FirestoreRepository
import com.example.tfg_kotlin.data.repository.ReservationRepository
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class EmpleadosViewModel : ViewModel() {

    private val firestore = FirebaseFirestore.getInstance()
    private val firestoreRepo = FirestoreRepository(firestore)
    private val reservationRepo = ReservationRepository(firestore)

    private val _pisos = MutableLiveData<List<Piso>>()
    val pisos: LiveData<List<Piso>> = _pisos

    private val _salas = MutableLiveData<List<Sala>>()
    val salas: LiveData<List<Sala>> = _salas

    private val _reservas = MutableLiveData<List<Reserva>>()
    val reservas: LiveData<List<Reserva>> = _reservas

    private val _fechaSeleccionada = MutableLiveData<String>("")
    val fechaSeleccionada: LiveData<String> = _fechaSeleccionada

    private val _horaSeleccionada = MutableLiveData<String>("")
    val horaSeleccionada: LiveData<String> = _horaSeleccionada

    private val _franjas = MutableLiveData<List<String>>()
    val franjas: LiveData<List<String>> = _franjas

    private val _error = MutableLiveData<String>()
    val error: LiveData<String> = _error

    private val _loading = MutableLiveData<Boolean>()
    val loading: LiveData<Boolean> = _loading

    private val _reservaStatus = MutableLiveData<Boolean>()
    val reservaStatus: LiveData<Boolean> = _reservaStatus

    fun loadPisos() {
        val empresaId = Sesion.datos?.empresa?.nombre ?: return
        viewModelScope.launch {
            _loading.value = true
            try {
                _pisos.value = firestoreRepo.getPisosByEmpresa(empresaId)
                _franjas.value = firestoreRepo.getFranjasByEmpresa(empresaId).map { it.hora }.sorted()
            } catch (e: Exception) {
                _error.value = "Error al cargar datos"
            } finally {
                _loading.value = false
            }
        }
    }

    fun loadSalas(pisoId: String) {
        val empresaId = Sesion.datos?.empresa?.nombre ?: return
        viewModelScope.launch {
            try {
                _salas.value = firestoreRepo.getSalasByPiso(empresaId, pisoId)
                checkAvailability()
            } catch (e: Exception) {
                _error.value = "Error al cargar salas"
            }
        }
    }

    fun updateFecha(fecha: String) {
        _fechaSeleccionada.value = fecha
        checkAvailability()
    }

    fun updateHora(hora: String) {
        _horaSeleccionada.value = hora
        checkAvailability()
    }

    fun checkAvailability() {
        val fecha = _fechaSeleccionada.value ?: ""
        val hora = _horaSeleccionada.value ?: ""
        if (fecha.isEmpty() || hora.isEmpty()) return

        val empresaId = Sesion.datos?.empresa?.nombre ?: return
        val fechaHora = "$fecha $hora"

        viewModelScope.launch {
            try {
                _reservas.value = reservationRepo.getReservationsByDateTime(empresaId, fechaHora)
            } catch (e: Exception) {
                // Silently fail availability check to not annoy user
            }
        }
    }

    fun reservarSala(sala: Sala, pisoNombre: String) {
        val empresaId = Sesion.datos?.empresa?.nombre ?: return
        val usuario = Sesion.datos?.usuario ?: return
        val fecha = _fechaSeleccionada.value ?: return
        val hora = _horaSeleccionada.value ?: return
        val fechaHora = "$fecha $hora"

        viewModelScope.launch {
            _loading.value = true
            try {
                // Re-check overlap
                val existing = reservationRepo.getReservationsByDateTime(empresaId, fechaHora)
                if (existing.any { it.idusuario == usuario.uid }) {
                    _error.value = "Ya tienes una reserva en este horario"
                    return@launch
                }

                val reserva = Reserva(
                    nombreSala = sala.nombre,
                    idSala = sala.id ?: "",
                    fechaHora = fechaHora,
                    nombreUsuario = usuario.nombre,
                    idusuario = usuario.uid,
                    piso = pisoNombre
                )

                val success = reservationRepo.addReservation(empresaId, reserva)
                if (success) {
                    _reservaStatus.value = true
                    checkAvailability()
                } else {
                    _error.value = "No se pudo realizar la reserva"
                }
            } catch (e: Exception) {
                _error.value = "Error al reservar: ${e.message}"
            } finally {
                _loading.value = false
            }
        }
    }

    fun cancelReserva(reservaId: String) {
        val empresaId = Sesion.datos?.empresa?.nombre ?: return
        viewModelScope.launch {
            try {
                val success = reservationRepo.cancelReservation(empresaId, reservaId)
                if (success) {
                    checkAvailability()
                } else {
                    _error.value = "No se pudo cancelar la reserva"
                }
            } catch (e: Exception) {
                _error.value = "Error al cancelar: ${e.message}"
            }
        }
    }
}
