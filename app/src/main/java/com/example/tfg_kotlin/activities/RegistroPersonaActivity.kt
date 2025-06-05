package com.example.tfg_kotlin.activities

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import android.widget.Button
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.tfg_kotlin.R
import com.example.tfg_kotlin.utils.Validaciones
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.auth.FirebaseAuth


class RegistroPersonaActivity : AppCompatActivity() {

    // Vistas
    private lateinit var tilNombre: TextInputLayout
    private lateinit var etNombre: TextInputEditText
    private lateinit var tilApellidos: TextInputLayout
    private lateinit var etApellidos: TextInputEditText
    private lateinit var tilCorreo: TextInputLayout
    private lateinit var etCorreo: TextInputEditText
    private lateinit var tilContrasena: TextInputLayout
    private lateinit var etContrasena: TextInputEditText
    private lateinit var tilRepContrasena: TextInputLayout
    private lateinit var etRepContrasena: TextInputEditText
    private lateinit var tilCif: TextInputLayout
    private lateinit var etCif: TextInputEditText
    private lateinit var btnFinalizarRegistro: Button

    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()


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

        inicializarVistas()
        configurarBotonFinalizar()
    }

    private fun inicializarVistas() {
        btnFinalizarRegistro = findViewById(R.id.btnFinalizarRegistro)

        tilNombre = findViewById(R.id.tilNombre)
        etNombre = findViewById(R.id.etNombre)

        tilApellidos = findViewById(R.id.tilApellidos)
        etApellidos = findViewById(R.id.etApellidos)

        tilCorreo = findViewById(R.id.tilCorreo)
        etCorreo = findViewById(R.id.etCorreo)

        tilContrasena = findViewById(R.id.tilContrasena)
        etContrasena = findViewById(R.id.etContrasena)

        tilRepContrasena = findViewById(R.id.tilRepContrasena)
        etRepContrasena = findViewById(R.id.etRepContrasena)

        tilCif = findViewById(R.id.tilNumEmpresa)
        etCif = findViewById(R.id.etNumEmpresa)
    }

    private fun configurarBotonFinalizar() {
        btnFinalizarRegistro.setOnClickListener {
            if (!validarFormulario()) return@setOnClickListener
            registrarPersona()
        }
    }

    private fun validarFormulario(): Boolean {
        val esValido = Validaciones.validarRegistroPersona(
            tilNombre, etNombre,
            tilApellidos, etApellidos,
            tilCorreo, etCorreo,
            tilContrasena, etContrasena,
            tilRepContrasena, etRepContrasena,
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

    private fun registrarPersona() {
        val nombre = etNombre.text.toString().trim()
        val apellidos = etApellidos.text.toString().trim()
        val correo = etCorreo.text.toString().trim().lowercase()
        val contrasena = etContrasena.text.toString().trim()
        val cif = etCif.text.toString().trim().uppercase()
        val dominio = "@" + correo.substringAfter("@")

        // Paso 1: Comprobar si el dominio existe en la colección "empresas"
        db.collection("empresas")
            .whereEqualTo("dominio", dominio)
            .get()
            .addOnSuccessListener { documentos ->
                if (documentos.isEmpty) {
                    tilCorreo.error = "No existe ninguna empresa con ese dominio"
                    return@addOnSuccessListener
                }

                val empresaDoc = documentos.documents.first()
                val cifEmpresa = empresaDoc.getString("cif") ?: ""
                val esJefe = cif == cifEmpresa

                // Paso 2: Registrar el usuario en FirebaseAuth
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

                        // Paso 3: Guardar usuario dentro de la colección empresas
                        empresaDoc.reference.collection("Usuarios")
                            .document(uid)
                            .set(usuarioMap)
                            .addOnSuccessListener {
                                val intent = if (esJefe)
                                    Intent(this, JefeActivity::class.java)
                                else
                                    Intent(this, EmpleadoActivity::class.java)
                                startActivity(intent)
                                finish()
                            }
                            .addOnFailureListener {
                                Log.e("REGISTRO", "Error al guardar usuario", it)
                            }
                    }
                    .addOnFailureListener {
                        Log.e("REGISTRO", "Error al registrar auth", it)
                        tilCorreo.error = "No se pudo registrar el usuario"
                    }
            }
            .addOnFailureListener {
                Log.e("REGISTRO", "Error al buscar empresa", it)
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
