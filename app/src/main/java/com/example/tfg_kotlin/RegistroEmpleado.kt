package com.example.tfg_kotlin

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

class RegistroEmpleado : AppCompatActivity() {
    private lateinit var etCorreo: EditText
    private lateinit var etContrasena: EditText
    private lateinit var etRepetirContrasena: EditText
    private lateinit var etNombre: EditText
    private lateinit var etApellidos: EditText
    private lateinit var etCif: EditText
    private lateinit var btnRegistrar: Button

    private lateinit var auth: FirebaseAuth
    private val firestore = Firebase.firestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_registro)

        etCorreo = findViewById(R.id.etCorreo)
        etCif = findViewById(R.id.etCif)
        etNombre = findViewById(R.id.etNombre)
        etApellidos = findViewById(R.id.etApellidos)
        etContrasena = findViewById(R.id.etContrasena)
        etRepetirContrasena = findViewById(R.id.etRepetirContrasena)
        btnRegistrar = findViewById(R.id.btnRegistrar)

        auth = FirebaseAuth.getInstance()

        btnRegistrar.setOnClickListener {
            registrarEmpleado()
        }
    }

    private fun registrarEmpleado() {
        val correo = etCorreo.text.toString().trim()
        val cif = etCif.text.toString().trim()
        val contrasena = etContrasena.text.toString()
        val repetirContrasena = etRepetirContrasena.text.toString()
        val nombre = etNombre.text.toString().trim()
        val apellidos = etApellidos.text.toString().trim()

        if (correo.isEmpty() || contrasena.isEmpty() || repetirContrasena.isEmpty() || nombre.isEmpty() || apellidos.isEmpty()) {
            mostrarToast("Completa todos los campos")
            return
        }

        if (!correo.contains("@")) {
            mostrarToast("Correo inválido")
            return
        }

        if (contrasena != repetirContrasena) {
            mostrarToast("Las contraseñas no coinciden")
            return
        }

        val dominio = correo.substringAfterLast("@")
        val dominioConArroba = "@$dominio"

        val empresasRef = firestore.collection("empresas")

        // Buscamos la empresa por dominio
        empresasRef.whereEqualTo("dominio", dominioConArroba).get()
            .addOnSuccessListener { empresasSnapshot ->
                if (empresasSnapshot.isEmpty) {
                    mostrarToast("El dominio introducido no existe o es incorrecto")
                } else {
                    val empresaDoc = empresasSnapshot.documents[0]
                    val empresaCif = empresaDoc.getString("cif") ?: ""
                    val esJefe = cif.equals(empresaCif, ignoreCase = true)
                    val cifFinal = empresaCif // se use o no, se guarda el cif correcto siempre
                    if (esJefe && cif != empresaCif) {
                        mostrarToast("El CIF no coincide con el dominio. Corrígelo para continuar.")
                        etCif.requestFocus()
                        return@addOnSuccessListener
                    }

                    // Creamos el usuario en Firebase Authentication
                    auth.createUserWithEmailAndPassword(correo, contrasena)
                        .addOnCompleteListener { task ->
                            if (task.isSuccessful) {
                                //val esJefe = empresaCif.equals(cif, ignoreCase = true)
                                val uid = auth.currentUser?.uid ?: ""

                                val nuevoUsuario = hashMapOf(
                                    "email" to correo,
                                    "contrasena" to contrasena,
                                    "cif" to cifFinal,
                                    "esJefe" to esJefe,
                                    "nombre" to nombre,
                                    "apellidos" to apellidos,
                                    "uid" to uid,
                                )

                                // Guardamos datos extra en Firestore, colección "usuarios"
                                empresaDoc.reference.collection("usuarios").document(uid)
                                    .set(nuevoUsuario)
                                    .addOnSuccessListener {
                                        if (esJefe) {
                                            mostrarToast("Empleado registrado como Jefe")
                                        } else {
                                            mostrarToast("Empleado registrado")
                                        }
                                        limpiarCampos()
                                    }
                                    .addOnFailureListener { e ->
                                        mostrarToast("Error guardando datos de usuario")
                                        e.printStackTrace()
                                    }
                            } else {
                                mostrarToast("Error registrando usuario: ${task.exception?.message}")
                            }
                        }
                }
            }
            .addOnFailureListener { e ->
                mostrarToast("Error buscando empresa")
                e.printStackTrace()
            }
    }

    private fun mostrarToast(mensaje: String) {
        Toast.makeText(this@RegistroEmpleado, mensaje, Toast.LENGTH_SHORT).show()
    }

    private fun limpiarCampos() {
        etCorreo.text.clear()
        etContrasena.text.clear()
        etRepetirContrasena.text.clear()
        etNombre.text.clear()
        etApellidos.text.clear()
        etCif.text.clear()
    }
}
