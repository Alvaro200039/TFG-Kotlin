package com.example.tfg_kotlin

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.tfg_kotlin.databinding.ActivityMenuCreadorBinding
import com.example.tfg_kotlin.data.model.Sesion
import com.example.tfg_kotlin.ui.viewmodel.MenuViewModel

class MenuCreadorActivity : BaseMenuActivity() {

    private lateinit var binding: ActivityMenuCreadorBinding
    override val menuViewModel: MenuViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMenuCreadorBinding.inflate(layoutInflater)
        setContentView(binding.root)

        enableEdgeToEdge()
        ViewCompat.setOnApplyWindowInsetsListener(binding.menuCreacionLayout) { v, insets ->
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
            setDisplayHomeAsUpEnabled(false) // Redundant with Logout button
            title = ""
        }
    }

    private fun setupObservers() {
        menuViewModel.usuario.observe(this) { user ->
            user?.let {
                binding.textEmpresa.text = Sesion.datos?.empresa?.nombre ?: ""
                binding.textTitulo.text = getString(R.string.label_user_name_format, it.nombre, it.apellidos)
                binding.textRol.text = if (it.esJefe) getString(R.string.label_rol_admin) else getString(R.string.label_rol_empleado)
            }
        }

        menuViewModel.error.observe(this) { msg ->
            Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupListeners() {
        binding.btnEditarSalas.setOnClickListener {
            startActivity(Intent(this, CreacionActivity::class.java))
        }

        binding.btnNuevaReserva.setOnClickListener { handleNuevaReserva() }

        binding.btnVerReservas.setOnClickListener {
            mostrarDialogoReservas()
        }

        binding.btnFranjas.setOnClickListener {
            cargarFranjas()
        }

        binding.btnExtras.setOnClickListener {
            startActivity(Intent(this, GestionExtrasActivity::class.java))
        }

        binding.btnLogout.setOnClickListener {
            menuViewModel.logout()
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }
    }
}
