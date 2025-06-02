package com.example.tfg_kotlin.viewmodel

import androidx.lifecycle.*
import com.example.tfg_kotlin.entities.Empresa
import com.example.tfg_kotlin.repository.MasterRepository
import kotlinx.coroutines.launch

class RegistroEmpresaViewModel (private val repository: MasterRepository) : ViewModel() {

    private val _registroExitoso = MutableLiveData<Boolean>()
    val registroExitoso: LiveData<Boolean> = _registroExitoso

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    fun registrarEmpresa(empresa: Empresa) {
        viewModelScope.launch {
            try {
                val existente = repository.obtenerEmpresaPorCif(empresa.cif)
                if (existente != null) {
                    _error.value = "Ya existe una empresa con ese CIF"
                    _registroExitoso.value = false
                    return@launch
                }

                repository.insertarEmpresa(empresa)
                _registroExitoso.value = true
            } catch (e: Exception) {
                _error.value = "Error al registrar: ${e.message}"
                _registroExitoso.value = false
            }
        }
    }

    fun limpiarEstado() {
        _registroExitoso.value = false
        _error.value = null
    }
}