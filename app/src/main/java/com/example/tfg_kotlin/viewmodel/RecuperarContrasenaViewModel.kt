package com.example.tfg_kotlin.viewmodel

import androidx.lifecycle.*
import com.example.tfg_kotlin.repository.RecuperarContrasenaRepository
import kotlinx.coroutines.launch

class RecuperarContrasenaViewModel (private val repository: RecuperarContrasenaRepository) : ViewModel() {

    private val _codigoGenerado = MutableLiveData<String?>()
    val codigoGenerado: LiveData<String?> = _codigoGenerado

    private val _correoValido = MutableLiveData<Boolean>()
    val correoValido: LiveData<Boolean> = _correoValido

    private val _cambioExitoso = MutableLiveData<Boolean>()
    val cambioExitoso: LiveData<Boolean> = _cambioExitoso

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    fun verificarCorreo(correo: String) {
        viewModelScope.launch {
            val usuario = repository.buscarPorCorreo(correo)
            if (usuario != null) {
                val codigo = (100000..999999).random().toString()
                _codigoGenerado.value = codigo
                _correoValido.value = true
            } else {
                _error.value = "No hay ninguna cuenta con este correo"
                _correoValido.value = false
            }
        }
    }

    fun cambiarContrasena(correo: String, nuevaPass: String) {
        viewModelScope.launch {
            try {
                repository.actualizarContrasena(correo, nuevaPass)
                _cambioExitoso.value = true
            } catch (e: Exception) {
                _error.value = "Error al actualizar la contraseña"
                _cambioExitoso.value = false
            }
        }
    }

    fun limpiarEstado() {
        _codigoGenerado.value = null
        _error.value = null
        _cambioExitoso.value = false
    }
}