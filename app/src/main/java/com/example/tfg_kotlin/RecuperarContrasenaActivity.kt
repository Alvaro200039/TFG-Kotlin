package com.example.tfg_kotlin

import android.os.Bundle
import android.view.MenuItem
import android.widget.*
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.room.Room
import com.example.tfg_kotlin.BBDD.BBDD
import com.example.tfg_kotlin.BBDD.BBDDInstance
import com.example.tfg_kotlin.Validaciones.construirNombreBD

class RecuperarContrasenaActivity : AppCompatActivity() {

    private lateinit var layoutPaso1: LinearLayout
    private lateinit var layoutPaso2: LinearLayout

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

        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        layoutPaso1 = findViewById(R.id.layoutPaso1)
        layoutPaso2 = findViewById(R.id.layoutPaso2)

        val etCorreo = findViewById<EditText>(R.id.etCorreoRec)
        val btnEnviarCodigo = findViewById<Button>(R.id.btnEnviarCodigo)

        val etCodigo = findViewById<EditText>(R.id.etCodigo)
        val etNuevaPass = findViewById<EditText>(R.id.etNuevaPass)
        val etRepetirPass = findViewById<EditText>(R.id.etRepetirPass)
        val btnCambiar = findViewById<Button>(R.id.btnCambiarPass)

        btnEnviarCodigo.setOnClickListener {
            correo = etCorreo.text.toString().trim()
            if (correo.isEmpty() || !correo.contains("@")) {
                etCorreo.error = "Introduce un correo válido"
                return@setOnClickListener
            }

            val dominio = correo.substringAfter("@")
            val nombreBD = construirNombreBD(dominio)

            db = BBDDInstance.getDatabase(applicationContext, nombreBD)

            val usuario = db.appDao().buscarEmpleadoPorCorreo(correo)

            if (usuario == null) {
                etCorreo.error = "No hay ninguna cuenta con este correo"
                return@setOnClickListener
            }

            // Simular envío del código
            codigoGenerado = (100000..999999).random().toString()
            Toast.makeText(this, "Tu código es: $codigoGenerado", Toast.LENGTH_LONG).show()

            layoutPaso1.visibility = LinearLayout.GONE
            layoutPaso2.visibility = LinearLayout.VISIBLE
        }

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

            db.appDao().actualizarContrasena(correo, nuevaPass)
            Toast.makeText(this, "Contraseña actualizada correctamente", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

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