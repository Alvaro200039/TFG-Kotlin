package com.example.tfg_kotlin


import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import com.example.tfg_kotlin.BBDD_Global.Entities.Empresa
import com.example.tfg_kotlin.utils.Validaciones.validarRegistroEmpresa
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.google.android.material.button.MaterialButton
import com.google.firebase.firestore.FirebaseFirestore

class RegistroEmpresa : AppCompatActivity() {

    private lateinit var tilNombreEmpresa: TextInputLayout
    private lateinit var etNombreEmpresa: TextInputEditText
    private lateinit var tilDominioEmpresa: TextInputLayout
    private lateinit var etDominioEmpresa: TextInputEditText
    private lateinit var tilCif: TextInputLayout
    private lateinit var etCif: TextInputEditText
    private lateinit var btnRegistrarEmpresa: MaterialButton
    private val firestore = FirebaseFirestore.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_registro_empresa)

        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        inicializarVistas()
        btnRegistrarEmpresa.setOnClickListener { registrarEmpresa() }
    }

    private fun inicializarVistas() {
        tilNombreEmpresa = findViewById(R.id.tilNombreEmpresa)
        etNombreEmpresa = findViewById(R.id.editNombreEmpresa)
        tilDominioEmpresa = findViewById(R.id.tilDominio)
        etDominioEmpresa = findViewById(R.id.editDominio)
        tilCif = findViewById(R.id.tilCif)
        etCif = findViewById(R.id.editCif)
        btnRegistrarEmpresa = findViewById(R.id.btnRegistrarEmpresa)
    }

    private fun registrarEmpresa() {
        val nombre = etNombreEmpresa.text.toString().trim()
        var dominio = etDominioEmpresa.text.toString().trim().lowercase()
        val cif = etCif.text.toString().trim().uppercase()

        if (!dominio.startsWith("@")) {
            dominio = "@$dominio"
        }

        if (!validarRegistroEmpresa(
                tilNombreEmpresa, etNombreEmpresa,
                tilDominioEmpresa, etDominioEmpresa,
                tilCif, etCif
            )) return

        val empresa = Empresa(
            cif = cif,
            nombre = nombre,
            dominio = dominio
        )

        val empresasRef = firestore.collection("empresas")

        empresasRef.document(cif).get()
            .addOnSuccessListener { docCif ->
                if (docCif.exists()) {
                    tilCif.error = "Este CIF ya está registrado"
                    return@addOnSuccessListener
                }

                empresasRef.whereEqualTo("dominio", dominio).get()
                    .addOnSuccessListener { querySnapshot ->
                        if (!querySnapshot.isEmpty) {
                            tilDominioEmpresa.error = "Este dominio ya existe"
                        } else {
                            empresasRef.document(nombre).set(empresa)
                                .addOnSuccessListener {
                                    Toast.makeText(
                                        this,
                                        "Empresa registrada correctamente",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                    limpiarCampos()
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
                        Toast.makeText(
                            this,
                            "Error al comprobar el dominio",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
            }
            .addOnFailureListener {
                Toast.makeText(this, "Error al comprobar el CIF", Toast.LENGTH_SHORT).show()
            }
    }

    private fun limpiarCampos() {
        etNombreEmpresa.text?.clear()
        etDominioEmpresa.text?.clear()
        etCif.text?.clear()
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
}
