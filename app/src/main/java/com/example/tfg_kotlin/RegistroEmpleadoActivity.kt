package com.example.tfg_kotlin

import android.os.Bundle
import android.view.MenuItem
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.activity.viewModels
import com.example.tfg_kotlin.databinding.ActivityRegistroBinding
import com.example.tfg_kotlin.ui.viewmodel.RegistroViewModel

class RegistroEmpleadoActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRegistroBinding
    private val viewModel: RegistroViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegistroBinding.inflate(layoutInflater)
        setContentView(binding.root)

        enableEdgeToEdge()
        ViewCompat.setOnApplyWindowInsetsListener(binding.registroEmpleadoLayout) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        setupToolbar()
        setupObservers()
        setupListeners()
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            title = getString(R.string.title_registro_empleados)
            setHomeAsUpIndicator(R.drawable.ic_adagora_nav)
        }
    }

    private fun setupObservers() {
        viewModel.registroState.observe(this) { state ->
            when (state) {
                is RegistroViewModel.RegistroState.Loading -> {
                    binding.btnRegistrar.isEnabled = false
                }
                is RegistroViewModel.RegistroState.Success -> {
                    binding.btnRegistrar.isEnabled = true
                    if (state.esJefe) {
                        mostrarToast(getString(R.string.msg_empleado_registrado_jefe))
                    } else {
                        mostrarToast(getString(R.string.msg_empleado_registrado))
                    }
                    limpiarCampos()
                }
                is RegistroViewModel.RegistroState.Error -> {
                    binding.btnRegistrar.isEnabled = true
                    mostrarToast(state.message)
                }
            }
        }
    }

    private fun setupListeners() {
        binding.btnRegistrar.setOnClickListener { registrarEmpleado() }
    }

    private fun registrarEmpleado() {
        val correo = binding.etCorreo.text.toString().trim()
        val cifIntroducido = binding.etCif.text.toString().trim()
        val contrasena = binding.etContrasena.text.toString()
        val repetirContrasena = binding.etRepetirContrasena.text.toString()
        val nombre = binding.etNombre.text.toString().trim()
        val apellidos = binding.etApellidos.text.toString().trim()

        if (correo.isEmpty() || contrasena.isEmpty() || repetirContrasena.isEmpty() || nombre.isEmpty() || apellidos.isEmpty()) {
            mostrarToast(getString(R.string.err_completar_campos))
            return
        }

        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(correo).matches()) {
            mostrarToast(getString(R.string.err_correo_invalido))
            return
        }

        if (contrasena != repetirContrasena) {
            mostrarToast(getString(R.string.err_contrasenas_no_coinciden))
            return
        }

        viewModel.registrarEmpleado(correo, cifIntroducido, contrasena, nombre, apellidos)
    }

    private fun mostrarToast(mensaje: String) {
        Toast.makeText(this, mensaje, Toast.LENGTH_SHORT).show()
    }

    private fun limpiarCampos() {
        binding.etCorreo.text?.clear()
        binding.etContrasena.text?.clear()
        binding.etRepetirContrasena.text?.clear()
        binding.etNombre.text?.clear()
        binding.etApellidos.text?.clear()
        binding.etCif.text?.clear()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                onBackPressedDispatcher.onBackPressed()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
}

