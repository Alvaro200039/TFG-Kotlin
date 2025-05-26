package com.example.tfg_kotlin

import android.os.Bundle
import android.view.MenuItem
import android.widget.Button
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.room.Room
import com.example.tfg_kotlin.BBDD.BBDD
import com.example.tfg_kotlin.BBDD.Empleados
import com.example.tfg_kotlin.BBDD.MIGRATION_1_2
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout

class RegistroPersonaActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_registro_persona)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.registropersona)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Toolbar
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        // Referencias a vistas
        val btnFinalizarRegistro = findViewById<Button>(R.id.btnFinalizarRegistro)

        val tilNombre = findViewById<TextInputLayout>(R.id.tilNombre)
        val etNombre = findViewById<TextInputEditText>(R.id.etNombre)

        val tilApellidos = findViewById<TextInputLayout>(R.id.tilApellidos)
        val etApellidos = findViewById<TextInputEditText>(R.id.etApellidos)

        val tilCorreo = findViewById<TextInputLayout>(R.id.tilCorreo)
        val etCorreo = findViewById<TextInputEditText>(R.id.etCorreo)

        val tilContrasena = findViewById<TextInputLayout>(R.id.tilContrasena)
        val etContrasena = findViewById<TextInputEditText>(R.id.etContrasena)

        val tilRepContrasena = findViewById<TextInputLayout>(R.id.tilRepContrasena)
        val etRepContrasena = findViewById<TextInputEditText>(R.id.etRepContrasena)

        btnFinalizarRegistro.setOnClickListener {
            val esValido = Validaciones.validarRegistroPersona(
                tilNombre, etNombre,
                tilApellidos, etApellidos,
                tilCorreo, etCorreo,
                tilContrasena, etContrasena,
                tilRepContrasena, etRepContrasena
            )

            if (esValido) {
                val db = Room.databaseBuilder(
                    applicationContext,
                    BBDD::class.java,
                    "reservs_db"
                ).addMigrations(MIGRATION_1_2).allowMainThreadQueries().build()

                val correo = etCorreo.text.toString()
                val usuarioExistente = db.appDao().buscarEmpleadoPorCorreo(correo)

                if (usuarioExistente != null) {
                    tilCorreo.error = "Este correo ya está registrado"
                    etCorreo.requestFocus()
                    return@setOnClickListener
                }

                val empleado = Empleados(
                    nombre = etNombre.text.toString(),
                    apellidos = etApellidos.text.toString(),
                    correo = etCorreo.text.toString(),
                    contrasena = etContrasena.text.toString(),
                    esJefe = false
                )

                db.appDao().insertarEmpleado(empleado)
                Toast.makeText(this, "Registro completado", Toast.LENGTH_SHORT).show()
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
