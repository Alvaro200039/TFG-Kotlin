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

        // Acciones de botón de inicio de sesion y redireccionamiento a activity con botón de regegistro
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
                        // Usamos la función de buscar usaurio por correo (campo Id de los usuarios)
                        buscarUsuarioEnEmpresas(correo)
                    }
                } else {
                    // En caso de que haya algún error con el login
                    Toast.makeText(this, "Credenciales incorrectas o error de red", Toast.LENGTH_SHORT).show()
                }
            }
    }

    // Función para buscar usuarios por correo
    private fun buscarUsuarioEnEmpresas(correo: String) {
        // en firebase, busca en la coleción (rama) empresas
        db.collection("empresas")
            .get()
            .addOnSuccessListener { empresasSnapshot ->
                // en caso de que no se encuentren empresas registradas con ese correo (dominio)
                if (empresasSnapshot.isEmpty) {
                    Toast.makeText(this, "No hay empresas registradas", Toast.LENGTH_SHORT).show()
                    return@addOnSuccessListener
                }
                // En caso de que sí existan empresas con el correo, accedemos a la siguiete colleción, "empleados"
                buscarUsuarioRecursivo(empresasSnapshot.documents, correo, 0)
            }
            // Añadimos una excepción en caso de que haya algún error con la obtención de empresas
            .addOnFailureListener {
                Toast.makeText(this, "Error al obtener empresas", Toast.LENGTH_SHORT).show()
            }
    }

    // Función para buscar de forma recursiva los usuarios de las empresas registradas
    private fun buscarUsuarioRecursivo(
        // Tiene en cuenta el correo, la lista de empresas y el númerop de empleados registrados en una empresa (index)
        empresas: List<DocumentSnapshot>,
        correo: String,
        index: Int
    ) {
        // En caso de que no se encuentren usuarios registrados en una emppresa
        if (index >= empresas.size) {
            Toast.makeText(this, "No se encontraron datos del usuario", Toast.LENGTH_SHORT).show()
            return
        }
        // En el caso de que sí haya empleados registrados
        val empresaDoc = empresas[index]
        val empresaRef = empresaDoc.reference

        // Buscamos en la rama secundaria de usuarios por correo (id de los usuarios)
        empresaRef.collection("usuarios").document(correo)
            .get()
            .addOnSuccessListener { usuarioDoc ->

                // Verifica que la rama de usarios no esté vacía o no exista
                if (usuarioDoc != null && usuarioDoc.exists()) {
                    // Convierte a los uasuarios en objetos de la dataClass correspondiente
                    val usuario = usuarioDoc.toObject(Usuario::class.java) ?: return@addOnSuccessListener
                    //  Convierte a los uasuarios en objetos de la dataClass correspondiente
                    val empresa = empresaDoc.toObject(Empresa::class.java)
                    // Si falla la conversíon
                    if (empresa == null) {
                        Toast.makeText(this, "Error al obtener empresa", Toast.LENGTH_SHORT).show()
                        return@addOnSuccessListener
                    }
                    // En caso de no fallar
                    // Obtiene pisos de la empresa a la que el empleado corresponde
                    empresaRef.collection("pisos").get().addOnSuccessListener { pisosSnap ->
                        // Convierte la lista de pisos en objetos de la dataClass correspondiente
                        val listaPisos = pisosSnap.mapNotNull { it.toObject(Piso::class.java) }

                        // Obtener franjas horarias
                        empresaRef.collection("franjasHorarias").get()
                            .addOnSuccessListener { franjasSnap ->
                                val listaFranjas =
                                    // Convierte las franjas horarias en objetos de la dataClass correspondiente
                                    franjasSnap.mapNotNull { it.toObject(FranjaHoraria::class.java) }

                                // 🔐 GUARDAR LA SESIÓN EN MEMORIA CON SINGLETON
                                Sesion.datos = UsuarioSesion(
                                    empresa = empresa,
                                    usuario = usuario,
                                    pisos = listaPisos,
                                    franjasHorarias = listaFranjas
                                )

                                // Valida si un empleado es Jefe o no, cada uno tiene su activity porque no pueden hacer lo mismo
                                val intent = if (usuario.esJefe) {
                                    Toast.makeText(this, "Bienvenido Jefe", Toast.LENGTH_SHORT).show()
                                    Intent(this, Activity_menu_creador::class.java)
                                } else {
                                    Toast.makeText(this, "Bienvenido Empleado", Toast.LENGTH_SHORT).show()
                                    Intent(this, activity_menu_empleado::class.java)
                                }

                                // Inicia la actívity según el tipo de empleado
                                startActivity(intent)
                                finish()

                            // Crea excepción si da error la carga de los horarios
                            }.addOnFailureListener {
                            Toast.makeText(this, "Error al cargar franjas horarias", Toast.LENGTH_SHORT).show()
                        }
                    // Crea excepcioón si da error la carga se pisos
                    }.addOnFailureListener {
                        Toast.makeText(this, "Error al cargar pisos", Toast.LENGTH_SHORT).show()
                    }
                } else {

                    // No encontrado en esta empresa, buscar en la siguiente
                    buscarUsuarioRecursivo(empresas, correo, index + 1)
                }
            //Crea excepción si hay errpr al buscar un usiario
            }.addOnFailureListener {
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
