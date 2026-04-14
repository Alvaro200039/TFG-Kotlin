package com.example.tfg_kotlin

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.tfg_kotlin.databinding.ActivityMenuEmpleadoBinding
import com.example.tfg_kotlin.ui.viewmodel.MenuViewModel

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
        binding.btnNuevaReserva.setOnClickListener { handleNuevaReserva() }

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
}
