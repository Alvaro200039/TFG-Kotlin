package com.example.tfg_kotlin.activities

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import com.example.tfg_kotlin.R
import com.example.tfg_kotlin.database.BBDDInstance
import com.example.tfg_kotlin.entities.Empresa
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout

class RegistroEmpresaActivity : AppCompatActivity() {

        private lateinit var tilNombreEmpresa: TextInputLayout
        private lateinit var etNombreEmpresa: TextInputEditText
        private lateinit var tilDominioEmpresa: TextInputLayout
        private lateinit var etDominioEmpresa: TextInputEditText
        private lateinit var tilCif: TextInputLayout
        private lateinit var etCif: TextInputEditText
        private lateinit var btnFinalizarRegistroEmpresa: Button

        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)
            setContentView(R.layout.activity_registro_empresa)

            // Toolbar
            val toolbar = findViewById<Toolbar>(R.id.toolbar)
            setSupportActionBar(toolbar)
            supportActionBar?.setDisplayHomeAsUpEnabled(true)

            inicializarVistas()
            configurarBotonFinalizar()
        }

        private fun inicializarVistas() {
            tilNombreEmpresa = findViewById(R.id.tilNombreEmpresa)
            etNombreEmpresa = findViewById(R.id.etNombreEmpresa)
            tilDominioEmpresa = findViewById(R.id.tilDominio)
            etDominioEmpresa = findViewById(R.id.etDominio)
            tilCif = findViewById(R.id.tilNumEmpresa)
            etCif = findViewById(R.id.etNumEmpresa)
            btnFinalizarRegistroEmpresa = findViewById(R.id.btnFinalizarRegistroEmpresa)
        }

        private fun configurarBotonFinalizar() {
            btnFinalizarRegistroEmpresa.setOnClickListener {
                registrarEmpresa()
            }
        }

        private fun registrarEmpresa() {
            val nombre = etNombreEmpresa.text.toString().trim()
            val dominio = etDominioEmpresa.text.toString().trim().lowercase()
            val cif = etCif.text.toString().trim().uppercase()

            if (!validarCampos(nombre, dominio, cif)) return

            val dbMaestra = BBDDInstance.getDatabase(this, "maestra_db")
            val empresaExistente = dbMaestra.empresaDao().getEmpresaPorDominioEnEmpresa(dominio)

            if (empresaExistente != null) {
                tilDominioEmpresa.error = "El dominio ya está registrado"
                etDominioEmpresa.requestFocus()
                return
            }

            val nuevaEmpresa = Empresa(nombre = nombre, dominio = dominio, cif = cif)
            dbMaestra.empresaDao().insertarEmpresa(nuevaEmpresa)

            val nombreBD = "empresa_${dominio.replace(".", "_")}"
            BBDDInstance.getDatabase(this, nombreBD)

            Toast.makeText(this, "Empresa registrada correctamente", Toast.LENGTH_SHORT).show()
            Log.i("Empresa", "Registrada: $nombre, dominio: $dominio")

            startActivity(Intent(this, RegistroPersonaActivity::class.java))
            finish()
        }

        private fun validarCampos(nombre: String, dominio: String, cif: String): Boolean {
            var esValido = true

            if (nombre.isEmpty()) {
                tilNombreEmpresa.error = "Este campo no puede estar vacío"
                esValido = false
            } else {
                tilNombreEmpresa.error = null
            }

            if (dominio.isEmpty() || !dominio.contains(".")) {
                tilDominioEmpresa.error = "Dominio no válido"
                esValido = false
            } else {
                tilDominioEmpresa.error = null
            }

            if (cif.length != 9) {
                tilCif.error = "CIF inválido"
                esValido = false
            } else {
                tilCif.error = null
            }

            return esValido
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

