package com.example.tfg_kotlin

import android.content.Intent
import android.os.Bundle
import android.util.Log
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
import com.example.tfg_kotlin.BBDD.BBDDInstance
import com.example.tfg_kotlin.BBDD.Empleados
import com.example.tfg_kotlin.Validaciones.construirNombreBD
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

        val tilCif = findViewById<TextInputLayout>(R.id.tilNumEmpresa)
        val etCif = findViewById<TextInputEditText>(R.id.etNumEmpresa)


        btnFinalizarRegistro.setOnClickListener {
            val esValido = Validaciones.validarRegistroPersona(
                tilNombre, etNombre,
                tilApellidos, etApellidos,
                tilCorreo, etCorreo,
                tilContrasena, etContrasena,
                tilRepContrasena, etRepContrasena,
                tilCif, etCif
            )


            if (esValido) {
                val correo = etCorreo.text.toString().trim()
                val dominio = correo.substringAfter("@").lowercase()
                val cifInput = etCif.text.toString().trim().uppercase()

                Log.d("REGISTRO_PERSONA", "Dominio buscado: $dominio")

                val dbMaestra = BBDDInstance.getDatabase(this, "maestra_db")

                val todas = dbMaestra.appDao().getTodasLasEmpresas()
                for (e in todas) {
                    Log.d("EMPRESA_CHECK", "Dominio en BBDD: '${e.dominio}'")
                }
                Log.d("EMPRESA_CHECK", "Dominio buscado: '$dominio'")

                // Validar si existe el jefe (empresa)
                val empresa = dbMaestra .appDao().getEmpresaPorDominioEnEmpresa(dominio)

                if (empresa == null) {
                    tilCorreo.error = "No existe ninguna empresa con el dominio @$dominio"
                    etCorreo.requestFocus()
                    return@setOnClickListener
                }
                // Verificar si el correo ya está registrado en la bd
                val nombreBD = construirNombreBD(dominio)

                val dbEmpresa = BBDDInstance.getDatabase(this, nombreBD)


                // Verificar si el correo ya está registrado
                val usuarioExistente = dbEmpresa.appDao().buscarEmpleadoPorCorreo(correo)

                if (usuarioExistente != null) {
                    tilCorreo.error = "Este correo ya está registrado"
                    etCorreo.requestFocus()
                    return@setOnClickListener
                }

                val esJefe = cifInput.isNotEmpty() && cifInput == empresa.cif

                val empleado = Empleados(
                    nombre = etNombre.text.toString(),
                    apellidos = etApellidos.text.toString(),
                    correo = correo,
                    dominio = dominio,
                    contrasena = etContrasena.text.toString(),
                    cif = if (esJefe) cifInput else "",
                    esJefe = esJefe
                )

                dbEmpresa .appDao().insertarEmpleado(empleado)
                Toast.makeText(this, "Registro completado", Toast.LENGTH_SHORT).show()

                val intent = if (esJefe) {
                    Intent(this, JefeActivity::class.java)
                } else {
                    Intent(this, EmpleadoActivity::class.java)
                }
                startActivity(intent)
                finish()

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
