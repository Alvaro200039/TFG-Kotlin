package com.example.tfg_kotlin.ui.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.tfg_kotlin.data.model.Piso
import com.example.tfg_kotlin.data.model.Sala
import com.example.tfg_kotlin.data.model.Sesion
import com.example.tfg_kotlin.data.repository.FirestoreRepository
import com.example.tfg_kotlin.data.repository.ReservationRepository
import com.example.tfg_kotlin.data.repository.StorageRepository
import kotlinx.coroutines.launch

class CreacionViewModel(
    private val repository: FirestoreRepository = FirestoreRepository(),
    private val reservationRepo: ReservationRepository = ReservationRepository(),
    private val storageRepo: StorageRepository = StorageRepository()
) : ViewModel() {

    private val salasBorradas = mutableListOf<String>()

    private val _pisos = MutableLiveData<List<Piso>>()
    val pisos: LiveData<List<Piso>> = _pisos

    private val _pisoActual = MutableLiveData<Piso?>()
    val pisoActual: LiveData<Piso?> = _pisoActual

    private val _salas = MutableLiveData<List<Sala>>(emptyList())
    val salas: LiveData<List<Sala>> = _salas

    private val _error = MutableLiveData<String>()
    val error: LiveData<String> = _error

    private val _saveStatus = MutableLiveData<Boolean>()
    val saveStatus: LiveData<Boolean> = _saveStatus


    fun loadPisos(action: String? = null, selectedPisoId: String? = null) {
        val nombreEmpresa = Sesion.datos?.empresa?.nombre ?: return
        viewModelScope.launch {
            try {
                val listaPisos = repository.getPisosByEmpresa(nombreEmpresa)
                _pisos.value = listaPisos
                
                // Si no hay acción específica, creamos un piso nuevo por defecto al entrar
                if (action == null) {
                    val count = listaPisos.size + 1
                    crearNuevoPiso("Piso nº $count")
                    return@launch
                }

                when (action) {
                    "CREATE_NEW" -> {
                        val count = listaPisos.size + 1
                        crearNuevoPiso("Piso nº $count")
                    }
                    "EDIT" -> {
                        val toEdit = listaPisos.find { it.id == selectedPisoId }
                        if (toEdit != null) {
                            setPisoActual(toEdit)
                        } else {
                            val count = listaPisos.size + 1
                            crearNuevoPiso("Piso nº $count")
                        }
                    }
                    else -> {
                        if (listaPisos.isNotEmpty()) {
                            setPisoActual(listaPisos.last())
                        }
                    }
                }
            } catch (e: Exception) {
                _error.value = "Error al cargar pisos"
            }
        }
    }

    fun setPisoActual(piso: Piso) {
        _pisoActual.value = piso
        loadSalas(piso)
    }

    private fun loadSalas(piso: Piso) {
        val nombreEmpresa = Sesion.datos?.empresa?.nombre ?: return
        val pisoId = piso.id ?: return
        viewModelScope.launch {
            try {
                val listaSalas = repository.getSalasByPiso(nombreEmpresa, pisoId)
                _salas.value = listaSalas
            } catch (e: Exception) {
                _error.value = "Error al cargar salas"
            }
        }
    }


    fun crearNuevoPiso(nuevoNombre: String) {
        val cif = Sesion.datos?.empresa?.cif ?: ""
        // Un piso totalmente nuevo (no ide) y limpiamos las salas del layout
        _pisoActual.value = Piso(nombre = nuevoNombre, empresaCif = cif)
        _salas.value = emptyList()
    }



    fun updateSala(oldSala: Sala, newSala: Sala) {
        val currentSalas = _salas.value?.toMutableList() ?: mutableListOf()
        val index = currentSalas.indexOf(oldSala)
        if (index != -1) {
            currentSalas[index] = newSala
            _salas.value = currentSalas
        }
    }

    fun syncSalas(nuevasSalas: List<Sala>) {
        val prevIds = _salas.value?.mapNotNull { it.id } ?: emptyList()
        val newIds = nuevasSalas.mapNotNull { it.id }
        salasBorradas.addAll(prevIds - newIds.toSet())
        salasBorradas.removeAll(newIds)
        _salas.value = nuevasSalas.toList()
    }

    private val _confirmacionRequerida = MutableLiveData<Boolean>()
    val confirmacionRequerida: LiveData<Boolean> = _confirmacionRequerida

    private val _confirmacionRequeridaEliminar = MutableLiveData<Piso?>()
    val confirmacionRequeridaEliminar: LiveData<Piso?> = _confirmacionRequeridaEliminar

    fun confirmarGuardado(imageBytes: ByteArray? = null) {
        _confirmacionRequerida.value = false
        guardarDistribucion(imageBytes)
    }

    fun verificarReservasYGuardar(imageBytes: ByteArray? = null) {
        val pisoId = _pisoActual.value?.id
        val nombreEmpresa = Sesion.datos?.empresa?.nombre ?: return
        
        if (pisoId == null) {
            guardarDistribucion(imageBytes)
            return
        }

        viewModelScope.launch {
            try {
                val reservas = reservationRepo.getReservationsByEmpresa(nombreEmpresa)
                val activeIds = _salas.value?.mapNotNull { it.id } ?: emptyList()
                val deletedIds = salasBorradas.toList()
                val affectedRooms = activeIds + deletedIds
                
                val hasReservations = reservas.any { it.idSala in affectedRooms }
                
                if (hasReservations) {
                    _confirmacionRequerida.value = true
                } else {
                    guardarDistribucion(imageBytes)
                }
            } catch (e: Exception) {
                guardarDistribucion(imageBytes) // Si falla verificar, guardamos igual por si acaso
            }
        }
    }

    fun guardarDistribucion(imageBytes: ByteArray? = null) {
        var piso = _pisoActual.value
        val nombreEmpresa = Sesion.datos?.empresa?.nombre ?: return
        
        if (piso == null) {
            val cif = Sesion.datos?.empresa?.cif ?: ""
            piso = Piso(nombre = "Piso Principal", empresaCif = cif)
            _pisoActual.value = piso
        }
        
        val salas = _salas.value ?: emptyList()

        viewModelScope.launch {
            try {
                // Aquí iría la lógica de guardado que antes estaba en la Activity
                // Invocando a repository.savePiso y repository.saveSala
                // Por ahora simplificamos a una sola llamada si el repo lo permite o varias
                
                val pisoId = repository.savePiso(nombreEmpresa, piso)
                if (pisoId != null) {
                    piso.id = pisoId

                    // Subir imagen si existe
                    if (imageBytes != null) {
                        val imageUrl = storageRepo.uploadPisoImagen(nombreEmpresa, pisoId, imageBytes)
                        if (imageUrl != null) {
                            piso.imagenUrl = imageUrl
                            repository.savePiso(nombreEmpresa, piso) // Actualizar con URL
                        }
                    }

                    // Guardar cada sala activa
                    salas.forEach { sala ->
                        sala.idPiso = pisoId
                        repository.saveSala(nombreEmpresa, pisoId, sala)
                        if (sala.id != null) {
                            reservationRepo.cascadeUpdateSala(nombreEmpresa, sala.id!!, sala.nombre, piso.nombre)
                        }
                    }
                    
                    // Borrar las salas que el usuario ha quitado, y marcar las reservas asociadas como huérfanas
                    salasBorradas.toList().forEach { borradaId ->
                        repository.deleteSala(nombreEmpresa, pisoId, borradaId)
                        reservationRepo.markReservationsAsOrphanedBySala(nombreEmpresa, borradaId)
                    }
                    salasBorradas.clear()
                    
                    _saveStatus.value = true
                    loadPisos("EDIT", pisoId)
                } else {
                    _saveStatus.value = false
                }
            } catch (e: Exception) {
                _error.value = "Error al guardar: ${e.message}"
                _saveStatus.value = false
            }
        }
    }

    fun verificarReservasYEliminarPiso(piso: Piso) {
        val nombreEmpresa = Sesion.datos?.empresa?.nombre ?: return
        viewModelScope.launch {
            try {
                val reservas = reservationRepo.getReservationsByFloor(nombreEmpresa, piso.nombre)
                if (reservas.isNotEmpty()) {
                    _confirmacionRequeridaEliminar.value = piso
                } else {
                    eliminarPiso(piso)
                }
            } catch (e: Exception) {
                eliminarPiso(piso)
            }
        }
    }

    fun eliminarPiso(piso: Piso) {
        val nombreEmpresa = Sesion.datos?.empresa?.nombre ?: return
        val pisoId = piso.id ?: return
        viewModelScope.launch {
            try {
                // Borrar imagen de Storage si existe
                if (!piso.imagenUrl.isNullOrEmpty()) {
                    storageRepo.deletePisoImagen(piso.imagenUrl!!)
                }

                if (repository.deletePiso(nombreEmpresa, pisoId)) {
                    // Marcar reservas como huérfanas
                    reservationRepo.markReservationsAsOrphanedByFloor(nombreEmpresa, piso.nombre)
                    _confirmacionRequeridaEliminar.value = null
                    loadPisos()
                }
            } catch (e: Exception) {
                _error.value = "Error al eliminar piso"
            }
        }
    }
}
