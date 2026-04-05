package com.example.tfg_kotlin.ui.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.tfg_kotlin.data.model.Usuario
import com.example.tfg_kotlin.data.repository.FirestoreRepository
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class RegistroViewModel(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val repository: FirestoreRepository = FirestoreRepository()
) : ViewModel() {

    private val _registroState = MutableLiveData<RegistroState>()
    val registroState: LiveData<RegistroState> = _registroState

    fun registrarEmpleado(
        correo: String,
        cifIntroducido: String,
        contrasena: String,
        nombre: String,
        apellidos: String
    ) {
        _registroState.value = RegistroState.Loading

        viewModelScope.launch {
            try {
                val dominio = "@" + correo.substringAfterLast("@")
                val empresa = repository.getEmpresaByDominio(dominio)

                if (empresa == null) {
                    _registroState.value = RegistroState.Error("Dominio de correo no registrado en ninguna empresa")
                    return@launch
                }

                val esJefe = cifIntroducido.equals(empresa.cif, ignoreCase = true)

                // Crear usuario en Auth
                auth.createUserWithEmailAndPassword(correo, contrasena).await()

                val uid = auth.currentUser?.uid ?: ""
                val usuario = Usuario(
                    email = correo,
                    nombre = nombre,
                    apellidos = apellidos,
                    esJefe = esJefe,
                    cif = empresa.cif,
                    uid = uid
                )

                val success = repository.createUsuario(empresa.nombre, usuario)
                if (success) {
                    _registroState.value = RegistroState.Success(esJefe)
                } else {
                    _registroState.value = RegistroState.Error("Error al guardar datos del usuario")
                }

            } catch (e: Exception) {
                _registroState.value = RegistroState.Error(e.message ?: "Error en el registro")
            }
        }
    }

    sealed class RegistroState {
        object Loading : RegistroState()
        data class Success(val esJefe: Boolean) : RegistroState()
        data class Error(val message: String) : RegistroState()
    }
}
