package com.example.tfg_kotlin

import android.os.Bundle
import android.view.MenuItem
import android.widget.Button
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.room.Room
import com.example.tfg_kotlin.BBDD.BBDD
import com.example.tfg_kotlin.BBDD.Empleados
import com.example.tfg_kotlin.BBDD.Empresa
import com.example.tfg_kotlin.Validaciones.construirNombreBD
import com.example.tfg_kotlin.Validaciones.validarRegistroEmpresa
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout

class RegistroEmpresaActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_registro_empresa)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.registroempresa)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Toolbar
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        // Referencias a vistas
        val btnFinalizarRegistro = findViewById<Button>(R.id.btnFinalizarRegistroEmpresa)

        val tilNombreEmpresa = findViewById<TextInputLayout>(R.id.tilNombreEmpresa)
        val tilDominio = findViewById<TextInputLayout>(R.id.tilDominio)
        val tilContrasena = findViewById<TextInputLayout>(R.id.tilContrasena)
        val tilRepContrasena = findViewById<TextInputLayout>(R.id.tilRepContrasena)
        val tilNumEmpresa = findViewById<TextInputLayout>(R.id.tilNumEmpresa)

        val etNombreEmpresa = findViewById<TextInputEditText>(R.id.etNombreEmpresa)
        val etDominio = findViewById<TextInputEditText>(R.id.etDominio)
        val etContrasena = findViewById<TextInputEditText>(R.id.etContrasena)
        val etRepContrasena = findViewById<TextInputEditText>(R.id.etRepContrasena)
        val etNumEmpresa = findViewById<TextInputEditText>(R.id.etNumEmpresa)

        btnFinalizarRegistro.setOnClickListener {
            val nombreEmpresa = etNombreEmpresa.text.toString().trim()
            val cif = etNumEmpresa.text.toString().trim().uppercase()
            val dominio = etDominio.text.toString().trim().lowercase()
            val contrasena = etContrasena.text.toString()
            val repetir = etRepContrasena.text.toString()

            // Validaciones
            val esValido = validarRegistroEmpresa(
                tilNombreEmpresa, etNombreEmpresa,
                tilDominio, etDominio,
                tilContrasena, etContrasena,
                tilRepContrasena, etRepContrasena,
                tilNumEmpresa, etNumEmpresa,
            )

            if (!esValido) return@setOnClickListener

            if (contrasena != repetir) {
                tilRepContrasena.error = "Las contraseñas no coinciden"
                return@setOnClickListener
            }

            val dbMaestra = Room.databaseBuilder(
                applicationContext,
                BBDD::class.java,
                "maestra_db"
            ).allowMainThreadQueries().build()

            // Comprobar si ya existe ese CIF
            val empresaExistente = dbMaestra.appDao().getEmpresaPorCif(cif)
            if (empresaExistente != null) {
                tilNumEmpresa.error = "Ya existe una empresa con este CIF"
                return@setOnClickListener
            }

            // Guardar la empresa
            val empresa = Empresa(nombreEmpresa, dominio, cif)
            dbMaestra.appDao().insertarEmpresa(empresa)

            // Crear base de datos específica de la empresa
            val nombreBD = construirNombreBD(dominio)
            Room.databaseBuilder(applicationContext, BBDD::class.java, nombreBD)
                .allowMainThreadQueries()
                .build()

            Toast.makeText(this, "Empresa registrada correctamente", Toast.LENGTH_SHORT).show()
            finish()
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
