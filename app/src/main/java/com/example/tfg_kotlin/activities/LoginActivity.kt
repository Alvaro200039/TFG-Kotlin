package com.example.tfg_kotlin.activities

import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.ViewModelProvider
import com.example.tfg_kotlin.R
import com.example.tfg_kotlin.database.AppDatabase
import com.example.tfg_kotlin.database.BBDDMaestra
import com.example.tfg_kotlin.repository.EmpleadoRepository
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import androidx.lifecycle.lifecycleScope
import com.example.tfg_kotlin.repository.LoginRepository
import com.example.tfg_kotlin.viewmodel.LoginViewModel
import com.example.tfg_kotlin.viewmodel.LoginViewModelFactory
import kotlinx.coroutines.launch

class LoginActivity : AppCompatActivity() {

    private lateinit var btnFinalizarLogin: Button
    private lateinit var tilCorreo: TextInputLayout
    private lateinit var tilContrasena: TextInputLayout
    private lateinit var etCorreo: TextInputEditText
    private lateinit var etContrasena: TextInputEditText
    private lateinit var tvOlvidarContrasena: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_login)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.loginpersona)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Toolbar
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        inicializarVistas()
        configurarRecuperarContrasena()
        configurarLogin()
    }

    private fun inicializarVistas() {
        btnFinalizarLogin = findViewById(R.id.btnFinalizarLogin)
        tilCorreo = findViewById(R.id.tilCorreo)
        tilContrasena = findViewById(R.id.tilContrasena)
        etCorreo = findViewById(R.id.etCorreo)
        etContrasena = findViewById(R.id.etContrasena)
        tvOlvidarContrasena = findViewById(R.id.tvOlvidarContrasena)
    }

    private fun configurarRecuperarContrasena() {
        tvOlvidarContrasena.setOnClickListener {
            startActivity(Intent(this, RecuperarContrasenaActivity::class.java))
        }
    }

    private fun configurarLogin() {
        btnFinalizarLogin.setOnClickListener {
            val correo = etCorreo.text.toString().trim()
            val contrasena = etContrasena.text.toString().trim()
            val dominioCorreo = "@" + correo.substringAfter("@").lowercase()

            if (correo.isEmpty() || contrasena.isEmpty()) {
                mostrarError("Rellena todos los campos")
                return@setOnClickListener
            }

            lifecycleScope.launch {

                val dbMaestra = BBDDMaestra.getInstance(applicationContext)
                val empresaExiste = dbMaestra.empresaDao().getEmpresaPorDominio(dominioCorreo)

                if (empresaExiste == null) {
                    mostrarError("No existe ninguna empresa registrada con el dominio @$dominioCorreo")
                    return@launch
                }

                // Base de datos específica de la empresa
                val dominioSinArroba = dominioCorreo.substringAfter("@")
                val dbEmpresa = AppDatabase.getInstance(applicationContext, dominioSinArroba)
                val loginRepository = LoginRepository(dbEmpresa.loginDao())
                val viewModelFactory = LoginViewModelFactory(loginRepository)
                val viewModel = ViewModelProvider(
                    this@LoginActivity,
                    viewModelFactory
                )[LoginViewModel::class.java]

                viewModel.usuario.observe(this@LoginActivity) { usuario ->
                    usuario?.let {
                        val destino =
                            if (it.esJefe) JefeActivity::class.java else EmpleadoActivity::class.java
                        Toast.makeText(
                            this@LoginActivity,
                            "Bienvenido ${if (it.esJefe) "jefe" else "empleado"}",
                            Toast.LENGTH_SHORT
                        ).show()
                        startActivity(Intent(this@LoginActivity, destino))
                        finish()
                    }
                }

                viewModel.error.observe(this@LoginActivity) { error ->
                    error?.let {
                        mostrarError(it)
                        viewModel.limpiarEstado()
                    }
                }

                viewModel.login(correo, contrasena)
            }
        }
    }



    private fun mostrarError(mensaje: String) {
        Toast.makeText(this, mensaje, Toast.LENGTH_SHORT).show()
    }

    // Flecha "Atrás"
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when(item.itemId) {
            android.R.id.home -> {
                onBackPressed()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
}