package com.example.tfg_kotlin.activities


import android.os.Bundle
import android.view.MenuItem
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import com.example.tfg_kotlin.R
import com.example.tfg_kotlin.entities.Empresa
import com.example.tfg_kotlin.utils.Validaciones.validarRegistroEmpresa
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.google.firebase.firestore.FirebaseFirestore

class RegistroEmpresaActivity : AppCompatActivity() {

        private lateinit var tilNombreEmpresa: TextInputLayout
        private lateinit var etNombreEmpresa: TextInputEditText
        private lateinit var tilDominioEmpresa: TextInputLayout
        private lateinit var etDominioEmpresa: TextInputEditText
        private lateinit var tilCif: TextInputLayout
        private lateinit var etCif: TextInputEditText
        private lateinit var btnFinalizarRegistroEmpresa: Button
        private val db = FirebaseFirestore.getInstance()

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
            var dominio = etDominioEmpresa.text.toString().trim().lowercase()
            if (!dominio.startsWith("@")) {
                dominio = "@$dominio"
            }
            val cif = etCif.text.toString().trim().uppercase()

            if (!validarRegistroEmpresa(tilNombreEmpresa, etNombreEmpresa,
                    tilDominioEmpresa, etDominioEmpresa,
                    tilCif, etCif
                )) return
            val empresa = Empresa(
                cif = cif,
                nombre = nombre,
                dominio = dominio
            )
            val empresasRef = db.collection("empresas")

            empresasRef
                .whereEqualTo("dominio", dominio)
                .get()
                .addOnSuccessListener { querySnapshot ->
                    if (!querySnapshot.isEmpty) {
                        tilDominioEmpresa.error = "Este dominio ya existe"
                    } else {
                        empresasRef.document(cif).set(empresa)
                            .addOnSuccessListener {
                                Toast.makeText(
                                    this,
                                    "Empresa registrada correctamente",
                                    Toast.LENGTH_SHORT
                                ).show()
                                finish()
                            }
                            .addOnFailureListener {
                                Toast.makeText(
                                    this,
                                    "Error al registrar la empresa",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                    }
                }
                .addOnFailureListener {
                    Toast.makeText(this, "Error al comprobar el dominio", Toast.LENGTH_SHORT).show()
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

