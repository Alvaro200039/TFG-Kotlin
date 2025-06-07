package com.example.tfg_kotlin

import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.DocumentSnapshot
import com.example.tfg_kotlin.BBDD_Global.Entities.Empresa
import com.example.tfg_kotlin.BBDD_Global.Entities.FranjaHoraria
import com.example.tfg_kotlin.BBDD_Global.Entities.Piso
import com.example.tfg_kotlin.BBDD_Global.Entities.Sesion
import com.example.tfg_kotlin.BBDD_Global.Entities.Usuario
import com.example.tfg_kotlin.BBDD_Global.Entities.UsuarioSesion

class LoginActivity : AppCompatActivity() {
    private lateinit var etCorreo: EditText
    private lateinit var etContrasena: EditText
    private lateinit var btnLogin: Button
    private lateinit var btnRegistro: Button
    private lateinit var tvOlvidarContrasena: TextView

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        enableEdgeToEdge()
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.loginLayout)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Toolbar
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        etCorreo = findViewById(R.id.etCorreo)
        etContrasena = findViewById(R.id.etContrasena)
        btnLogin = findViewById(R.id.btnLogin)
        btnRegistro = findViewById(R.id.btnRegistro)
        tvOlvidarContrasena = findViewById(R.id.tvOlvidarContrasena)


        configurarRecuperarContrasena()

        btnLogin.setOnClickListener { iniciarSesion() }
        btnRegistro.setOnClickListener {
            val registro = Intent(this, RegistroEmpleado::class.java)
            startActivity(registro)
        }
    }

    // Configura el botón de "¿Has olvidado tu contraseña?" para que abra la pantalla de RecuperarContrasenaActivity
    private fun configurarRecuperarContrasena() {
        tvOlvidarContrasena.setOnClickListener {
            startActivity(Intent(this, RecuperarContrasenaActivity::class.java))
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
                        buscarUsuarioEnEmpresas(correo)
                    }
                } else {
                    Toast.makeText(
                        this,
                        "Credenciales incorrectas o error de red",
                        Toast.LENGTH_SHORT
                    ).show()
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

    private fun buscarUsuarioRecursivo(
        empresas: List<DocumentSnapshot>,
        correo: String,
        index: Int
    ) {
        if (index >= empresas.size) {
            Toast.makeText(this, "No se encontraron datos del usuario", Toast.LENGTH_SHORT).show()
            return
        }

        val empresaDoc = empresas[index]
        val empresaRef = empresaDoc.reference
        val cif = empresaDoc.id

        empresaRef.collection("usuarios").document(correo)
            .get()
            .addOnSuccessListener { usuarioDoc ->
                if (usuarioDoc != null && usuarioDoc.exists()) {
                    val usuario =
                        usuarioDoc.toObject(Usuario::class.java) ?: return@addOnSuccessListener
                    val empresa = empresaDoc.toObject(Empresa::class.java)
                    if (empresa == null) {
                        Toast.makeText(this, "Error al obtener empresa", Toast.LENGTH_SHORT).show()
                        return@addOnSuccessListener
                    }

                    // Obtener pisos
                    empresaRef.collection("pisos").get().addOnSuccessListener { pisosSnap ->
                        val listaPisos = pisosSnap.mapNotNull { it.toObject(Piso::class.java) }

                        // Obtener franjas horarias
                        empresaRef.collection("franjasHorarias").get()
                            .addOnSuccessListener { franjasSnap ->
                                val listaFranjas =
                                    franjasSnap.mapNotNull { it.toObject(FranjaHoraria::class.java) }

                                // 🔐 GUARDAR LA SESIÓN EN MEMORIA CON SINGLETON
                                Sesion.datos = UsuarioSesion(
                                    empresa = empresa,
                                    usuario = usuario,
                                    pisos = listaPisos,
                                    franjasHorarias = listaFranjas
                                )

                                val intent = if (usuario.esJefe) {
                                    Toast.makeText(this, "Bienvenido Jefe", Toast.LENGTH_SHORT)
                                        .show()
                                    Intent(this, Activity_menu_creador::class.java)
                                } else {
                                    Toast.makeText(this, "Bienvenido Empleado", Toast.LENGTH_SHORT)
                                        .show()
                                    Intent(this, activity_menu_empleado::class.java)
                                }

                                startActivity(intent)
                                finish()

                            }.addOnFailureListener {
                            Toast.makeText(
                                this,
                                "Error al cargar franjas horarias",
                                Toast.LENGTH_SHORT
                            ).show()
                        }

                    }.addOnFailureListener {
                        Toast.makeText(this, "Error al cargar pisos", Toast.LENGTH_SHORT).show()
                    }

                } else {
                    // No encontrado en esta empresa, buscar en la siguiente
                    buscarUsuarioRecursivo(empresas, correo, index + 1)
                }
            }.addOnFailureListener {
                Toast.makeText(this, "Error al buscar usuario", Toast.LENGTH_SHORT).show()
            }
    }

    // Flecha "Atrás"
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
