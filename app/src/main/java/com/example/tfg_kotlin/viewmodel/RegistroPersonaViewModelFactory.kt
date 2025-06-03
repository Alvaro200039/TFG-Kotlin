package com.example.tfg_kotlin.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.tfg_kotlin.repository.RegistroPersonaRepository

class RegistroPersonaViewModelFactory (
    private val usuarioDao: UsuarioDao,
    private val salaDao: SalaDao,
    private val reservaDao: ReservaDao,
    private val franjaHorariaDao: FranjaHorariaDao,
    private val pisoDao: PisoDao
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(RegistroPersonaViewModel::class.java)) {
            val repository = RegistroPersonaRepository(
                usuarioDao,
                salaDao,
                reservaDao,
                franjaHorariaDao,
                pisoDao
            )
            @Suppress("UNCHECKED_CAST")
            return RegistroPersonaViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
