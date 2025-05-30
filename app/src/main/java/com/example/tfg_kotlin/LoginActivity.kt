package com.example.tfg_kotlin

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
import androidx.room.Room
import com.example.tfg_kotlin.BBDD.BBDD
import com.example.tfg_kotlin.Validaciones.construirNombreBD
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout

class LoginActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_login)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.loginpersona)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Asocia Toolbar como ActionBar
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        // Referencias a vistas
        val btnFinalizarLogin = findViewById<Button>(R.id.btnFinalizarLogin)
        val tilCorreo = findViewById<TextInputLayout>(R.id.tilCorreo)
        val tilContrasena = findViewById<TextInputLayout>(R.id.tilContrasena)


        val etCorreo = findViewById<TextInputEditText>(R.id.etCorreo)
        val etContrasena = findViewById<TextInputEditText>(R.id.etContrasena)

        //Conexion para activity recuperar la contraseña
        findViewById<TextView>(R.id.tvOlvidarContrasena).setOnClickListener {
            startActivity(Intent(this, RecuperarContrasenaActivity::class.java))
        }


        btnFinalizarLogin.setOnClickListener {
            val correo = etCorreo.text.toString().trim()
            val contrasena = etContrasena.text.toString().trim()
            val dominioCorreo = correo.substringAfter("@")

            if (correo.isEmpty() || contrasena.isEmpty()) {
                Toast.makeText(this, "Rellena todos los campos", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Validar que el dominio existe en la base de datos maestra
            val dbMaestra = Room.databaseBuilder(
                applicationContext,
                BBDD::class.java,
                "maestra_db"
            ).allowMainThreadQueries().build()

            val empresaExiste = dbMaestra.appDao().getEmpresaPorDominioEnEmpresa(dominioCorreo)
            if (empresaExiste == null) {
                mostrarError("No existe ninguna empresa registrada con el dominio @$dominioCorreo")
                return@setOnClickListener
            }

            // Construimos nombre de la BD según el dominio
            val nombreBD = construirNombreBD(dominioCorreo)

            val db = Room.databaseBuilder(
                applicationContext,
                BBDD::class.java,
                nombreBD
            ).allowMainThreadQueries().build()

            val usuario = db.appDao().loginUsuario(correo, contrasena)

            if (usuario != null) {
                Toast.makeText(
                    this,
                    if (usuario.esJefe) "Bienvenido jefe" else "Bienvenido empleado",
                    Toast.LENGTH_SHORT
                ).show()

                // Aquí rediriges según tipo
                val destino =
                    if (usuario.esJefe) JefeActivity::class.java else EmpleadoActivity::class.java
                startActivity(Intent(this, destino))
                finish()
            } else {
                mostrarError("Correo o contraseña incorrectos o empresa no registrada")
            }
        }
    }
    private fun mostrarError(mensaje: String) {
        Toast.makeText(this, mensaje, Toast.LENGTH_SHORT).show()
    }


    // Flecha TOOLBAR "ATRÁS"
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
