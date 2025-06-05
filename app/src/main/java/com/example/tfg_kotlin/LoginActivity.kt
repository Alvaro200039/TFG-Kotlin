package com.example.tfg_kotlin

import android.content.Intent
import android.os.Bundle
import android.provider.Settings.Global.putString
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import androidx.core.content.edit

class LoginActivity : AppCompatActivity() {
    private lateinit var etCorreo: EditText
    private lateinit var etContrasena: EditText
    private lateinit var btnLogin: Button
    private lateinit var btnRegistro: Button

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)
        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        etCorreo = findViewById(R.id.etCorreo)
        etContrasena = findViewById(R.id.etContrasena)
        btnLogin = findViewById(R.id.btnLogin)
        btnRegistro = findViewById(R.id.btnRegistro)

        btnLogin.setOnClickListener {
            iniciarSesion()
        }
        btnRegistro.setOnClickListener {
            val registro = Intent(this, RegistroEmpleado::class.java)
            startActivity(registro)
        }
    }

    private fun iniciarSesion() {
        val correo = etCorreo.text.toString().trim()
        val contrasena = etContrasena.text.toString()

        if (correo.isEmpty() || contrasena.isEmpty()) {
            Toast.makeText(this, "Completa todos los campos", Toast.LENGTH_SHORT).show()
            return
        }

        if (!correo.contains("@")) {
            Toast.makeText(this, "Correo inválido", Toast.LENGTH_SHORT).show()
            return
        }

        auth.signInWithEmailAndPassword(correo, contrasena)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    val user = auth.currentUser
                    if (user != null && user.email != null) {
                        // Buscar usuario dentro de las empresas
                        buscarUsuarioEnEmpresas(correo)
                    }
                } else {
                    Toast.makeText(this, "Credenciales incorrectas o error de red", Toast.LENGTH_SHORT).show()
                }
            }
    }

    private fun buscarUsuarioEnEmpresas(correo: String) {
        db.collection("empresas")
            .get()
            .addOnSuccessListener { empresasSnapshot ->
                if (empresasSnapshot.isEmpty) {
                    Toast.makeText(this, "No hay empresas registradas", Toast.LENGTH_SHORT).show()
                    return@addOnSuccessListener
                }

                buscarUsuarioRecursivo(empresasSnapshot.documents, correo, 0)
            }
            .addOnFailureListener {
                Toast.makeText(this, "Error al obtener empresas", Toast.LENGTH_SHORT).show()
            }
    }

    private fun buscarUsuarioRecursivo(empresas: List<com.google.firebase.firestore.DocumentSnapshot>, correo: String, index: Int) {
        if (index >= empresas.size) {
            Toast.makeText(this, "No se encontraron datos del usuario", Toast.LENGTH_SHORT).show()
            return
        }

        val empresaDoc = empresas[index]
        empresaDoc.reference.collection("usuarios").document(correo)
            .get()
            .addOnSuccessListener { usuarioDoc ->
                if (usuarioDoc != null && usuarioDoc.exists()) {
                    val esJefe = usuarioDoc.getBoolean("esJefe") ?: false
                    val nombre = usuarioDoc.getString("nombre") ?: ""
                    val apellidos = usuarioDoc.getString("apellidos") ?: ""
                    val cif = usuarioDoc.getString("cif") ?: empresaDoc.getString("cif") ?: ""
                    val nombreEmpresa = empresaDoc.id

                    val intent = if (esJefe) {
                        Toast.makeText(this, "Bienvenido Jefe", Toast.LENGTH_SHORT).show()
                        Intent(this, Activity_menu_creador::class.java)
                    } else {
                        Toast.makeText(this, "Bienvenido Empleado", Toast.LENGTH_SHORT).show()
                        Intent(this, activity_menu_empleado::class.java)
                    }

                    val prefs = getSharedPreferences("MiAppPrefs", MODE_PRIVATE)
                    prefs.edit { putString("correo", correo) }
                    prefs.edit{ putString("nombreUsuario", "$nombre $apellidos") }
                    prefs.edit { putString("cifUsuario", cif) }
                    prefs.edit { putString("nombreEmpresa", nombreEmpresa) }

                    startActivity(intent)
                    finish()

                } else {
                    // No encontrado en esta empresa, buscar en la siguiente
                    buscarUsuarioRecursivo(empresas, correo, index + 1)
                }
            }
            .addOnFailureListener {
                Toast.makeText(this, "Error al buscar usuario", Toast.LENGTH_SHORT).show()
            }
    }
}