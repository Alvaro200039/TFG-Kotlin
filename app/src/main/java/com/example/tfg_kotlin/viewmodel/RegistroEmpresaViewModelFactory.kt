package com.example.tfg_kotlin.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.tfg_kotlin.repository.MasterRepository

class RegistroEmpresaViewModelFactory (private val repository: MasterRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(RegistroEmpresaViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return RegistroEmpresaViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}