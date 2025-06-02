package com.example.tfg_kotlin.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.tfg_kotlin.repository.RecuperarContrasenaRepository

class RecuperarContrasenaViewModelFactory (private val repository: RecuperarContrasenaRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(RecuperarContrasenaViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return RecuperarContrasenaViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}