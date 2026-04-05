package com.example.tfg_kotlin.ui.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.tfg_kotlin.data.model.Empresa
import com.example.tfg_kotlin.data.repository.FirestoreRepository
import kotlinx.coroutines.launch

class RegistroEmpresaViewModel(
    private val repository: FirestoreRepository = FirestoreRepository()
) : ViewModel() {

    private val _registroState = MutableLiveData<RegistroEmpresaState>()
    val registroState: LiveData<RegistroEmpresaState> = _registroState

    fun registrarEmpresa(nombre: String, dominio: String, cif: String) {
        _registroState.value = RegistroEmpresaState.Loading

        viewModelScope.launch {
            try {
                // Validar si el nombre ya existe
                if (repository.getEmpresaByNombre(nombre) != null) {
                    _registroState.value = RegistroEmpresaState.Error("nombre", "Nombre de empresa ya registrado")
                    return@launch
                }

                // Validar si el dominio ya existe
                if (repository.getEmpresaByDominio(dominio) != null) {
                    _registroState.value = RegistroEmpresaState.Error("dominio", "Dominio ya existente")
                    return@launch
                }

                // Validar si el CIF ya existe
                if (repository.getEmpresaByCif(cif) != null) {
                    _registroState.value = RegistroEmpresaState.Error("cif", "CIF ya existente")
                    return@launch
                }

                val nuevaEmpresa = Empresa(
                    cif = cif,
                    nombre = nombre,
                    dominio = dominio
                )

                val success = repository.saveEmpresa(nuevaEmpresa)
                if (success) {
                    _registroState.value = RegistroEmpresaState.Success
                } else {
                    _registroState.value = RegistroEmpresaState.Error("general", "Error al registrar la empresa")
                }
            } catch (e: Exception) {
                _registroState.value = RegistroEmpresaState.Error("general", e.message ?: "Error en el registro")
            }
        }
    }

    sealed class RegistroEmpresaState {
        object Loading : RegistroEmpresaState()
        object Success : RegistroEmpresaState()
        data class Error(val field: String, val message: String) : RegistroEmpresaState()
    }
}
