package com.example.tfg_kotlin

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.example.tfg_kotlin.data.model.Sesion
import com.example.tfg_kotlin.databinding.ActivityMenuEmpleadoBinding
import com.example.tfg_kotlin.ui.viewmodel.MenuViewModel
import kotlinx.coroutines.launch

class MenuEmpleadoActivity : BaseMenuActivity() {

    private lateinit var binding: ActivityMenuEmpleadoBinding
    override val menuViewModel: MenuViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMenuEmpleadoBinding.inflate(layoutInflater)
        setContentView(binding.root)

        enableEdgeToEdge()
        ViewCompat.setOnApplyWindowInsetsListener(binding.menuEmpleadoLayout) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        setupToolbar()
        setupObservers()
        setupListeners()
        
        setupBaseObservers(binding.textProximaSala, binding.cardProximaSala, binding.textProximaPuesto, binding.cardProximaPuesto, binding.cardContenedorProximas)
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(false) // Redundant with logout button
            title = ""
        }
    }

    private fun setupObservers() {
        menuViewModel.usuario.observe(this) { user ->
            user?.let {
                binding.textTitulo.text = getString(R.string.label_user_name_format, it.nombre, it.apellidos)
                binding.textRol.text = if (it.esJefe) getString(R.string.label_rol_admin) else getString(R.string.label_rol_empleado)
            }
        }

        menuViewModel.error.observe(this) { msg ->
            Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupListeners() {
        binding.btnNuevaReserva.setOnClickListener {
            handleNuevaReserva()
        }

        binding.btnVerReservas.setOnClickListener {
            mostrarDialogoReservas()
        }
        
        binding.btnFranjas.setOnClickListener {
            cargarFranjas()
        }

        binding.btnLogout.setOnClickListener {
            menuViewModel.logout()
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }
    }

    private fun handleNuevaReserva() {
        lifecycleScope.launch {
            try {
                val sesion = Sesion.datos ?: return@launch
                val empresaId = sesion.empresa.nombre
                
                if (empresaId.isEmpty()) {
                    Toast.makeText(this@MenuEmpleadoActivity, getString(R.string.err_empresa_no_definida), Toast.LENGTH_SHORT).show()
                    return@launch
                }

                val empresaActualizada = firestoreRepo.getEmpresaByNombre(empresaId)
                if (empresaActualizada == null) {
                    Toast.makeText(this@MenuEmpleadoActivity, getString(R.string.err_empresa_no_encontrada), Toast.LENGTH_SHORT).show()
                    return@launch
                }
                sesion.empresa = empresaActualizada

                val pisos = firestoreRepo.getPisosByEmpresa(empresaId)
                if (pisos.isNotEmpty()) {
                    sesion.pisos = listOf(pisos.last())
                    startActivity(Intent(this@MenuEmpleadoActivity, EmpleadosActivity::class.java))
                } else {
                    Toast.makeText(this@MenuEmpleadoActivity, getString(R.string.msg_no_piso_creado), Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@MenuEmpleadoActivity, getString(R.string.err_cargar_pisos), Toast.LENGTH_SHORT).show()
                e.printStackTrace()
            }
        }
    }
}
