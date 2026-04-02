package com.example.tfg_kotlin

import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.view.MenuItem
import android.widget.Button
import android.widget.CheckBox
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
import com.example.tfg_kotlin.BBDD_Global.Entities.Empresa
import com.example.tfg_kotlin.BBDD_Global.Entities.FranjaHoraria
import com.example.tfg_kotlin.BBDD_Global.Entities.Piso
import com.example.tfg_kotlin.BBDD_Global.Entities.Sesion
import com.example.tfg_kotlin.BBDD_Global.Entities.Usuario
import com.example.tfg_kotlin.BBDD_Global.Entities.UsuarioSesion
import androidx.core.content.edit

class LoginActivity : AppCompatActivity() {

    // Creación de variables globales
    private lateinit var etCorreo: EditText
    private lateinit var etContrasena: EditText
    private lateinit var btnLogin: Button
    private lateinit var btnRegistro: Button
    private lateinit var tvOlvidarContrasena: TextView

    // Variables para acceso a firebase
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

        // Toolbar con título, acción y logo del botón
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Inicio Sesión"
        supportActionBar?.setHomeAsUpIndicator(R.drawable.ic_adagora)

        // Instanicación para el acceso a firebase
        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        // Variables locales para el uso de los editText
        etCorreo = findViewById(R.id.etCorreo)
        etContrasena = findViewById(R.id.etContrasena)
        btnLogin = findViewById(R.id.btnLogin)
        btnRegistro = findViewById(R.id.btnRegistro)
        tvOlvidarContrasena = findViewById(R.id.tvOlvidarContrasena)

        // Llamamos a la funnción de recuperación de contraseña
        configurarRecuperarContrasena()

        // Cargamos el correo guardado si el checkbox está marcado
        val prefs = getSharedPreferences("loginPrefs", MODE_PRIVATE)
        val recordar = prefs.getBoolean("recordar", false)

        if (recordar) {
            etCorreo.setText(prefs.getString("correo", ""))
            findViewById<CheckBox>(R.id.cbRecordar).isChecked = true

            // Si Firebase mantiene la sesión activa, auto-login sin pedir contraseña
            val usuarioActivo = FirebaseAuth.getInstance().currentUser
            if (usuarioActivo != null && usuarioActivo.email != null) {
                buscarUsuarioEnEmpresas(usuarioActivo.email!!)
            }
        }

        // Acciones de botón de inicio de sesion y redireccionamiento a activity con botón de regegistro
        btnLogin.setOnClickListener { iniciarSesion() }
        btnRegistro.setOnClickListener {
            val registro = Intent(this, RegistroEmpleadoActivity::class.java)
            startActivity(registro)
        }
    }

    // Configura el botón de "¿Has olvidado tu contraseña?" para que abra la pantalla de RecuperarContrasenaActivity
    private fun configurarRecuperarContrasena() {
        tvOlvidarContrasena.setOnClickListener {
            startActivity(Intent(this, RecuperarContrasenaActivity::class.java))
        }
    }

    // función de inicio de sesión
    private fun iniciarSesion() {
        val correo = etCorreo.text.toString().trim()
        val contrasena = etContrasena.text.toString()

        // Comprobación de que los campos no estén vacíos
        if (correo.isEmpty() || contrasena.isEmpty()) {
            Toast.makeText(this, "Completa todos los campos", Toast.LENGTH_SHORT).show()
            return
        }

        // Validación del formato del correo
        if (!correo.contains("@") || !correo.contains(".")) {
            Toast.makeText(this, "Correo inválido", Toast.LENGTH_SHORT).show()
            return
        }

        // Inicia sesión con correo y contraseña
        auth.signInWithEmailAndPassword(correo, contrasena)
            .addOnCompleteListener(this) { task ->
                // En caso de que se iniciab bien la sesión
                if (task.isSuccessful) {
                    // Obtenemos el usuario actual de la BD
                    val user = auth.currentUser
                    // Comprobamos que el usuario exista y que el correo corresponde con el de la BD
                    if (user != null && user.email != null) {

                        // Configuramos el checkbox para recordar solo el correo (sin contraseña)
                        val cbRecordar = findViewById<CheckBox>(R.id.cbRecordar)
                        val prefs = getSharedPreferences("loginPrefs", MODE_PRIVATE)
                        if (cbRecordar.isChecked) {
                            prefs.edit().apply {
                                putString("correo", correo)
                                putBoolean("recordar", true)
                                apply()
                            }
                        } else {
                            prefs.edit() { clear() }
                        }
                        // Usamos la función de buscar usaurio por correo (campo Id de los usuarios)
                        buscarUsuarioEnEmpresas(correo)
                    }
                } else {
                    // En caso de que haya algún error con el login
                    Toast.makeText(this, "Credenciales incorrectas o error de red", Toast.LENGTH_SHORT).show()
                }
            }
    }

    // Función para buscar usuarios por correo usando collectionGroup (búsqueda global O(1))
    // Busca al usuario en la subcolección "usuarios" de todas las empresas en una sola consulta
    private fun buscarUsuarioEnEmpresas(correo: String) {
        db.collectionGroup("usuarios")
            .whereEqualTo("email", correo)
            .get()
            .addOnSuccessListener { querySnapshot ->
                // Si no se encuentra ningún documento del usuario
                if (querySnapshot.isEmpty) {
                    Toast.makeText(this, "No se encontraron datos del usuario", Toast.LENGTH_SHORT).show()
                    return@addOnSuccessListener
                }

                // Obtenemos el primer documento encontrado (el usuario)
                val usuarioDoc = querySnapshot.documents[0]
                val usuario = usuarioDoc.toObject(Usuario::class.java)
                if (usuario == null) {
                    Toast.makeText(this, "Error al obtener datos del usuario", Toast.LENGTH_SHORT).show()
                    return@addOnSuccessListener
                }

                // Obtenemos la referencia de la empresa padre a partir de la ruta del documento
                // Ruta: empresas/{empresaId}/usuarios/{correo}
                val empresaRef = usuarioDoc.reference.parent.parent
                if (empresaRef == null) {
                    Toast.makeText(this, "Error al localizar la empresa del usuario", Toast.LENGTH_SHORT).show()
                    return@addOnSuccessListener
                }

                // Obtenemos los datos de la empresa
                empresaRef.get().addOnSuccessListener { empresaDoc ->
                    val empresa = empresaDoc.toObject(Empresa::class.java)
                    if (empresa == null) {
                        Toast.makeText(this, "Error al obtener empresa", Toast.LENGTH_SHORT).show()
                        return@addOnSuccessListener
                    }

                    // Obtenemos los pisos de la empresa
                    empresaRef.collection("pisos").get().addOnSuccessListener { pisosSnap ->
                        val listaPisos = pisosSnap.mapNotNull { it.toObject(Piso::class.java) }

                        // Obtenemos las franjas horarias
                        empresaRef.collection("franjasHorarias").get()
                            .addOnSuccessListener { franjasSnap ->
                                val listaFranjas =
                                    franjasSnap.mapNotNull { it.toObject(FranjaHoraria::class.java) }

                                // Guardamos la sesión en memoria con el Singleton
                                Sesion.datos = UsuarioSesion(
                                    empresa = empresa,
                                    usuario = usuario,
                                    pisos = listaPisos,
                                    franjasHorarias = listaFranjas
                                )

                                // Valida si un empleado es Jefe o no
                                val intent = if (usuario.esJefe) {
                                    Toast.makeText(this, "Bienvenido ${usuario.nombre}: Iniciada sesión como Jefe", Toast.LENGTH_SHORT).show()
                                    Intent(this, MenuCreadorActivity::class.java)
                                } else {
                                    Toast.makeText(this, "Bienvenido ${usuario.nombre}: Iniciada sesión como Empleado", Toast.LENGTH_SHORT).show()
                                    Intent(this, MenuEmpleadoActivity::class.java)
                                }

                                // Inicia la activity según el tipo de empleado
                                startActivity(intent)
                                finish()

                            }.addOnFailureListener {
                                Toast.makeText(this, "Error al cargar franjas horarias", Toast.LENGTH_SHORT).show()
                            }
                    }.addOnFailureListener {
                        Toast.makeText(this, "Error al cargar pisos", Toast.LENGTH_SHORT).show()
                    }
                }.addOnFailureListener {
                    Toast.makeText(this, "Error al obtener empresa", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener {
                Toast.makeText(this, "Error al buscar usuario", Toast.LENGTH_SHORT).show()
            }
    }

    // Crea funcionalidad de la toolbar
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
