package com.example.tfg_kotlin.activities

import android.os.Bundle
import android.view.MenuItem
import android.widget.*
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.tfg_kotlin.R
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
            correo = etCorreo.text.toString().trim()

            if (correo.isEmpty() || !correo.contains("@")) {
                etCorreo.error = "Introduce un correo válido"
                return@setOnClickListener
            }

            val dominio = correo.substringAfter("@")
            val dbName = dominio.replace(".", "_")
            val db = Firebase.firestore

            db.collection("usuarios_$dbName")
                .whereEqualTo("email", correo)
                .get()
                .addOnSuccessListener { documents ->
                    if (!documents.isEmpty) {
                        codigoGenerado = (100000..999999).random().toString()
                        Toast.makeText(this, "Tu código es: $codigoGenerado", Toast.LENGTH_LONG).show()
                        layoutPaso1.visibility = LinearLayout.GONE
                        layoutPaso2.visibility = LinearLayout.VISIBLE
                    } else {
                        etCorreo.error = "No hay ninguna cuenta con este correo"
                    }
                }
                .addOnFailureListener {
                    etCorreo.error = "Error al buscar el usuario"
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

            val dominio = correo.substringAfter("@")
            val dbName = dominio.replace(".", "_")
            val db = Firebase.firestore

            db.collection("usuarios_$dbName")
                .whereEqualTo("email", correo)
                .get()
                .addOnSuccessListener { result ->
                    if (!result.isEmpty) {
                        val docId = result.documents[0].id
                        db.collection("usuarios_$dbName")
                            .document(docId)
                            .update("contrasena", nuevaPass)
                            .addOnSuccessListener {
                                Toast.makeText(this, "Contraseña actualizada correctamente", Toast.LENGTH_SHORT).show()
                                finish()
                            }
                            .addOnFailureListener {
                                Toast.makeText(this, "Error al actualizar la contraseña", Toast.LENGTH_SHORT).show()
                            }
                    } else {
                        Toast.makeText(this, "No se encontró el usuario", Toast.LENGTH_SHORT).show()
                    }
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