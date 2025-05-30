package com.example.tfg_kotlin

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.tfg_kotlin.BBDD.DB_Empresa
import com.example.tfg_kotlin.BBDD.TablaEmpleados
import com.example.tfg_kotlin.BBDD.Operaciones

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
/*
        etCorreo = findViewById(R.id.etCorreo)
        etCif = findViewById(R.id.etCif)
        etNombre = findViewById(R.id.etNombre)
        etApellidos = findViewById(R.id.etApellidos)
        etContrasena = findViewById(R.id.etContrasena)
        etRepetirContrasena = findViewById(R.id.etRepetirContrasena)
        btnRegistrar = findViewById(R.id.btnRegistrar)


        btnRegistrar.setOnClickListener {
            registrarEmpleados()
        }*/
    }
/*
    private fun registrarEmpleados(){
        val correo = etCorreo.text.toString()
        val cif = etCif.text.toString()
        val contrasena = etContrasena.text.toString()
        val repetirContrasena = etRepetirContrasena.text.toString()
        val nombre = etNombre.text.toString()
        val apellidos = etApellidos.text.toString()

        if (correo.isEmpty() || contrasena.isEmpty() || nombre.isEmpty() || apellidos.isEmpty() || cif.isEmpty()) {
            Toast.makeText(this, "Completa todos los campos", Toast.LENGTH_SHORT).show()
            return
        }

        if (!correo.contains("@")) {
            Toast.makeText(this, "Correo inválido", Toast.LENGTH_SHORT).show()
            return
        }

        if (contrasena != repetirContrasena) {
            Toast.makeText(this, "Las contraseñas no coinciden", Toast.LENGTH_SHORT).show()
            return
        }

        val dominioCorreo = "@" + correo.substringAfter("@")

        // Obtener empresa desde la BD maestra
        val dbMaestra = DB_Empresa.getInstance(this)
        val empresaDao = dbMaestra.appDao()
        val empresa = empresaDao.buscarPorDominio(dominioCorreo)

        if (empresa == null) {
            Toast.makeText(this, "No existe una empresa con ese dominio", Toast.LENGTH_SHORT).show()
            return
        }

        // Verificar si el CIF coincide
        val esJefe = empresa.cif == cif

        val nuevoEmpleado = TablaEmpleados(
            correo = correo,
            cif = cif,
            contrasena = contrasena,
            nombre = nombre,
            apellidos = apellidos,
            esJefe = esJefe
        )

        val dao = dbMaestra.appDao()
        dao.insertarEmpleado(nuevoEmpleado)
        Toast.makeText(this, "Usuario registrado", Toast.LENGTH_SHORT).show()
        startActivity(Intent(this, LoginActivity::class.java))
        finish()
    }
*/
}
