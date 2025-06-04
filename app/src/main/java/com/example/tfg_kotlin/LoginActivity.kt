package com.example.tfg_kotlin

import android.content.Intent
import android.os.Bundle
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

        etCorreo = findViewById(R.id.etCorreo)
        etContrasena = findViewById(R.id.etContrasena)
        btnLogin = findViewById(R.id.btnLogin)
        btnRegistro = findViewById(R.id.btnRegistro)


        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

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
                    if (user != null) {
                        // Obtener datos adicionales del usuario (ejemplo: rol) de Firestore
                        db.collection("usuarios").document(user.uid).get()
                            .addOnSuccessListener { document ->
                                if (document != null && document.exists()) {
                                    val esJefe = document.getBoolean("esJefe") ?: false
                                    val nombre = document.getString("nombre") ?: ""
                                    val apellidos = document.getString("apellidos") ?: ""
                                    val cif = document.getString("cif") ?: ""

                                    val intent = if (esJefe) {
                                        Toast.makeText(this, "Bienvenido Jefe", Toast.LENGTH_SHORT).show()
                                        Intent(this, Activity_menu_creador::class.java)
                                    } else {
                                        Toast.makeText(this, "Bienvenido Empleado", Toast.LENGTH_SHORT).show()
                                        Intent(this, activity_menu_empleado::class.java)
                                    }

                                    intent.putExtra("idUsuario", user.uid)
                                    intent.putExtra("nombreUsuario", "$nombre $apellidos")
                                    intent.putExtra("cifUsuario", cif)
                                    startActivity(intent)
                                    finish()
                                }
                                else {
                                    Toast.makeText(this, "No se encontraron datos del usuario", Toast.LENGTH_SHORT).show()
                                }
                            }
                            .addOnFailureListener {
                                Toast.makeText(this, "Error al obtener datos del usuario", Toast.LENGTH_SHORT).show()
                            }
                    }
                } else {
                    Toast.makeText(this, "Credenciales incorrectas o error de red", Toast.LENGTH_SHORT).show()
                }
            }
    }
}