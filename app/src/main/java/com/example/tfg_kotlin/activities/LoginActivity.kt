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
import com.example.tfg_kotlin.R
import com.example.tfg_kotlin.utils.Validaciones.validarLogin
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

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

            if (!validarLogin(
                    tilCorreo, etCorreo,
                    tilContrasena, etContrasena))
                return@setOnClickListener


            FirebaseAuth.getInstance().signInWithEmailAndPassword(correo, contrasena)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        val uid = FirebaseAuth.getInstance().currentUser?.uid
                        if (uid != null) {
                            FirebaseFirestore.getInstance()
                                .collection("usuarios")
                                .document(uid)
                                .get()
                                .addOnSuccessListener { document ->
                                    if (document.exists()) {
                                        val esJefe = document.getBoolean("esJefe") ?: false
                                        val destino = if (esJefe) JefeActivity::class.java else EmpleadoActivity::class.java
                                        Toast.makeText(this, "Bienvenido", Toast.LENGTH_SHORT).show()
                                        startActivity(Intent(this, destino))
                                        finish()
                                    } else {
                                        mostrarError("No se encontraron datos del usuario.")
                                    }
                                }
                                .addOnFailureListener {
                                    mostrarError("Error al obtener datos del usuario.")
                                }
                        } else {
                            mostrarError("No se pudo obtener el UID del usuario.")
                        }
                    } else {
                        mostrarError("Correo o contraseña incorrectos.")
                    }
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