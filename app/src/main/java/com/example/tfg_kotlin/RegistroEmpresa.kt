package com.example.tfg_kotlin

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.room.Room
import com.example.tfg_kotlin.BBDD.DB_Empresa
import com.example.tfg_kotlin.BBDD.DB_Global
import com.example.tfg_kotlin.BBDD.Operaciones
import com.example.tfg_kotlin.BBDD.TablaEmpresa

class RegistroEmpresa : AppCompatActivity() {

    private lateinit var editNombre: EditText
    private lateinit var editDominio: EditText
    private lateinit var editCif: EditText
    private lateinit var btnRegistrar: Button
    private lateinit var database: Operaciones

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_registro_empresa)

        database = Room.databaseBuilder(
            applicationContext,
            DB_Empresa::class.java,
            "reservas_db"
        ).allowMainThreadQueries().build().appDao()

        // Referencias a las vistas
        val nombre = findViewById<EditText>(R.id.editNombreEmpresa)
        val editDominio = findViewById<EditText>(R.id.editDominio)
        val editCif = findViewById<EditText>(R.id.editCif)
        val btnRegistrar = findViewById<Button>(R.id.btnRegistrarEmpresa)

        // Acción al pulsar el botón
        btnRegistrar.setOnClickListener {
            registrarEmpresa()
        }
    }

    private fun registrarEmpresa() {
        val nombre = editNombre.text.toString().trim()
        val dominio = editDominio.text.toString().trim()
        val cif = editCif.text.toString().trim().uppercase()


        // Validaciones básicas
        // Validacion comprobacion todos los dato en los textView
        if (nombre.isEmpty() || dominio.isEmpty() || cif.isEmpty()) {
            Toast.makeText(this, "Completa todos los campos", Toast.LENGTH_SHORT).show()
            return
        }

        // Validacion formato dominio
        if (!dominio.startsWith("@") || !dominio.contains(".")) {
            Toast.makeText(this, "Dominio inválido (ej: @empresa.com)", Toast.LENGTH_SHORT).show()
            return
        }


        // Evitar registros duplicados
        val empresaExistente = database.buscarPorCif(cif)
        if (empresaExistente != null) {
            Toast.makeText(this, "Ya existe una empresa con ese CIF", Toast.LENGTH_SHORT).show()
            return
        }

        // Crear nueva empresa
        val nuevaEmpresa = TablaEmpresa(
            cif = cif,
            nombre = nombre,
            dominio = dominio
        )

        database.insertarEmpresa(nuevaEmpresa)

        Toast.makeText(this, "Empresa registrada correctamente", Toast.LENGTH_LONG).show()

        // Finaliza la actividad y vuelve atrás
        finish()
    }
}