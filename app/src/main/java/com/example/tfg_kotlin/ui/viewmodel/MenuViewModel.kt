package com.example.tfg_kotlin.ui.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.tfg_kotlin.data.model.Reserva
import com.example.tfg_kotlin.data.model.Sesion
import com.example.tfg_kotlin.data.model.Usuario
import com.example.tfg_kotlin.data.repository.FirestoreRepository
import com.example.tfg_kotlin.data.repository.ReservationRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MenuViewModel : ViewModel() {

    private val firestore = FirebaseFirestore.getInstance()
    private val firestoreRepo = FirestoreRepository(firestore)
    private val reservationRepo = ReservationRepository(firestore)
    private val auth = FirebaseAuth.getInstance()

    private val _usuario = MutableLiveData<Usuario?>()
    val usuario: LiveData<Usuario?> = _usuario

    private val _nextReserva = MutableLiveData<Reserva?>()
    val nextReserva: LiveData<Reserva?> = _nextReserva

    private val _userReservas = MutableLiveData<List<Reserva>>()
    val userReservas: LiveData<List<Reserva>> = _userReservas

    private val _error = MutableLiveData<String>()
    val error: LiveData<String> = _error

    private val _loading = MutableLiveData<Boolean>()
    val loading: LiveData<Boolean> = _loading

    init {
        loadUserData()
    }

    private fun loadUserData() {
        val sesion = Sesion.datos ?: return
        val email = sesion.usuario.email

        if (sesion.usuario.nombre.isEmpty()) {
            viewModelScope.launch {
                _loading.value = true
                try {
                    val user = firestoreRepo.getUsuarioByEmail(email)
                    if (user != null) {
                        _usuario.value = user
                        Sesion.datos = sesion.copy(usuario = user)
                    } else {
                        _error.value = "Usuario no encontrado"
                    }
                } catch (e: Exception) {
                    _error.value = "Error al cargar usuario: ${e.message}"
                } finally {
                    _loading.value = false
                }
            }
        } else {
            _usuario.value = sesion.usuario
        }
    }

    fun logout() {
        auth.signOut()
        Sesion.cerrarSesion()
    }

    fun loadNextReserva() {
        val sesion = Sesion.datos ?: return
        val uid = auth.currentUser?.uid ?: return
        val empresaId = sesion.empresa.nombre

        viewModelScope.launch {
            try {
                reservationRepo.deletePastReservations(empresaId)
                val status = reservationRepo.getReservationsByUser(empresaId, uid)
                
                val now = Date()
                val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
                
                val next = status.filter {
                    try {
                        val date = sdf.parse(it.fechaHora)
                        date?.after(now) == true
                    } catch (_: Exception) { false }
                }.minByOrNull {
                    sdf.parse(it.fechaHora)?.time ?: Long.MAX_VALUE
                }
                
                _nextReserva.value = next
                _userReservas.value = status
            } catch (_: Exception) {
                // Silently handle error or post to _error
            }
        }
    }

    fun cancelReserva(reservaId: String) {
        val empresaId = Sesion.datos?.empresa?.nombre ?: return
        viewModelScope.launch {
            try {
                val success = reservationRepo.cancelReservation(empresaId, reservaId)
                if (success) {
                    loadNextReserva()
                } else {
                    _error.value = "No se pudo cancelar la reserva"
                }
            } catch (_: Exception) {
                _error.value = "Error al cancelar la reserva"
            }
        }
    }
}
