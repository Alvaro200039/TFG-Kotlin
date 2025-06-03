package com.example.tfg_kotlin.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.tfg_kotlin.entities.Empleados
import com.example.tfg_kotlin.entities.Usuario
import com.example.tfg_kotlin.repository.LoginRepository
import kotlinx.coroutines.launch


class LoginViewModel (private val repository: LoginRepository) : ViewModel() {

    private val _usuario = MutableLiveData<Usuario?>()
    val usuario: LiveData<Usuario?> = _usuario

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    fun login(correo: String, contrasena: String) {
        viewModelScope.launch {
            try {
                val resultado = repository.loginUsuario(correo, contrasena)
                if (resultado != null) {
                    _usuario.value = resultado
                } else {
                    _error.value = "Correo o contraseña incorrectos"
                }
            } catch (e: Exception) {
                _error.value = "Error al iniciar sesión: ${e.message}"
            }
        }
    }

    fun limpiarEstado() {
        _usuario.value = null
        _error.value = null
    }
}