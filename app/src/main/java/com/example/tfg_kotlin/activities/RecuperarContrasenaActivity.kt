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
import com.example.tfg_kotlin.database.BBDDInstance
import com.example.tfg_kotlin.utils.Validaciones.construirNombreBD
import com.example.tfg_kotlin.database.BBDD

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
    private lateinit var db: BBDD

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
            val nombreBD = construirNombreBD(dominio)

            db = BBDDInstance.getDatabase(applicationContext, nombreBD)
            val usuario = db.recuperarContrasenaDao().buscarEmpleadoPorCorreo(correo)

            if (usuario == null) {
                etCorreo.error = "No hay ninguna cuenta con este correo"
                return@setOnClickListener
            }

            codigoGenerado = (100000..999999).random().toString()
            Toast.makeText(this, "Tu código es: $codigoGenerado", Toast.LENGTH_LONG).show()

            layoutPaso1.visibility = LinearLayout.GONE
            layoutPaso2.visibility = LinearLayout.VISIBLE
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

            if (nuevaPass.length < 4) {
                etNuevaPass.error = "La contraseña debe tener al menos 4 caracteres"
                return@setOnClickListener
            }

            if (nuevaPass != repetirPass) {
                etRepetirPass.error = "Las contraseñas no coinciden"
                return@setOnClickListener
            }

            db.recuperarContrasenaDao().actualizarContrasena(correo, nuevaPass)
            Toast.makeText(this, "Contraseña actualizada correctamente", Toast.LENGTH_SHORT).show()
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