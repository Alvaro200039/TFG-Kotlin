package com.example.tfg_kotlin.ui.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.tfg_kotlin.data.model.Sesion
import com.example.tfg_kotlin.data.model.UserSession
import com.example.tfg_kotlin.data.model.Usuario
import com.example.tfg_kotlin.data.repository.FirestoreRepository
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class LoginViewModel(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val repository: FirestoreRepository = FirestoreRepository()
) : ViewModel() {

    private val _loginState = MutableLiveData<LoginState>()
    val loginState: LiveData<LoginState> = _loginState

    fun login(email: String, contrasena: String) {
        _loginState.value = LoginState.Loading

        auth.signInWithEmailAndPassword(email, contrasena)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val user = auth.currentUser
                    if (user != null && user.email != null) {
                        loadUserData(user.email!!)
                    } else {
                        _loginState.value = LoginState.Error("User not found after login")
                    }
                } else {
                    _loginState.value = LoginState.Error(task.exception?.message ?: "Login failed")
                }
            }
    }

    fun autoLoginIfRecordado(email: String) {
        val userActivo = auth.currentUser
        if (userActivo != null && userActivo.email == email) {
            loadUserData(email)
        }
    }

    private fun loadUserData(email: String) {
        viewModelScope.launch {
            try {
                _loginState.value = LoginState.Loading
                
                val usuario = repository.getUsuarioByEmail(email) ?: run {
                    _loginState.value = LoginState.Error("Usuario no encontrado en base de datos")
                    return@launch
                }

                val empresa = repository.getEmpresaByUsuarioEmail(email) ?: run {
                    _loginState.value = LoginState.Error("Empresa no encontrada para este usuario")
                    return@launch
                }

                val listaPisos = repository.getPisosByEmpresa(empresa.nombre)
                val listaFranjas = repository.getFranjasByEmpresa(empresa.nombre)

                val session = UserSession(
                    empresa = empresa,
                    usuario = usuario,
                    pisos = listaPisos,
                    franjasHorarias = listaFranjas
                )
                
                Sesion.datos = session
                _loginState.value = LoginState.Success(usuario)

            } catch (e: Exception) {
                _loginState.value = LoginState.Error(e.message ?: "Error al cargar datos del usuario")
            }
        }
    }

    sealed class LoginState {
        object Loading : LoginState()
        data class Success(val usuario: Usuario) : LoginState()
        data class Error(val message: String) : LoginState()
    }
}
