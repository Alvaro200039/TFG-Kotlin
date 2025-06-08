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

    // Creación variables globales
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
        // Toolbar con título, icono y funcionalidad
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Reseteo de Contraseña"
        supportActionBar?.setHomeAsUpIndicator(R.drawable.ic_adagora)

        configurarPaso1()
    }


    private fun configurarPaso1() {
        // Cración variables locales para los editText y botones
        layoutPaso1 = findViewById(R.id.layoutPaso1)
        etCorreo = findViewById(R.id.etCorreoRec)
        btnEnviarCodigo = findViewById(R.id.btnEnviarCodigo)

        btnEnviarCodigo.setOnClickListener {
            correo = etCorreo.text.toString().trim()

            // Validación etCorreo está vacío y formato del correo
            if (correo.isEmpty() || !correo.contains("@") || !correo.contains(".")) {
                etCorreo.error = "Introduce un correo válido"
                return@setOnClickListener
            }

            // Instanciación para el accedo a firabase y coprobación del correo existente en la BD
            FirebaseAuth.getInstance().sendPasswordResetEmail(correo)
                .addOnSuccessListener {
                    // En caso de que exista el correo
                    Toast.makeText(this, "Correo de recuperación enviado a $correo", Toast.LENGTH_LONG).show()
                    finish()
                }
                .addOnFailureListener {
                    // En caso de que el corero no esté registrado
                    Toast.makeText(this, "No se pudo enviar el correo. Verifica que esté registrado.", Toast.LENGTH_SHORT).show()

                }
        }
    }


    // Funcionalidad de la toolbar
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