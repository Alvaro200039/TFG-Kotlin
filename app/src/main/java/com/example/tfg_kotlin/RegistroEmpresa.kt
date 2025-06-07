package com.example.tfg_kotlin

import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.content.edit
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

class RegistroEmpresa : AppCompatActivity() {
    private lateinit var editNombre: EditText
    private lateinit var editDominio: EditText
    private lateinit var editCif: EditText
    private lateinit var btnRegistrar: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_registro_empresa)

        enableEdgeToEdge()
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.registroempresaLayout)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Toolbar
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        editNombre = findViewById(R.id.editNombreEmpresa)
        editDominio = findViewById(R.id.editDominio)
        editCif = findViewById(R.id.editCif)
        btnRegistrar = findViewById(R.id.btnRegistrarEmpresa)

        btnRegistrar.setOnClickListener {
            registrarEmpresa()
        }
    }

    private fun registrarEmpresa() {
        val nombre = editNombre.text.toString().trim()
        val dominio = editDominio.text.toString().trim()
        val cif = editCif.text.toString().trim().uppercase()

        if (nombre.isEmpty() || dominio.isEmpty() || cif.isEmpty()) {
            Toast.makeText(this, "Completa todos los campos", Toast.LENGTH_SHORT).show()
            return
        }

        if (!dominio.startsWith("@") || !dominio.contains(".")) {
            Toast.makeText(this, "Dominio inválido (ej: @empresa.com)", Toast.LENGTH_SHORT).show()
            return
        }

        val firestore = Firebase.firestore
        val empresasRef = firestore.collection("empresas")

        // Primero comprobamos si ya existe empresa con ese CIF
        empresasRef.whereEqualTo("nombre", nombre).get()
            .addOnSuccessListener { nombreSnapshot ->
                if (!nombreSnapshot.isEmpty) {
                    Toast.makeText(this, "Nombre de empresa ya registrado", Toast.LENGTH_SHORT).show()
                    editNombre.requestFocus()
                } else {
                    // Comprobamos si el dominio ya existe
                    empresasRef.whereEqualTo("dominio", dominio).get()
                        .addOnSuccessListener { dominioSnapshot ->
                            if (!dominioSnapshot.isEmpty) {
                                Toast.makeText(this, "Dominio existente", Toast.LENGTH_SHORT).show()
                                editDominio.requestFocus()
                            } else {
                                empresasRef.whereEqualTo("cif", cif).get()
                                    .addOnSuccessListener { cifSnapshot ->
                                        if (!cifSnapshot.isEmpty){
                                            Toast.makeText(this, "CIF existente", Toast.LENGTH_SHORT).show()
                                            editCif.requestFocus()
                                        } else {
                                            // Insertamos nueva empresa en Firestore
                                            val empresaMap = hashMapOf(
                                                "cif" to cif,
                                                "nombre" to nombre,
                                                "dominio" to dominio
                                            )

                                            empresasRef.document(nombre).set(empresaMap)
                                                .addOnSuccessListener {
                                                    Toast.makeText(
                                                        this,
                                                        "Empresa registrada",
                                                        Toast.LENGTH_SHORT
                                                    ).show()
                                                    editCif.text.clear()
                                                    editNombre.text.clear()
                                                    editDominio.text.clear()
                                                    Log.d(
                                                        "DB_LOG",
                                                        "Empresa registrada: CIF=$cif, Nombre=$nombre, Dominio=$dominio"
                                                    )
                                                }
                                                .addOnFailureListener { e ->
                                                    Toast.makeText(this, "Error al registrar empresa", Toast.LENGTH_SHORT).show()
                                                    e.printStackTrace()
                                                }
                                        }
                                    }
                                    .addOnFailureListener { e ->
                                        Toast.makeText(this, "Error validando CIF", Toast.LENGTH_SHORT).show()
                                        e.printStackTrace()
                                    }
                            }
                        }
                        .addOnFailureListener { e ->
                            Toast.makeText(this, "Error validando dominio", Toast.LENGTH_SHORT).show()
                            e.printStackTrace()
                        }
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error validando nombre de empresa", Toast.LENGTH_SHORT).show()
                e.printStackTrace()
            }
    }

    // Flecha "Atrás"
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                onBackPressed()
                true
            }

            else -> super.onOptionsItemSelected(item)
        }
    }
}
