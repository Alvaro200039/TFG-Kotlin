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
import com.example.tfg_kotlin.utils.Validaciones
import com.example.tfg_kotlin.database.BBDDInstance
import com.example.tfg_kotlin.entities.Empleados
import com.example.tfg_kotlin.utils.Validaciones.construirNombreBD
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout

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
        val dominio = correo.substringAfter("@").lowercase()
        val cifInput = etCif.text.toString().trim().uppercase()

        Log.d("REGISTRO_PERSONA", "Dominio buscado: $dominio")

        val dbMaestra = BBDDInstance.getDatabase(this, "maestra_db")
        val empresa = dbMaestra.empleadoDao().getEmpresaPorDominioEnEmpresa(dominio)

        if (empresa == null) {
            tilCorreo.error = "No existe ninguna empresa con el dominio @$dominio"
            etCorreo.requestFocus()
            return
        }

        val nombreBD = construirNombreBD(dominio)
        val dbEmpresa = BBDDInstance.getDatabase(this, nombreBD)
        val usuarioExistente = dbEmpresa.empleadoDao().buscarEmpleadoPorCorreo(correo)

        if (usuarioExistente != null) {
            tilCorreo.error = "Este correo ya está registrado"
            etCorreo.requestFocus()
            return
        }

        val esJefe = cifInput.isNotEmpty() && cifInput == empresa.cif

        val empleado = Empleados(
            nombre = etNombre.text.toString(),
            apellidos = etApellidos.text.toString(),
            correo = correo,
            contrasena = etContrasena.text.toString(),
            cif = if (esJefe) cifInput else "",
            esJefe = esJefe
        )

        dbEmpresa.empleadoDao().insertarEmpleado(empleado)

        Toast.makeText(this, "Registro completado", Toast.LENGTH_SHORT).show()

        val intent = if (esJefe) {
            Intent(this, JefeActivity::class.java)
        } else {
            Intent(this, EmpleadoActivity::class.java)
        }

        startActivity(intent)
        finish()
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
