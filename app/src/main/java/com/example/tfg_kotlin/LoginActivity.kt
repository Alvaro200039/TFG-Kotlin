package com.example.tfg_kotlin


import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import android.widget.*
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.tfg_kotlin.R
import com.example.tfg_kotlin.RecuperarContrasenaActivity
import com.example.tfg_kotlin.utils.Validaciones.validarLogin
import com.example.tfg_kotlin.BBDD_Global.Entities.*
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.DocumentSnapshot
import com.example.tfg_kotlin.Activity_menu_creador
import com.example.tfg_kotlin.activity_menu_empleado

class LoginActivity : AppCompatActivity() {

    private lateinit var tilCorreo: TextInputLayout
    private lateinit var etCorreo: TextInputEditText
    private lateinit var tilContrasena: TextInputLayout
    private lateinit var etContrasena: TextInputEditText
    private lateinit var btnLogin: Button
    private lateinit var btnRegistro: Button
    private lateinit var tvOlvidarContrasena: TextView

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_login)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.loginLayout)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        inicializarVistas()
        configurarEventos()
    }

    private fun inicializarVistas() {
        tilCorreo = findViewById(R.id.tilCorreo)
        etCorreo = findViewById(R.id.etCorreo)
        tilContrasena = findViewById(R.id.tilContrasena)
        etContrasena = findViewById(R.id.etContrasena)
        btnLogin = findViewById(R.id.btnLogin)
        btnRegistro = findViewById(R.id.btnRegistro)
        tvOlvidarContrasena = findViewById(R.id.tvOlvidarContrasena)
    }

    private fun configurarEventos() {
        btnLogin.setOnClickListener {
            if (!validarLogin(tilCorreo, etCorreo, tilContrasena, etContrasena)) return@setOnClickListener

            val correo = etCorreo.text.toString().trim()
            val contrasena = etContrasena.text.toString().trim()

            auth.signInWithEmailAndPassword(correo, contrasena)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        val uid = auth.currentUser?.uid
                        if (uid != null) {
                            buscarUsuarioEnEmpresas(uid)
                        } else {
                            mostrarError("No se pudo obtener el UID del usuario.")
                        }
                    } else {
                        mostrarError("Correo o contraseña incorrectos.")
                    }
                }
        }

        btnRegistro.setOnClickListener {
            startActivity(Intent(this, RegistroEmpleado::class.java))
        }

        tvOlvidarContrasena.setOnClickListener {
            startActivity(Intent(this, RecuperarContrasenaActivity::class.java))
        }
    }

    private fun buscarUsuarioEnEmpresas(uid: String) {
        db.collection("empresas")
            .get()
            .addOnSuccessListener { empresasSnapshot ->
                if (empresasSnapshot.isEmpty) {
                    mostrarError("No hay empresas registradas.")
                    return@addOnSuccessListener
                }
                buscarUsuarioRecursivo(empresasSnapshot.documents, uid, 0)
            }
            .addOnFailureListener {
                mostrarError("Error al obtener empresas.")
            }
    }

    private fun buscarUsuarioRecursivo(empresas: List<DocumentSnapshot>, uid: String, index: Int) {
        if (index >= empresas.size) {
            mostrarError("No se encontraron datos del usuario.")
            FirebaseAuth.getInstance().signOut()
            return
        }

        val empresaDoc = empresas[index]
        val empresaRef = empresaDoc.reference

        empresaRef.collection("Usuarios").document(uid)
            .get()
            .addOnSuccessListener { usuarioDoc ->
                if (usuarioDoc != null && usuarioDoc.exists()) {
                    val usuario = usuarioDoc.toObject(Usuario::class.java) ?: return@addOnSuccessListener
                    val empresa = empresaDoc.toObject(Empresa::class.java)
                    if (empresa == null) {
                        mostrarError("Error al obtener empresa.")
                        return@addOnSuccessListener
                    }

                    empresaRef.collection("pisos").get().addOnSuccessListener { pisosSnap ->
                        val listaPisos = pisosSnap.mapNotNull { it.toObject(Piso::class.java) }

                        empresaRef.collection("franjasHorarias").get().addOnSuccessListener { franjasSnap ->
                            val listaFranjas = franjasSnap.mapNotNull { it.toObject(FranjaHoraria::class.java) }

                            // Guardar la sesión
                            Sesion.datos = UsuarioSesion(
                                empresa = empresa,
                                usuario = usuario,
                                pisos = listaPisos,
                                franjasHorarias = listaFranjas
                            )

                            val destino = if (usuario.esJefe) {
                                Toast.makeText(this, "Bienvenido Jefe", Toast.LENGTH_SHORT).show()
                                Activity_menu_creador::class.java
                            } else {
                                Toast.makeText(this, "Bienvenido Empleado", Toast.LENGTH_SHORT).show()
                                activity_menu_empleado::class.java
                            }

                            startActivity(Intent(this, destino))
                            finish()

                        }.addOnFailureListener {
                            mostrarError("Error al cargar franjas horarias.")
                        }

                    }.addOnFailureListener {
                        mostrarError("Error al cargar pisos.")
                    }

                } else {
                    buscarUsuarioRecursivo(empresas, uid, index + 1)
                }
            }.addOnFailureListener {
                mostrarError("Error al buscar usuario.")
            }
    }

    private fun mostrarError(mensaje: String) {
        Toast.makeText(this, mensaje, Toast.LENGTH_SHORT).show()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                onBackPressedDispatcher.onBackPressed()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
}
