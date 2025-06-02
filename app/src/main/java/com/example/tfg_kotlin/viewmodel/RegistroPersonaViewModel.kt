package com.example.tfg_kotlin.viewmodel

import androidx.lifecycle.*
import com.example.tfg_kotlin.entities.Empleados
import com.example.tfg_kotlin.repository.EmpleadoRepository
import kotlinx.coroutines.launch

class RegistroPersonaViewModel (private val repository: EmpleadoRepository) : ViewModel() {

    private val _registroExitoso = MutableLiveData<Boolean>()
    val registroExitoso: LiveData<Boolean> = _registroExitoso

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    fun registrarEmpleado(empleado: Empleados) {
        viewModelScope.launch {
            try {
                val existente = repository.obtenerEmpleadoPorCorreo(empleado.correo)
                if (existente != null) {
                    _error.value = "El correo ya está registrado"
                    _registroExitoso.value = false
                    return@launch
                }

                repository.insertarEmpleado(empleado)
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