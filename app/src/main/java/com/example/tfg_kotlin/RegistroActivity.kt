package com.example.tfg_kotlin

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.room.Room
import com.example.tfg_kotlin.BBDD.BBDD
import com.example.tfg_kotlin.BBDD.Empleados
import com.example.tfg_kotlin.BBDD.Operaciones

class RegistroActivity : AppCompatActivity() {

    private lateinit var database: Operaciones

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_registro)

        database = Room.databaseBuilder(
            applicationContext,
            BBDD::class.java,
            "reservas_db"
        ).allowMainThreadQueries().build().appDao()


        val etNombre = findViewById<EditText>(R.id.etNombre)
        val etApellidos = findViewById<EditText>(R.id.etApellidos)
        val etSeccion = findViewById<EditText>(R.id.etSeccion)
        val etContrasena = findViewById<EditText>(R.id.etContrasena)
        val etRepetirContrasena = findViewById<EditText>(R.id.etRepetirContrasena)
        val checkEsJefe = findViewById<CheckBox>(R.id.checkEsJefe)
        val btnRegistrar = findViewById<Button>(R.id.btnRegistrar)


        btnRegistrar.setOnClickListener {
            val nombre = etNombre.text.toString()
            val apellidos = etApellidos.text.toString()
            val seccion = etSeccion.text.toString()
            val contrasena = etContrasena.text.toString()
            val repetirContrasena = etRepetirContrasena.text.toString()
            val esJefe = checkEsJefe.isChecked

            if (contrasena != repetirContrasena) {
                Toast.makeText(this, "Las contraseñas no coinciden", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (database.buscarEmpleadoPorNombre(nombre) != null) {
                Toast.makeText(this, "Ese nombre ya está registrado", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val nuevoEmpleado = Empleados(
                nombre = nombre,
                apellidos = apellidos,
                seccion = seccion,
                contrasena = contrasena,
                esJefe = esJefe
            )
            database.insertarEmpleado(nuevoEmpleado)
            Toast.makeText(this, "Usuario registrado", Toast.LENGTH_SHORT).show()
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }
    }
}