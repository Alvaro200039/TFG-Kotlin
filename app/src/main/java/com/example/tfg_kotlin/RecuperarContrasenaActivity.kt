package com.example.tfg_kotlin

import android.os.Bundle
import android.view.MenuItem
import android.widget.*
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.firebase.auth.FirebaseAuth

class RecuperarContrasenaActivity : AppCompatActivity() {

    private lateinit var layoutPaso1: LinearLayout
    private lateinit var etCorreo: EditText
    private lateinit var btnEnviarCodigo: Button

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
        supportActionBar?.title = ""
        supportActionBar?.setHomeAsUpIndicator(R.drawable.ic_adagora)

        inicializarVistas()
        configurarPaso1()
    }

    private fun inicializarVistas() {
        layoutPaso1 = findViewById(R.id.layoutPaso1)
        etCorreo = findViewById(R.id.etCorreoRec)
        btnEnviarCodigo = findViewById(R.id.btnEnviarCodigo)

    }


    private fun configurarPaso1() {
        btnEnviarCodigo.setOnClickListener {
            correo = etCorreo.text.toString().trim()

            if (correo.isEmpty() || !correo.contains("@")) {
                etCorreo.error = "Introduce un correo válido"
                return@setOnClickListener
            }

            FirebaseAuth.getInstance().sendPasswordResetEmail(correo)
                .addOnSuccessListener {
                    Toast.makeText(this, "Correo de recuperación enviado a $correo", Toast.LENGTH_LONG).show()
                    finish()
                }
                .addOnFailureListener {
                    Toast.makeText(this, "No se pudo enviar el correo. Verifica que esté registrado.", Toast.LENGTH_SHORT).show()
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