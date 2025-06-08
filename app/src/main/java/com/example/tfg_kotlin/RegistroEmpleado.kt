package com.example.tfg_kotlin

import android.os.Bundle
import android.view.MenuItem
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import androidx.appcompat.widget.Toolbar

class RegistroEmpleado : AppCompatActivity() {

    // Creación de variables globales
    private lateinit var etCorreo: EditText
    private lateinit var etContrasena: EditText
    private lateinit var etRepetirContrasena: EditText
    private lateinit var etNombre: EditText
    private lateinit var etApellidos: EditText
    private lateinit var etCif: EditText
    private lateinit var btnRegistrar: Button

    // Variable global para acceder a firebase
    private lateinit var auth: FirebaseAuth
    private val firestore = Firebase.firestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_registro)

        enableEdgeToEdge()
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.registroempleadoLayout)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Toolbar con funcionalidad, nombre e icono
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Registro de Empleados"
        supportActionBar?.setHomeAsUpIndicator(R.drawable.ic_adagora)

        // Hacemos referencia a los editText y los renombramos para el uso
        etCorreo = findViewById(R.id.etCorreo)
        etCif = findViewById(R.id.etCif)
        etNombre = findViewById(R.id.etNombre)
        etApellidos = findViewById(R.id.etApellidos)
        etContrasena = findViewById(R.id.etContrasena)
        etRepetirContrasena = findViewById(R.id.etRepetirContrasena)
        btnRegistrar = findViewById(R.id.btnRegistrar)

        // Instanciamos firebase para su uso
        auth = FirebaseAuth.getInstance()

        // Botón para registrar el empleado
        btnRegistrar.setOnClickListener {
            registrarEmpleado()
        }
    }

    private fun registrarEmpleado() {

        // Variable local de la función pare hacer uso de los detos de los editText
        val correo = etCorreo.text.toString().trim()
        val cif = etCif.text.toString().trim()
        val contrasena = etContrasena.text.toString()
        val repetirContrasena = etRepetirContrasena.text.toString()
        val nombre = etNombre.text.toString().trim()
        val apellidos = etApellidos.text.toString().trim()

        //Validación de que todos los campos obligatorios están introdicidos
        if (correo.isEmpty() || contrasena.isEmpty() || repetirContrasena.isEmpty() || nombre.isEmpty() || apellidos.isEmpty()) {
            mostrarToast("Completa todos los campos")
            return
        }

        // Validación del formato del correo
        if (!correo.contains("@")|| !correo.contains(".")) {
            mostrarToast("Formato de correo inválido")
            return
        }

        // Validación de que la contraseña y repetir contraseña sean iguales
        if (contrasena != repetirContrasena) {
            mostrarToast("Las contraseñas no coinciden")
            return
        }

        // tomamos el domionio del correo y lo guardamos en una variable
        val dominio = correo.substringAfterLast("@")
        val dominioConArroba = "@$dominio"

        // Nos situamos en la rama de la BD de "empresas" (rama principal)
        val empresasRef = firestore.collection("empresas")

        // Validamos que el dominio de una empresa y el gominio introducido con el correo son iguales
        empresasRef.whereEqualTo("dominio", dominioConArroba).get()
            .addOnSuccessListener { empresasSnapshot ->
                // Si no encuentra el domniono sale el siguiente mensaje
                if (empresasSnapshot.isEmpty) {
                    mostrarToast("El dominio introducido no existe o es incorrecto")
                } else {
                    // En caso de encontrar el dominio, usará la rama de las empresas registradas para comprobar los siquientes datos
                    val empresaDoc = empresasSnapshot.documents[0]
                    // Busca el cif en la BD
                    val empresaCif = empresaDoc.getString("cif") ?: ""
                    // En caso de coincidir el etCif y el cif de la BD el empleado sería Jefe
                    val esJefe = cif.equals(empresaCif, ignoreCase = true)
                    // Se guarda el valor del cif sea o no correcto
                    val cifFinal = empresaCif
                    // Valida que la persona sea jefe y si el cif coincide con el de la BD
                    if (esJefe && cif != empresaCif) {
                        mostrarToast("El CIF no coincide con el dominio. Corrígelo para continuar.")
                        etCif.requestFocus()
                        return@addOnSuccessListener
                    }

                    // Creamos el usuario en Firebase Authentication
                    auth.createUserWithEmailAndPassword(correo, contrasena)
                        .addOnCompleteListener { task ->
                            if (task.isSuccessful) {
                                // Se crea un id Personal para cada usuario
                                val uid = auth.currentUser?.uid ?: ""

                                // Se almacenan los datos de los editText en los campos definidos de la dataClass
                                val nuevoUsuario = hashMapOf(
                                    "email" to correo,
                                    "cif" to cifFinal,
                                    "esJefe" to esJefe,
                                    "nombre" to nombre,
                                    "apellidos" to apellidos,
                                    "uid" to uid
                                )

                                // Guardamos datos extra en Firestore, colección "usuarios", tomamos como id el correo del usuario
                                empresaDoc.reference.collection("usuarios").document(correo)
                                    .set(nuevoUsuario)
                                    .addOnSuccessListener {
                                        // Si es jefe
                                        if (esJefe) {
                                            mostrarToast("Empleado registrado como Jefe")
                                        // Si es empleado
                                        } else {
                                            mostrarToast("Empleado registrado")
                                        }
                                        //
                                        limpiarCampos()
                                    }
                                    // Añadimos excepción en caso de que haya un error al registrar al empleado
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
            // Añadimos una excepción en caso de que el dominio del correo no corresponda con el dominio de una emresa de la BD
            .addOnFailureListener { e ->
                mostrarToast("Error buscando empresa")
                e.printStackTrace()
            }
    }

    // Función que del toast que muestra qué tipo de empleado es
    private fun mostrarToast(mensaje: String) {
        Toast.makeText(this@RegistroEmpleado, mensaje, Toast.LENGTH_SHORT).show()
    }

    // Función que pone los campos en blanco
    private fun limpiarCampos() {
        etCorreo.text.clear()
        etContrasena.text.clear()
        etRepetirContrasena.text.clear()
        etNombre.text.clear()
        etApellidos.text.clear()
        etCif.text.clear()
    }

    // Se le añade la funcionalidad al botón de la toolbar
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

