package com.example.tfg_kotlin.activities

import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import android.widget.*
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.tfg_kotlin.R
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

class RecuperarContrasenaActivity : AppCompatActivity() {

    private lateinit var layoutPaso1: LinearLayout
    private lateinit var layoutPaso2: LinearLayout
    private lateinit var etCorreo: EditText
    private lateinit var btnEnviarCodigo: Button
    private lateinit var etCodigo: EditText
    private lateinit var etNuevaPass: EditText
    private lateinit var etRepetirPass: EditText
    private lateinit var btnCambiar: Button

    private var codigoGenerado: String = ""
    private var correo: String = ""
    private var cifEmpresa: String = ""
    private var usuarioId: String = ""


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_recuperar_contrasena)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.recuperarcontrasena)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        // Toolbar
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        inicializarVistas()
        configurarPaso1()
        configurarPaso2()
    }

    private fun inicializarVistas() {
        layoutPaso1 = findViewById(R.id.layoutPaso1)
        layoutPaso2 = findViewById(R.id.layoutPaso2)

        etCorreo = findViewById(R.id.etCorreoRec)
        btnEnviarCodigo = findViewById(R.id.btnEnviarCodigo)

        etCodigo = findViewById(R.id.etCodigo)
        etNuevaPass = findViewById(R.id.etNuevaPass)
        etRepetirPass = findViewById(R.id.etRepetirPass)
        btnCambiar = findViewById(R.id.btnCambiarPass)
    }


    private fun configurarPaso1() {
        btnEnviarCodigo.setOnClickListener {
            correo = etCorreo.text.toString().trim().lowercase()

            if (correo.isEmpty() || !correo.contains("@")) {
                etCorreo.error = "Introduce un correo válido"
                return@setOnClickListener
            }

            val db = Firebase.firestore
            val dominioCorreo = "@" + correo.substringAfter("@")

            // Buscar el CIF según el dominio
            db.collection("empresas")
                .whereEqualTo("dominio", dominioCorreo)
                .get()
                .addOnSuccessListener { empresaDocs  ->
                    if (!empresaDocs.isEmpty) {
                        val cif = empresaDocs.documents[0].getString("cif") ?: ""

                        // Buscar el usuario dentro de la subcolección de esa empresa
                        db.collection("empresas")
                            .document(cif)
                            .collection("Usuarios")
                            .whereEqualTo("email", correo)
                            .get()
                            .addOnSuccessListener { userDocs ->
                                Log.d("RecuperarContrasena", "Documentos encontrados: ${userDocs.size()}")
                                if (!userDocs.isEmpty) {
                                    val doc = userDocs.documents[0]
                                    usuarioId = doc.id
                                    cifEmpresa = cif
                                    codigoGenerado = (100000..999999).random().toString()
                                    Snackbar.make(findViewById(android.R.id.content), "Tu código es: $codigoGenerado", Snackbar.LENGTH_INDEFINITE)
                                        .setDuration(10000) // 10 segundos
                                        .show()
                                    layoutPaso1.visibility = LinearLayout.GONE
                                    layoutPaso2.visibility = LinearLayout.VISIBLE
                                } else {
                                    Log.d("RecuperarContrasena", "No hay documentos con ese correo")
                                    etCorreo.error = "No hay ninguna cuenta con este correo"
                                }
                            }
                            .addOnFailureListener {
                                Log.e("RecuperarContrasena", "Error al buscar usuario", it)
                                etCorreo.error = "Error al buscar el usuario"
                            }

                    } else {
                        Log.d("RecuperarContrasena", "No se encontró empresa con dominio: $dominioCorreo")
                        etCorreo.error = "No hay ninguna empresa asociada a ese dominio"
                    }
                }
                .addOnFailureListener {
                    Log.e("RecuperarContrasena", "Error al buscar empresa", it)
                    etCorreo.error = "Error al buscar empresa"
                }
        }

}

    private fun configurarPaso2() {
        btnCambiar.setOnClickListener {
            val codigoIngresado = etCodigo.text.toString().trim()
            val nuevaPass = etNuevaPass.text.toString().trim()
            val repetirPass = etRepetirPass.text.toString().trim()

            if (codigoIngresado != codigoGenerado) {
                etCodigo.error = "El código no es correcto"
                return@setOnClickListener
            }

            if (nuevaPass.length < 6) {
                etNuevaPass.error = "La contraseña debe tener al menos 6 caracteres"
                return@setOnClickListener
            }

            if (nuevaPass != repetirPass) {
                etRepetirPass.error = "Las contraseñas no coinciden"
                return@setOnClickListener
            }

            val db = Firebase.firestore

            db.collection("empresas")
                .document(cifEmpresa)
                .collection("Usuarios")
                .document(usuarioId)
                .update("contrasena", nuevaPass)
                .addOnSuccessListener {
                    Toast.makeText(this, "Contraseña actualizada correctamente", Toast.LENGTH_SHORT)
                        .show()
                    finish()
                }
                .addOnFailureListener {
                        Toast.makeText(this, "Error al actualizar la contraseña", Toast.LENGTH_SHORT).show()
                }
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