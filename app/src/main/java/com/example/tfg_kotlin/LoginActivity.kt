package com.example.tfg_kotlin

import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.tfg_kotlin.data.model.Usuario
import androidx.core.content.edit
import androidx.activity.viewModels
import com.example.tfg_kotlin.databinding.ActivityLoginBinding
import com.example.tfg_kotlin.ui.viewmodel.LoginViewModel

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private val viewModel: LoginViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        enableEdgeToEdge()
        ViewCompat.setOnApplyWindowInsetsListener(binding.loginLayout) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        setupToolbar()
        setupObservers()
        setupListeners()
        checkRememberedUser()
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            title = getString(R.string.title_inicio_sesion)
            setHomeAsUpIndicator(R.drawable.ic_adagora_nav)
        }
    }

    private fun setupObservers() {
        viewModel.loginState.observe(this) { state ->
            when (state) {
                is LoginViewModel.LoginState.Loading -> {
                    binding.btnLogin.isEnabled = false
                    // Podríamos mostrar un ProgressBar aquí
                }
                is LoginViewModel.LoginState.Success -> {
                    binding.btnLogin.isEnabled = true
                    handleLoginSuccess(state.usuario)
                }
                is LoginViewModel.LoginState.Error -> {
                    binding.btnLogin.isEnabled = true
                    Toast.makeText(this, state.message, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun setupListeners() {
        binding.btnLogin.setOnClickListener { iniciarSesion() }
        binding.btnRegistro.setOnClickListener {
            startActivity(Intent(this, RegistroEmpleadoActivity::class.java))
        }
        binding.tvOlvidarContrasena.setOnClickListener {
            startActivity(Intent(this, RecuperarContrasenaActivity::class.java))
        }
    }

    private fun checkRememberedUser() {
        val prefs = getSharedPreferences("loginPrefs", MODE_PRIVATE)
        val recordar = prefs.getBoolean("recordar", false)
        val correoGuardado = prefs.getString("correo", "")

        if (recordar && !correoGuardado.isNullOrEmpty()) {
            binding.etCorreo.setText(correoGuardado)
            binding.cbRecordar.isChecked = true
            viewModel.autoLoginIfRecordado(correoGuardado)
        }
    }

    private fun iniciarSesion() {
        val correo = binding.etCorreo.text.toString().trim()
        val contrasena = binding.etContrasena.text.toString()

        if (correo.isEmpty() || contrasena.isEmpty()) {
            Toast.makeText(this, getString(R.string.err_completar_campos), Toast.LENGTH_SHORT).show()
            return
        }

        if (!correo.contains("@") || !correo.contains(".")) {
            Toast.makeText(this, getString(R.string.err_correo_invalido), Toast.LENGTH_SHORT).show()
            return
        }

        // Manejo de SharedPreferences antes de llamar al ViewModel
        val prefs = getSharedPreferences("loginPrefs", MODE_PRIVATE)
        if (binding.cbRecordar.isChecked) {
            prefs.edit {
                putString("correo", correo)
                putBoolean("recordar", true)
            }
        } else {
            prefs.edit {
                clear()
            }
        }

        viewModel.login(correo, contrasena)
    }

    private fun handleLoginSuccess(usuario: Usuario) {
        val intent = if (usuario.esJefe) {
            Intent(this, MenuCreadorActivity::class.java)
        } else {
            Intent(this, MenuEmpleadoActivity::class.java)
        }
        startActivity(intent)
        finish()
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

