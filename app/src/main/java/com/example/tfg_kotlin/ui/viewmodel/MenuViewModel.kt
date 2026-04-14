package com.example.tfg_kotlin.ui.viewmodel

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.tfg_kotlin.data.model.Empresa
import com.example.tfg_kotlin.data.model.Reserva
import com.example.tfg_kotlin.data.model.Sesion
import com.example.tfg_kotlin.data.model.TipoElemento
import com.example.tfg_kotlin.data.model.Usuario
import com.example.tfg_kotlin.data.repository.FirestoreRepository
import com.example.tfg_kotlin.data.repository.ReservationRepository
import com.example.tfg_kotlin.util.DateFormats
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import java.util.Date


class MenuViewModel : ViewModel() {

    companion object {
        private const val TAG = "MenuViewModel"
    }

    private val empresaId: String?
        get() = Sesion.datos?.empresa?.nombre

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
                    Log.e(TAG, "Error loading user data", e)
                    _error.value = "Error al cargar datos de usuario"
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
        val eId = empresaId ?: return

        viewModelScope.launch {
            try {
                reservationRepo.deletePastReservations(eId)
                val status = reservationRepo.getReservationsByUser(eId, uid)
                
                val now = Date()
                val fullSdf = DateFormats.fullFormat
                val daySdf = DateFormats.dayFormat
                
                val cierreStr = sesion.empresa.cierre.ifEmpty { "23:59" }
                val futureOnes = status.filter {
                    try {
                        if (it.tipo == TipoElemento.PUESTO.valor) {
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

                _nextSalaReserva.value = futureOnes.filter { it.tipo == TipoElemento.SALA.valor }.minByOrNull {
                    fullSdf.parse(it.fechaHora)?.time ?: Long.MAX_VALUE
                }
                
                _nextPuestoReserva.value = futureOnes.filter { it.tipo == TipoElemento.PUESTO.valor }.minByOrNull {
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
        val eId = empresaId ?: return
        viewModelScope.launch {
            try {
                val success = reservationRepo.cancelReservation(eId, reservaId)
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



    fun loadEmpresaSettings() {
        val eId = empresaId ?: return
        viewModelScope.launch {
            try {
                val emp = firestoreRepo.getEmpresaByNombre(eId)
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
                Log.e(TAG, "Error saving schedule settings", e)
                _error.value = "Ha ocurrido un error. Inténtalo de nuevo."
            } finally {
                _loading.value = false
            }
        }
    }

    fun clearSettingsSaveStatus() {
        _settingsSaveStatus.value = false
    }
}
