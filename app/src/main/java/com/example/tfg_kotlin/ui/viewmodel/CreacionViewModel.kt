package com.example.tfg_kotlin.ui.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.tfg_kotlin.data.model.Piso
import com.example.tfg_kotlin.data.model.Sala
import com.example.tfg_kotlin.data.model.Sesion
import com.example.tfg_kotlin.data.repository.FirestoreRepository
import kotlinx.coroutines.launch

class CreacionViewModel(
    private val repository: FirestoreRepository = FirestoreRepository()
) : ViewModel() {

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

    private val _franjas = MutableLiveData<List<String>>()
    val franjas: LiveData<List<String>> = _franjas

    fun loadPisos() {
        val nombreEmpresa = Sesion.datos?.empresa?.nombre ?: return
        viewModelScope.launch {
            try {
                val listaPisos = repository.getPisosByEmpresa(nombreEmpresa)
                _pisos.value = listaPisos
                if (listaPisos.isNotEmpty()) {
                    setPisoActual(listaPisos.last())
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

    fun updatePisoActual(nuevoNombre: String) {
        val current = _pisoActual.value
        val cif = Sesion.datos?.empresa?.cif ?: ""
        
        if (current == null) {
            _pisoActual.value = Piso(nombre = nuevoNombre, empresaCif = cif)
        } else {
            _pisoActual.value = current.copy(nombre = nuevoNombre)
        }
    }

    fun addSala(sala: Sala) {
        val currentSalas = _salas.value?.toMutableList() ?: mutableListOf()
        currentSalas.add(sala)
        _salas.value = currentSalas
    }

    fun removeSala(sala: Sala) {
        val currentSalas = _salas.value?.toMutableList() ?: mutableListOf()
        currentSalas.remove(sala)
        _salas.value = currentSalas
    }

    fun updateSala(oldSala: Sala, newSala: Sala) {
        val currentSalas = _salas.value?.toMutableList() ?: mutableListOf()
        val index = currentSalas.indexOf(oldSala)
        if (index != -1) {
            currentSalas[index] = newSala
            _salas.value = currentSalas
        }
    }

    fun guardarDistribucion() {
        val piso = _pisoActual.value ?: return
        val salas = _salas.value ?: emptyList()
        val nombreEmpresa = Sesion.datos?.empresa?.nombre ?: return

        viewModelScope.launch {
            try {
                // Aquí iría la lógica de guardado que antes estaba en la Activity
                // Invocando a repository.savePiso y repository.saveSala
                // Por ahora simplificamos a una sola llamada si el repo lo permite o varias
                
                val pisoId = repository.savePiso(nombreEmpresa, piso)
                if (pisoId != null) {
                    // Guardar cada sala
                    salas.forEach { sala ->
                        repository.saveSala(nombreEmpresa, pisoId, sala)
                    }
                    _saveStatus.value = true
                } else {
                    _saveStatus.value = false
                }
            } catch (e: Exception) {
                _error.value = "Error al guardar: ${e.message}"
                _saveStatus.value = false
            }
        }
    }

    fun eliminarPiso(piso: Piso) {
        val nombreEmpresa = Sesion.datos?.empresa?.nombre ?: return
        val pisoId = piso.id ?: return
        viewModelScope.launch {
            try {
                if (repository.deletePiso(nombreEmpresa, pisoId)) {
                    loadPisos()
                }
            } catch (e: Exception) {
                _error.value = "Error al eliminar piso"
            }
        }
    }

    fun loadFranjas() {
        val nombreEmpresa = Sesion.datos?.empresa?.nombre ?: return
        viewModelScope.launch {
            try {
                val lista = repository.getFranjasByEmpresa(nombreEmpresa)
                _franjas.value = lista.map { it.hora }.sorted()
            } catch (e: Exception) {
                _error.value = "Error al cargar franjas horarias"
            }
        }
    }

    fun addFranja(hora: String) {
        val nombreEmpresa = Sesion.datos?.empresa?.nombre ?: return
        viewModelScope.launch {
            try {
                if (repository.addFranja(nombreEmpresa, hora)) {
                    loadFranjas()
                } else {
                    _error.value = "Error al añadir franja"
                }
            } catch (e: Exception) {
                _error.value = "Error al conectar con el servidor"
            }
        }
    }

    fun removeFranja(hora: String) {
        val nombreEmpresa = Sesion.datos?.empresa?.nombre ?: return
        viewModelScope.launch {
            try {
                if (repository.deleteFranja(nombreEmpresa, hora)) {
                    loadFranjas()
                } else {
                    _error.value = "Error al eliminar franja"
                }
            } catch (e: Exception) {
                _error.value = "Error al conectar con el servidor"
            }
        }
    }
}
