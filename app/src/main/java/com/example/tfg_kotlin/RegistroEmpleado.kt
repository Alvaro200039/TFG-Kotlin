package com.example.tfg_kotlin

import android.os.Bundle
import android.view.MenuItem
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.tfg_kotlin.utils.Validaciones
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.google.android.material.button.MaterialButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

class RegistroEmpleado : AppCompatActivity() {
    private lateinit var tilCorreo: TextInputLayout
    private lateinit var etCorreo: TextInputEditText
    private lateinit var tilNombre: TextInputLayout
    private lateinit var etNombre: TextInputEditText
    private lateinit var tilApellidos: TextInputLayout
    private lateinit var etApellidos: TextInputEditText
    private lateinit var tilCif: TextInputLayout
    private lateinit var etCif: TextInputEditText
    private lateinit var tilContrasena: TextInputLayout
    private lateinit var etContrasena: TextInputEditText
    private lateinit var tilRepetirContrasena: TextInputLayout
    private lateinit var etRepetirContrasena: TextInputEditText
    private lateinit var btnRegistrar: MaterialButton

    private lateinit var auth: FirebaseAuth
    private val firestore = Firebase.firestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_registro)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.registroempleadoLayout)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        // Toolbar
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)


        inicializarVistas()
        auth = FirebaseAuth.getInstance()
        configurarBotonRegistrar()

    }

    private fun inicializarVistas() {
        tilNombre = findViewById(R.id.tilNombre)
        etNombre = findViewById(R.id.etNombre)

        tilApellidos = findViewById(R.id.tilApellidos)
        etApellidos = findViewById(R.id.etApellidos)

        tilCorreo = findViewById(R.id.tilCorreo)
        etCorreo = findViewById(R.id.etCorreo)

        tilContrasena = findViewById(R.id.tilContrasena)
        etContrasena = findViewById(R.id.etContrasena)

        tilRepetirContrasena = findViewById(R.id.tilRepetirContrasena)
        etRepetirContrasena = findViewById(R.id.etRepetirContrasena)

        tilCif = findViewById(R.id.tilCif)
        etCif = findViewById(R.id.editCif)

        btnRegistrar = findViewById(R.id.btnRegistrar)
    }

    private fun configurarBotonRegistrar() {
        btnRegistrar.setOnClickListener {
            if (!validarFormulario()) return@setOnClickListener
            registrarEmpleado()
        }
    }

    private fun validarFormulario(): Boolean {
        val esValido = Validaciones.validarRegistroPersona(
            tilNombre, etNombre,
            tilApellidos, etApellidos,
            tilCorreo, etCorreo,
            tilContrasena, etContrasena,
            tilRepetirContrasena, etRepetirContrasena,
            tilCif, etCif
        )

        if (!esValido) return false

        val contrasena = etContrasena.text.toString().trim()
        if (contrasena.length < 6) {
            tilContrasena.error = "Debe tener al menos 6 caracteres"
            etContrasena.requestFocus()
            return false
        }

        return true
    }



    private fun registrarEmpleado() {

        val correo = etCorreo.text.toString().trim()
        val cif = etCif.text.toString().trim()
        val contrasena = etContrasena.text.toString().trim()
        val nombre = etNombre.text.toString().trim()
        val apellidos = etApellidos.text.toString().trim()

        val dominio = '@' + correo.substringAfterLast("@")
        val dominioConArroba = "@$dominio"

        firestore.collection("empresas").whereEqualTo("dominio", dominioConArroba).get()
            .addOnSuccessListener { empresasSnapshot ->
                if (empresasSnapshot.isEmpty) {
                    tilCorreo.error = "El dominio introducido no existe"
                    return@addOnSuccessListener
                }

                val empresaDoc = empresasSnapshot.documents.first()
                val empresaCif = empresaDoc.getString("cif") ?: ""
                val esJefe = cif.equals(empresaCif, ignoreCase = true)

                auth.createUserWithEmailAndPassword(correo, contrasena)
                    .addOnSuccessListener {
                        val uid = it.user?.uid ?: return@addOnSuccessListener

                        val usuarioMap = hashMapOf(
                            "id" to uid,
                            "email" to correo,
                            "nombre" to nombre,
                            "apellidos" to apellidos,
                            "contrasena" to contrasena,
                            "cif" to if (esJefe) cif else "",
                            "esJefe" to esJefe
                        )

                        empresaDoc.reference.collection("usuarios")
                            .document(uid)
                            .set(usuarioMap)
                            .addOnSuccessListener {
                                mostrarToast("Empleado registrado${if (esJefe) " como Jefe" else ""}")
                                limpiarCampos()
                            }
                            .addOnFailureListener {
                                it.printStackTrace()
                                mostrarToast("Error guardando usuario en Firestore")
                            }
                    }
                    .addOnFailureListener {
                        it.printStackTrace()
                        tilCorreo.error = "Error registrando usuario: ${it.message}"
                    }
            }
            .addOnFailureListener {
                it.printStackTrace()
                tilCorreo.error = "Error buscando empresa: ${it.message}"
            }
    }

    private fun mostrarToast(mensaje: String) {
        Toast.makeText(this@RegistroEmpleado, mensaje, Toast.LENGTH_SHORT).show()
    }

    private fun limpiarCampos() {
        etCorreo.text?.clear()
        etContrasena.text?.clear()
        etRepetirContrasena.text?.clear()
        etNombre.text?.clear()
        etApellidos.text?.clear()
        etCif.text?.clear()
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

