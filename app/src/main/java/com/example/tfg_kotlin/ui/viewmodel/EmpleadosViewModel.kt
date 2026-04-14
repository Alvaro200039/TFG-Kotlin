package com.example.tfg_kotlin.ui.viewmodel

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

class EmpleadosViewModel : ViewModel() {
    companion object {
        const val SLOT_PUESTO = "Día completo"
    }

    private val firestore = FirebaseFirestore.getInstance()
    private val firestoreRepo = FirestoreRepository(firestore)
    private val reservationRepo = ReservationRepository(firestore)

    private val _pisos = MutableLiveData<List<Piso>>()
    val pisos: LiveData<List<Piso>> = _pisos

    private val _salas = MutableLiveData<List<Sala>>()
    val salas: LiveData<List<Sala>> = _salas

    private val _reservas = MutableLiveData<List<Reserva>>()
    val reservas: LiveData<List<Reserva>> = _reservas

    private val _fechaSeleccionada = MutableLiveData("")
    val fechaSeleccionada: LiveData<String> = _fechaSeleccionada

    private val _horaSeleccionada = MutableLiveData(SLOT_PUESTO)
    val horaSeleccionada: LiveData<String> = _horaSeleccionada

    private val _startTime = MutableLiveData(9.0f)
    val startTime: LiveData<Float> = _startTime

    private val _endTime = MutableLiveData(17.0f)
    val endTime: LiveData<Float> = _endTime

    private val _overlaps = MutableLiveData<Map<String, List<Triple<Float, Float, Int>>>>()
    val overlaps: LiveData<Map<String, List<Triple<Float, Float, Int>>>> = _overlaps

    private val _franjas = MutableLiveData<List<String>>()
    val franjas: LiveData<List<String>> = _franjas

    private val _error = MutableLiveData<String>()
    val error: LiveData<String> = _error

    private val _loading = MutableLiveData<Boolean>()
    val loading: LiveData<Boolean> = _loading

    private val _reservaStatus = MutableLiveData<Boolean>()
    val reservaStatus: LiveData<Boolean> = _reservaStatus

    private val _empresa = MutableLiveData<com.example.tfg_kotlin.data.model.Empresa?>()
    val empresa: LiveData<com.example.tfg_kotlin.data.model.Empresa?> = _empresa

    private val _editingReservaId = MutableLiveData<String?>(null)
    val editingReservaId: LiveData<String?> = _editingReservaId

    private val _focusedSalaId = MutableLiveData<String?>(null)
    val focusedSalaId: LiveData<String?> = _focusedSalaId

    private var tempBackupReserva: Reserva? = null

    fun loadPisos() {
        val empresaId = Sesion.datos?.empresa?.nombre ?: run {
            _error.value = "Sesión no válida"
            return
        }
        
        viewModelScope.launch {
            _loading.value = true
            try {
                val pisosList = firestoreRepo.getPisosByEmpresa(empresaId)
                _pisos.value = pisosList
                
                val emp = firestoreRepo.getEmpresaByNombre(empresaId)
                _empresa.value = emp
                
                val franjasList = try {
                    firestoreRepo.getFranjasByEmpresa(empresaId).map { it.hora }.sorted()
                } catch (_: Exception) {
                    emptyList<String>()
                }
                _franjas.value = listOf(SLOT_PUESTO) + franjasList
            } catch (e: Exception) {
                // Solo mostrar error si realmente falla lo crítico (pisos o empresa)
                if (_pisos.value.isNullOrEmpty() || _empresa.value == null) {
                    _error.value = "Error al conectar con el servidor"
                }
                e.printStackTrace()
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
            } catch (_: Exception) {
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
        if (hora == SLOT_PUESTO) {
            _overlaps.value = emptyMap()
        }
        checkAvailability()
    }

    fun checkAvailability() {
        val fecha = _fechaSeleccionada.value ?: ""
        if (fecha.isEmpty()) return

        val empresaId = Sesion.datos?.empresa?.nombre ?: return
        
        viewModelScope.launch {
            try {
                val list = reservationRepo.getReservationsByDay(empresaId, fecha)
                _reservas.value = list
                calculatePartialOverlaps(list)
            } catch (_: Exception) {
                // Silently fail
            }
        }
    }

    fun updateRange(start: Float, end: Float) {
        _startTime.value = start
        _endTime.value = end
        _horaSeleccionada.value = "${formatTime(start)} - ${formatTime(end)}"
        _reservas.value?.let { calculatePartialOverlaps(it) }
    }

    private fun calculatePartialOverlaps(reservations: List<Reserva>) {
        val empresa = _empresa.value ?: return
        
        // Si estamos en modo día completo, no hay overlaps paramétricos de barras de volumen
        if (_horaSeleccionada.value == SLOT_PUESTO || _horaSeleccionada.value.isNullOrEmpty()) {
            _overlaps.value = emptyMap()
            return
        }
        
        val apertura = timeToFloat(empresa.apertura)
        val cierre = timeToFloat(empresa.cierre)
        
        val uStart = _startTime.value ?: apertura
        val uEnd = _endTime.value ?: cierre
        val uDuration = uEnd - uStart
        if (uDuration <= 0f) return

        val currentUid = Sesion.datos?.usuario?.uid ?: ""
        val editId = _editingReservaId.value
        
        val map = mutableMapOf<String, MutableList<Triple<Float, Float, Int>>>()
        val salas = _salas.value ?: emptyList()

        salas.forEach { sala ->
            val salaId = sala.id ?: return@forEach
            val list = mutableListOf<Triple<Float, Float, Int>>()
            
            // 1. Mostrar la original en NARANJA (0) a pesar de la colisión, si estamos editando
            if (editId != null && salaId == _focusedSalaId.value) {
                tempBackupReserva?.let { res ->
                    parseReservaRange(res.fechaHora)?.let { (rs, re) ->
                        val overlapStart = maxOf(uStart, rs)
                        val overlapEnd = minOf(uEnd, re)
                        if (overlapStart < overlapEnd) {
                            list.add(Triple((overlapStart - uStart) / uDuration, (overlapEnd - uStart) / uDuration, 0))
                        }
                    }
                }
            }

            // 2. Extraer los rangos reales de las reuniones ya ocupadas
            reservations.filter { it.idSala == salaId && it.id != editId }.forEach { res ->
                parseReservaRange(res.fechaHora)?.let { (rs, re) ->
                    val overlapStart = maxOf(uStart, rs)
                    val overlapEnd = minOf(uEnd, re)
                    if (overlapStart < overlapEnd) {
                        val isMine = res.idUsuario == currentUid
                        val type = if (isMine) 2 else 1
                        list.add(Triple((overlapStart - uStart) / uDuration, (overlapEnd - uStart) / uDuration, type))
                    }
                }
            }
            
            if (list.isNotEmpty()) {
                map[salaId] = list
            }
        }
        _overlaps.value = map
    }

    fun parseReservaRange(fechaHora: String): Pair<Float, Float>? {
        return try {
            val parts = fechaHora.split(" ")
            if (parts.size < 3) return null
            val rangePart = parts.subList(1, parts.size).joinToString(" ")
            val times = rangePart.split("-")
            if (times.size != 2) return null
            timeToFloat(times[0].trim()) to timeToFloat(times[1].trim())
        } catch (_: Exception) { null }
    }

    fun timeToFloat(time: String): Float {
        return try {
            val p = time.split(":")
            val h = p[0].filter { it.isDigit() }.toFloat()
            val m = if (p.size > 1) p[1].filter { it.isDigit() }.toFloat() else 0f
            h + m / 60f
        } catch (_: Exception) { 0f }
    }

    fun formatTime(value: Float): String {
        val h = value.toInt()
        val m = ((value - h) * 60).toInt()
        return "%02d:%02d".format(h, m)
    }

    fun reservarSala(sala: Sala, pisoNombre: String) {
        val empresaId = Sesion.datos?.empresa?.nombre ?: return
        val usuario = Sesion.datos?.usuario ?: return
        val fecha = _fechaSeleccionada.value ?: return
        val hora = _horaSeleccionada.value ?: return
        val empresa = _empresa.value ?: return
        val fechaHora = "$fecha $hora"

        // Calcular rango de la nueva reserva
        val bStart = timeToFloat(empresa.apertura)
        val bEnd = timeToFloat(empresa.cierre)
        val uRange = if (hora == SLOT_PUESTO) {
            bStart to bEnd
        } else {
            parseReservaRange(fechaHora) ?: return
        }

        viewModelScope.launch {
            _loading.value = true
            try {
                // Obtener todas las reservas del día para verificar solapamientos del usuario
                val allDayReservations = reservationRepo.getReservationsByDay(empresaId, fecha)
                val editId = _editingReservaId.value
                val myReservations = allDayReservations.filter { 
                    it.idUsuario == usuario.uid && it.tipo == sala.tipo && it.id != editId
                }

                for (res in myReservations) {
                    val rRange = if (res.fechaHora.contains(SLOT_PUESTO)) {
                        bStart to bEnd
                    } else {
                        parseReservaRange(res.fechaHora)
                    }

                    if (rRange != null) {
                        // Detección de solapamiento: max(inicio1, inicio2) < min(fin1, fin2)
                        if (maxOf(uRange.first, rRange.first) < minOf(uRange.second, rRange.second)) {
                            _error.value = "Ya tienes una sala reservada en este horario, cancela la otra reserva o elige otro horario"
                            return@launch
                        }
                    }
                }

                // --- Lógica de Fusión y Validación de Límite Máximo (Anti-trampa) ---
                val sameRoomReservations = allDayReservations.filter { 
                    it.idSala == sala.id && it.idUsuario == usuario.uid && it.id != editId 
                }

                var finalStart = uRange.first
                var finalEnd = uRange.second
                val idsToMergeAndDelete = mutableListOf<String>()

                var changed = true
                while (changed) {
                    changed = false
                    for (res in sameRoomReservations) {
                        if (res.id in idsToMergeAndDelete) continue
                        val rRange = parseReservaRange(res.fechaHora) ?: continue
                        
                        // Si se tocan o se solapan, los fusionamos virtualmente
                        val isContiguous = Math.abs(rRange.second - finalStart) < 0.01f || Math.abs(finalEnd - rRange.first) < 0.01f
                        val isOverlapping = maxOf(rRange.first, finalStart) < minOf(rRange.second, finalEnd)

                        if (isContiguous || isOverlapping) {
                            finalStart = minOf(finalStart, rRange.first)
                            finalEnd = maxOf(finalEnd, rRange.second)
                            res.id?.let { idsToMergeAndDelete.add(it) }
                            changed = true
                        }
                    }
                }

                val totalDuration = finalEnd - finalStart
                if (totalDuration > (empresa.maxDuration ?: 24) + 0.01f) {
                    _error.value = "El límite de tiempo para esta sala ha sido superado. Reduce el tiempo de la reunión o modifica la reserva actual."
                    return@launch
                }

                val finalFechaHora = "$fecha ${formatTime(finalStart)} - ${formatTime(finalEnd)}"
                
                val reserva = Reserva(
                    nombreSala = sala.nombre,
                    idSala = sala.id ?: "",
                    fechaHora = finalFechaHora,
                    nombreUsuario = "${usuario.nombre} ${usuario.apellidos}",
                    idUsuario = usuario.uid,
                    piso = pisoNombre,
                    tipo = sala.tipo
                )

                val success = reservationRepo.addReservation(empresaId, reserva)
                if (success) {
                    // Borrar todas las reservas que han sido absorbidas por la nueva gran reserva
                    for (id in idsToMergeAndDelete) {
                        reservationRepo.cancelReservation(empresaId, id)
                    }
                    // Borrar también la original si veníamos de edición y no estaba ya en la lista
                    editId?.let { 
                        if (it !in idsToMergeAndDelete) reservationRepo.cancelReservation(empresaId, it) 
                    }

                    _editingReservaId.value = null
                    tempBackupReserva = null
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
                    _reservaStatus.value = true
                    checkAvailability()
                } else {
                    _error.value = "No se pudo cancelar la reserva"
                }
            } catch (e: Exception) {
                _error.value = "Error al cancelar: ${e.message}"
            }
        }
    }

    fun clearReservaStatus() {
        _reservaStatus.value = false
    }

    fun startEditingSession(reserva: Reserva) {
        tempBackupReserva = reserva
        _editingReservaId.value = reserva.id
        // No borramos la reserva aquí. La borraremos solo si el usuario
        // logra guardar con éxito el nuevo horario.
        viewModelScope.launch {
            checkAvailability()
        }
    }

    fun restoreBackup(onDone: (() -> Unit)? = null) {
        // Como ya no borramos al inicio, restaurar es simplemente limpiar el estado local
        // para que el UI deje de ignorar la reserva original.
        tempBackupReserva = null
        _editingReservaId.value = null
        onDone?.invoke()
    }

    fun setEditingReservaId(id: String?) {
        _editingReservaId.value = id
    }

    fun clearEditingReservaId() {
        _editingReservaId.value = null
    }

    fun setFocusedSalaId(id: String?) {
        _focusedSalaId.value = id
    }

    fun cancelarReservaDirecto(reservaId: String) {
        val empresaId = Sesion.datos?.empresa?.nombre ?: return
        viewModelScope.launch {
            _loading.value = true
            try {
                reservationRepo.cancelReservation(empresaId, reservaId)
            } catch (e: Exception) {
                _error.value = "Error al eliminar: ${e.message}"
            } finally {
                _loading.value = false
            }
        }
    }
}
