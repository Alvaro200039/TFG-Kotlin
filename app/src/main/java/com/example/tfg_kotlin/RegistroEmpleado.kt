package com.example.tfg_kotlin

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import androidx.room.Room
import com.example.tfg_kotlin.BBDD_Global.Database.GlobalDB
import com.example.tfg_kotlin.BBDD_Global.Entities.Usuario
import com.example.tfg_kotlin.BBDD_Maestra.Database.MasterDB
import kotlinx.coroutines.launch

class RegistroEmpleado : AppCompatActivity() {
    private lateinit var etCorreo: EditText
    private lateinit var etContrasena: EditText
    private lateinit var etRepetirContrasena: EditText
    private lateinit var etNombre: EditText
    private lateinit var etApellidos: EditText
    private lateinit var etCif: EditText
    private lateinit var btnRegistrar: Button

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


        btnRegistrar.setOnClickListener {
            registrarEmpleados()
        }
    }

    private fun registrarEmpleados() {
        val correo = etCorreo.text.toString()
        val cif = etCif.text.toString()
        val contrasena = etContrasena.text.toString()
        val repetirContrasena = etRepetirContrasena.text.toString()
        val nombre = etNombre.text.toString()
        val apellidos = etApellidos.text.toString()

        // Validación de campos vacios
        if (correo.isEmpty() || contrasena.isEmpty() || repetirContrasena.isEmpty() || nombre.isEmpty() || apellidos.isEmpty()) {
            Toast.makeText(this, "Completa todos los campos", Toast.LENGTH_SHORT).show()
            return
        }

        // Validación formato correo
        if (!correo.contains("@")) {
            Toast.makeText(this, "Correo inválido", Toast.LENGTH_SHORT).show()
            return
        }

        // Validacion contraseñas son iguales
        if (contrasena != repetirContrasena) {
            Toast.makeText(this, "Las contraseñas no coinciden", Toast.LENGTH_SHORT).show()
            return
        }

        // Extracción del dominio en el correo
        val dominio = correo.substringAfterLast("@")
        val dominioarroba = "@$dominio"

        lifecycleScope.launch {
            // Accedemos a la BD Maestra
            val dbMaestra = Room.databaseBuilder(
                applicationContext,
                MasterDB::class.java,
                "db_maestra.db"
            ).build()

            // Buscamos si el domninio existe
            val daoMaestra = dbMaestra.empresaDao()
            val empresa = daoMaestra.buscarPorDominio(dominioarroba)

            //En caso de no existir, volvera a solicitar el correo
            if (empresa == null) {
                runOnUiThread {
                    Toast.makeText(this@RegistroEmpleado, "El dominio introducido no existe o es incorrecto", Toast.LENGTH_SHORT).show()
                }
                return@launch
            }

            // Si se introdujo un CIF, validar que coincide con el de la empresa
            if (cif.isNotEmpty()) {
                val cifValido = empresa.cif.equals(cif, ignoreCase = true)
                if (!cifValido) {
                    runOnUiThread {
                        Toast.makeText(this@RegistroEmpleado, "El CIF no coincide con el dominio. Corrígelo para continuar.", Toast.LENGTH_SHORT).show()
                        etCif.requestFocus()
                    }
                    return@launch
                }
            }

            //Creamos el acceso a la bd_individual por empresa
            val dbnombre = "db_${empresa.nombre.lowercase().replace(" ", "_")}"
            val dbEmpresa = Room.databaseBuilder(
                applicationContext,
                GlobalDB::class.java, "$dbnombre.db"
            ).build()

            val daoEmpleado = dbEmpresa.usuarioDao()

            // Verifica si el correo ya está registrado
            val empleadoExistente = daoEmpleado.obtenerPorCorreo(correo)
            if (empleadoExistente != null) {
                runOnUiThread {
                    Toast.makeText(
                        this@RegistroEmpleado,
                        "Correo ya registrado",
                        Toast.LENGTH_SHORT
                    ).show()
                }
                return@launch
            }

            // Determina si es Jefe
            val esJefe = empresa.cif.lowercase() == cif.lowercase()

            // Creamos al emprpleado y lo insertamos en la tabla de emplados
            val empleado = Usuario(
                email = correo,
                nombre = nombre,
                apellidos = apellidos,
                contrasena = contrasena,
                cif = cif,
                esJefe = esJefe
            )
            daoEmpleado.insertarUsuario(empleado)

            runOnUiThread {
                if (esJefe) {
                    Toast.makeText(this@RegistroEmpleado, "Empleado registrado como Jefe", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this@RegistroEmpleado, "Empleado registrado", Toast.LENGTH_SHORT).show()
                }
                etCorreo.text.clear()
                etContrasena.text.clear()
                etRepetirContrasena.text.clear()
                etNombre.text.clear()
                etApellidos.text.clear()
                etCif.text.clear()
            }
        }
    }
}