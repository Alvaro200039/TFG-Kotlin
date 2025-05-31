package com.example.tfg_kotlin

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.room.Room
import com.example.tfg_kotlin.BBDD.DB_Empresa
import com.example.tfg_kotlin.BBDD.DB_Maestra
import com.example.tfg_kotlin.BBDD.TablaEmpleados
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

    private fun registrarEmpleados(){
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
                DB_Maestra::class.java,
                "db_maestra")
                .build()

            // Buscamos si el domninio existe
            val daoMaestra = dbMaestra.userDao()
            val empresa = daoMaestra.buscarPorDominio(dominioarroba)

            //En caso de no existir, volvera a solicitar el correo
            if (empresa == null) {
                runOnUiThread {
                    Toast.makeText(this@RegistroEmpleado, "El dominio introducido no existe o es incorrecto", Toast.LENGTH_SHORT).show()
                }
                return@launch
            }

            //Creamos el acceso a la bd_individual por empresa
            val dbnombre = "db_${nombre.lowercase().replace(" ", "_")}"
            val dbEmpresa = Room.databaseBuilder(applicationContext,DB_Empresa::class.java, dbnombre).build()


            val daoEmpleado = dbEmpresa.appDao()

            // Verifica si el correo ya está registrado
            val empleadoExistente = daoEmpleado.obtenerPorCorreo(correo)
            if (empleadoExistente != null) {
                runOnUiThread {
                    Toast.makeText(this@RegistroEmpleado, "Correo ya registrado", Toast.LENGTH_SHORT).show()
                }
                return@launch
            }

            // Determina si es Jefe
            val esJefe = empresa.cif.equals(etCif)

            // Creamos al emprpleado y lo insertamos en la tabla de emplados
            val empleado = TablaEmpleados(
                correo = correo,
                nombre = nombre,
                apellidos = apellidos,
                contrasena = contrasena,
                cif = cif,
                esJefe = esJefe
            )

            val id = daoEmpleado.insertarEmpleado(empleado)

            runOnUiThread {
                val mensaje = if (esJefe) {
                    "Empleado registrado como Jefe (ID: $id)"
                } else {
                    "Empleado registrado (ID: $id)"
                }
                Toast.makeText(this@RegistroEmpleado, mensaje, Toast.LENGTH_SHORT).show()
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
