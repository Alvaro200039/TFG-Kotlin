package com.example.tfg_kotlin.ui.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.tfg_kotlin.data.model.Empresa
import com.example.tfg_kotlin.data.model.Reserva
import com.example.tfg_kotlin.data.model.Sesion
import com.example.tfg_kotlin.data.model.Usuario
import com.example.tfg_kotlin.data.repository.FirestoreRepository
import com.example.tfg_kotlin.data.repository.ReservationRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class MenuViewModel : ViewModel() {

    private val firestore = FirebaseFirestore.getInstance()
    private val firestoreRepo = FirestoreRepository(firestore)
    private val reservationRepo = ReservationRepository(firestore)
    private val auth = FirebaseAuth.getInstance()

    private val _usuario = MutableLiveData<Usuario?>()
    val usuario: LiveData<Usuario?> = _usuario

    private val _nextSalaReserva = MutableLiveData<Reserva?>()
    val nextSalaReserva: LiveData<Reserva?> = _nextSalaReserva

    private val _nextPuestoReserva = MutableLiveData<Reserva?>()
    val nextPuestoReserva: LiveData<Reserva?> = _nextPuestoReserva

    private val _userReservas = MutableLiveData<List<Reserva>>()
    val userReservas: LiveData<List<Reserva>> = _userReservas

    private val _franjas = MutableLiveData<List<String>>()
    val franjas: LiveData<List<String>> = _franjas

    private val _error = MutableLiveData<String>()
    val error: LiveData<String> = _error

    private val _loading = MutableLiveData<Boolean>()
    val loading: LiveData<Boolean> = _loading

    private val _empresa = MutableLiveData<Empresa?>()
    val empresa: LiveData<Empresa?> = _empresa

    private val _settingsSaveStatus = MutableLiveData<Boolean>()
    val settingsSaveStatus: LiveData<Boolean> = _settingsSaveStatus

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
                val fullSdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
                val daySdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                
                val cierreStr = sesion.empresa.cierre.ifEmpty { "23:59" }
                val futureOnes = status.filter {
                    try {
                        if (it.tipo == "PUESTO") {
                            val datePart = it.fechaHora.split(" ")[0]
                            val rEnd = fullSdf.parse("$datePart $cierreStr")
                            rEnd != null && rEnd.time >= now.time
                        } else {
                            val parts = it.fechaHora.split(" - ")
                            if (parts.size == 2) {
                                val datePart = parts[0].split(" ")[0]
                                val endTimeStr = parts[1].trim()
                                val rEnd = fullSdf.parse("$datePart $endTimeStr")
                                rEnd != null && rEnd.time >= now.time
                            } else {
                                val rStart = fullSdf.parse(it.fechaHora)
                                rStart != null && rStart.time >= now.time
                            }
                        }
                    } catch (_: Exception) { false }
                }

                _nextSalaReserva.value = futureOnes.filter { it.tipo == "SALA" }.minByOrNull {
                    fullSdf.parse(it.fechaHora)?.time ?: Long.MAX_VALUE
                }
                
                _nextPuestoReserva.value = futureOnes.filter { it.tipo == "PUESTO" }.minByOrNull {
                    val dateStr = it.fechaHora.split(" ")[0]
                    daySdf.parse(dateStr)?.time ?: Long.MAX_VALUE
                }

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

    fun loadFranjas() {
        val empresaId = Sesion.datos?.empresa?.nombre ?: return
        viewModelScope.launch {
            try {
                val franjas = firestoreRepo.getFranjasByEmpresa(empresaId)
                _franjas.value = franjas.map { it.hora }.sorted()
            } catch (e: Exception) {
                _error.value = "Error al cargar franjas: ${e.message}"
            }
        }
    }

    fun addFranja(franja: String) {
        val empresaId = Sesion.datos?.empresa?.nombre ?: return
        viewModelScope.launch {
            try {
                firestoreRepo.addFranja(empresaId, franja)
                loadFranjas()
            } catch (e: Exception) {
                _error.value = "Error al añadir franja: ${e.message}"
            }
        }
    }

    fun removeFranja(franja: String) {
        val empresaId = Sesion.datos?.empresa?.nombre ?: return
        viewModelScope.launch {
            try {
                firestoreRepo.deleteFranja(empresaId, franja)
                loadFranjas()
            } catch (e: Exception) {
                _error.value = "Error al eliminar franja: ${e.message}"
            }
        }
    }

    fun loadEmpresaSettings() {
        val empresaNombre = Sesion.datos?.empresa?.nombre ?: return
        viewModelScope.launch {
            try {
                val emp = firestoreRepo.getEmpresaByNombre(empresaNombre)
                if (emp != null) {
                    _empresa.value = emp
                    // Update session
                    Sesion.datos = Sesion.datos?.copy(empresa = emp)
                }
            } catch (_: Exception) {}
        }
    }

    fun saveScheduleSettings(
        apertura: String, 
        cierre: String, 
        diasApertura: List<Int>, 
        diasBloqueados: List<String>,
        stepSize: Float = 0.5f,
        maxDuration: Int = 2,
        extrasSalas: List<String>? = null,
        extrasPuestos: List<String>? = null
    ) {
        val empresaActual = _empresa.value ?: Sesion.datos?.empresa ?: return
        val updatedEmpresa = empresaActual.copy(
            apertura = apertura,
            cierre = cierre,
            diasApertura = diasApertura,
            diasBloqueados = diasBloqueados,
            stepSize = stepSize,
            maxDuration = maxDuration,
            extrasSalas = extrasSalas ?: empresaActual.extrasSalas,
            extrasPuestos = extrasPuestos ?: empresaActual.extrasPuestos
        )

        viewModelScope.launch {
            _loading.value = true
            try {
                val success = firestoreRepo.saveEmpresa(updatedEmpresa)
                if (success) {
                    _empresa.value = updatedEmpresa
                    Sesion.datos = Sesion.datos?.copy(empresa = updatedEmpresa)
                    _settingsSaveStatus.value = true
                } else {
                    _error.value = "Error al guardar configuración"
                }
            } catch (e: Exception) {
                _error.value = "Error: ${e.message}"
            } finally {
                _loading.value = false
            }
        }
    }

    fun clearSettingsSaveStatus() {
        _settingsSaveStatus.value = false
    }
}
