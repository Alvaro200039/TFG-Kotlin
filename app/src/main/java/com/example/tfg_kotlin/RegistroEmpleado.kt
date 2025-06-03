package com.example.tfg_kotlin

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.tfg_kotlin.BBDD_Global.Database.GlobalDBManager
import com.example.tfg_kotlin.BBDD_Global.Entities.Usuario
import com.example.tfg_kotlin.BBDD_Maestra.Database.MasterDBManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

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
            lifecycleScope.launch {
                registrarEmpleados()
            }
        }
    }

    private suspend fun registrarEmpleados() {
        val correo = etCorreo.text.toString()
        val cif = etCif.text.toString()
        val contrasena = etContrasena.text.toString()
        val repetirContrasena = etRepetirContrasena.text.toString()
        val nombre = etNombre.text.toString()
        val apellidos = etApellidos.text.toString()

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
        val dominioarroba = "@$dominio"

        val dbMaestra = MasterDBManager.getDatabase(applicationContext)
        val daoMaestra = dbMaestra.empresaDao()
        val empresa = daoMaestra.buscarPorDominio(dominioarroba)

        if (empresa == null) {
            mostrarToast("El dominio introducido no existe o es incorrecto")
            return
        }

        if (cif.isNotEmpty() && !empresa.cif.equals(cif, ignoreCase = true)) {
            mostrarToast("El CIF no coincide con el dominio. Corrígelo para continuar.")
            withContext(Dispatchers.Main) {
                etCif.requestFocus()
            }
            return
        }

        val dbnombre = "db_${empresa.nombre.lowercase().replace(" ", "_")}"
        val dbEmpresa = GlobalDBManager.getDatabase(applicationContext, dbnombre)
        val daoEmpleado = dbEmpresa.usuarioDao()

        val empleadoExistente = daoEmpleado.obtenerPorCorreo(correo)
        if (empleadoExistente != null) {
            mostrarToast("Correo ya registrado")
            return
        }

        val esJefe = empresa.cif.lowercase() == cif.lowercase()
        val empleado = Usuario(
            email = correo,
            nombre = nombre,
            apellidos = apellidos,
            contrasena = contrasena,
            cif = cif,
            esJefe = esJefe
        )
        daoEmpleado.insertarUsuario(empleado)

        withContext(Dispatchers.Main) {
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

    private suspend fun mostrarToast(mensaje: String) {
        withContext(Dispatchers.Main) {
            Toast.makeText(this@RegistroEmpleado, mensaje, Toast.LENGTH_SHORT).show()
        }
    }

}