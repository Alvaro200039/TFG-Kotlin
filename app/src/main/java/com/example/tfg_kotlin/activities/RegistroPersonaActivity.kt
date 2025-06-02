package com.example.tfg_kotlin.activities

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import android.widget.Button
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.tfg_kotlin.R
import com.example.tfg_kotlin.database.AppDatabase
import com.example.tfg_kotlin.utils.Validaciones
import com.example.tfg_kotlin.database.BBDDMaestra
import com.example.tfg_kotlin.entities.Empleados
import com.example.tfg_kotlin.utils.Validaciones.construirNombreBD
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import androidx.lifecycle.ViewModelProvider
import com.example.tfg_kotlin.viewmodel.RegistroPersonaViewModel
import com.example.tfg_kotlin.viewmodel.RegistroPersonaViewModelFactory
import com.example.tfg_kotlin.repository.EmpleadoRepository


class RegistroPersonaActivity : AppCompatActivity() {

    // Vistas
    private lateinit var tilNombre: TextInputLayout
    private lateinit var etNombre: TextInputEditText
    private lateinit var tilApellidos: TextInputLayout
    private lateinit var etApellidos: TextInputEditText
    private lateinit var tilCorreo: TextInputLayout
    private lateinit var etCorreo: TextInputEditText
    private lateinit var tilContrasena: TextInputLayout
    private lateinit var etContrasena: TextInputEditText
    private lateinit var tilRepContrasena: TextInputLayout
    private lateinit var etRepContrasena: TextInputEditText
    private lateinit var tilCif: TextInputLayout
    private lateinit var etCif: TextInputEditText
    private lateinit var btnFinalizarRegistro: Button


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_registro_persona)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.registropersona)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        // Toolbar
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        inicializarVistas()
        configurarBotonFinalizar()
    }

    private fun inicializarVistas() {
        btnFinalizarRegistro = findViewById(R.id.btnFinalizarRegistro)

        tilNombre = findViewById(R.id.tilNombre)
        etNombre = findViewById(R.id.etNombre)

        tilApellidos = findViewById(R.id.tilApellidos)
        etApellidos = findViewById(R.id.etApellidos)

        tilCorreo = findViewById(R.id.tilCorreo)
        etCorreo = findViewById(R.id.etCorreo)

        tilContrasena = findViewById(R.id.tilContrasena)
        etContrasena = findViewById(R.id.etContrasena)

        tilRepContrasena = findViewById(R.id.tilRepContrasena)
        etRepContrasena = findViewById(R.id.etRepContrasena)

        tilCif = findViewById(R.id.tilNumEmpresa)
        etCif = findViewById(R.id.etNumEmpresa)
    }

    private fun configurarBotonFinalizar() {
        btnFinalizarRegistro.setOnClickListener {
            if (!validarFormulario()) return@setOnClickListener
            registrarPersona()
        }
    }

    private fun validarFormulario(): Boolean {
        val esValido = Validaciones.validarRegistroPersona(
            tilNombre, etNombre,
            tilApellidos, etApellidos,
            tilCorreo, etCorreo,
            tilContrasena, etContrasena,
            tilRepContrasena, etRepContrasena,
            tilCif, etCif
        )

        if (!esValido) return false

        val contrasena = etContrasena.text.toString().trim()
        if (contrasena.length < 4) {
            tilContrasena.error = "Debe tener al menos 4 caracteres"
            etContrasena.requestFocus()
            return false
        }

        return true
    }

    private fun registrarPersona() {
        val correo = etCorreo.text.toString().trim()
        val dominio = "@" + correo.substringAfter("@").lowercase()
        val cifInput = etCif.text.toString().trim().uppercase()

        Log.d("REGISTRO_PERSONA", "Dominio buscado: $dominio")

        lifecycleScope.launch {
            val dbMaestra = BBDDMaestra.getInstance(applicationContext)
            val empresa = dbMaestra.empresaDao().getEmpresaPorDominio(dominio)

            if (empresa == null) {
                tilCorreo.error = "No existe ninguna empresa con el dominio @$dominio"
                etCorreo.requestFocus()
                return@launch
            }

            val nombreBD = construirNombreBD(dominio)
            val dbEmpresa = AppDatabase.getInstance(applicationContext, nombreBD)
            val repository = EmpleadoRepository(dbEmpresa.empleadoDao())
            val factory = RegistroPersonaViewModelFactory(repository)
            val viewModel = ViewModelProvider(this@RegistroPersonaActivity, factory)[RegistroPersonaViewModel::class.java]

            val esJefe = cifInput.isNotEmpty() && cifInput == empresa.cif

            val empleado = Empleados(
                nombre = etNombre.text.toString(),
                apellidos = etApellidos.text.toString(),
                correo = correo,
                contrasena = etContrasena.text.toString(),
                cif = if (esJefe) cifInput else "",
                esJefe = esJefe
            )

            viewModel.registroExitoso.observe(this@RegistroPersonaActivity) { ok ->
                if (ok) {
                    val intent = if (esJefe) Intent(this@RegistroPersonaActivity, JefeActivity::class.java)
                    else Intent(this@RegistroPersonaActivity, EmpleadoActivity::class.java)
                    startActivity(intent)
                    finish()
                }
            }

            viewModel.error.observe(this@RegistroPersonaActivity) { mensaje ->
                if (mensaje != null) {
                    tilCorreo.error = mensaje
                    viewModel.limpiarEstado()
                }
            }

            viewModel.registrarEmpleado(empleado)
        }
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
