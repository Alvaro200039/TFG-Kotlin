package com.example.tfg_kotlin

import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

class RegistroEmpresa : AppCompatActivity() {

    //Creación de varibles globales
    private lateinit var editNombre: EditText
    private lateinit var editDominio: EditText
    private lateinit var editCif: EditText
    private lateinit var btnRegistrar: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_registro_empresa)

        enableEdgeToEdge()
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.registroempresaLayout)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Toolbar añadimos el icono y funcionalidad del botón y título
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Registro de Empresas"
        supportActionBar?.setHomeAsUpIndicator(R.drawable.ic_adagora)

        // Hacemos referencia a los editText y los renombramos para el uso
        editNombre = findViewById(R.id.editNombreEmpresa)
        editDominio = findViewById(R.id.editDominio)
        editCif = findViewById(R.id.editCif)
        btnRegistrar = findViewById(R.id.btnRegistrarEmpresa)

        // Damos funcionalidad al botón de registrar
        btnRegistrar.setOnClickListener {
            registrarEmpresa()
        }
    }


    private fun registrarEmpresa() {
        // Guardamos los datos introducidos em el editText en variables para su posterior uso
        val nombre = editNombre.text.toString().trim()
        val dominio = editDominio.text.toString().trim()
        val cif = editCif.text.toString().trim().uppercase()

        // Validación de campos vacíos en el editText
        if (nombre.isEmpty() || dominio.isEmpty() || cif.isEmpty()) {
            Toast.makeText(this, "Completa todos los campos", Toast.LENGTH_SHORT).show()
            return
        }

        // Validación del formato del dominio
        if (!dominio.startsWith("@") || !dominio.contains(".")) {
            Toast.makeText(this, "Dominio inválido (ej: @empresa.com)", Toast.LENGTH_SHORT).show()
            return
        }

        // Creamos el acceso a la base de datos de FireStore
        val firestore = Firebase.firestore
        // Decimos que acceda a la colección de empresas (rama padre)
        val empresasRef = firestore.collection("empresas")

        // Para poder registrar una empresa, no pueden coincidir en nombre, dominio y cif dos empresas distintas

        // Comprovamos que no exista el nombre de empresa en la base deatos
        empresasRef.whereEqualTo("nombre", nombre).get()
            .addOnSuccessListener { nombreSnapshot ->
                if (!nombreSnapshot.isEmpty) {
                    Toast.makeText(this, "Nombre de empresa ya registrado", Toast.LENGTH_SHORT).show()
                    editNombre.requestFocus()
                } else {
                    // Comprobamos si el dominio ya existe
                    empresasRef.whereEqualTo("dominio", dominio).get()
                        .addOnSuccessListener { dominioSnapshot ->
                            if (!dominioSnapshot.isEmpty) {
                                Toast.makeText(this, "Dominio existente", Toast.LENGTH_SHORT).show()
                                editDominio.requestFocus()
                            } else {
                                //Comprobamos si el cif existe
                                empresasRef.whereEqualTo("cif", cif).get()
                                    .addOnSuccessListener { cifSnapshot ->
                                        if (!cifSnapshot.isEmpty){
                                            Toast.makeText(this, "CIF existente", Toast.LENGTH_SHORT).show()
                                            editCif.requestFocus()
                                        } else {
                                            // Se almacenan los datos de los editText en los campos correspondinetes
                                            val empresaMap = hashMapOf(
                                                "cif" to cif,
                                                "nombre" to nombre,
                                                "dominio" to dominio
                                            )

                                            // decimos que la segunda rama de la base de datos,
                                            // se guarde el nombre de empresa como ID
                                            empresasRef.document(nombre).set(empresaMap)
                                                .addOnSuccessListener {
                                                    // creamos un aviso para saber si se ha registrado la empresa
                                                    Toast.makeText(this, "Empresa registrada", Toast.LENGTH_SHORT).show()
                                                    // se ponen en blanco los editText tras registrar la empresa
                                                    editCif.text.clear()
                                                    editNombre.text.clear()
                                                    editDominio.text.clear()
                                                    // cremos un log interno para poder ver si la empresa se ha registrado correctamente
                                                    Log.d("DB_LOG", "Empresa registrada: CIF=$cif, Nombre=$nombre, Dominio=$dominio")
                                                }
                                                // añadimos una excepción en caso de que falle el registro de la empresa
                                                .addOnFailureListener { e ->
                                                    Toast.makeText(this, "Error al registrar empresa", Toast.LENGTH_SHORT).show()
                                                    e.printStackTrace()
                                                }
                                        }
                                    }
                                    // añadimos una excepción en caso de que falle el la validación del cif
                                    .addOnFailureListener { e ->
                                        Toast.makeText(this, "Error validando CIF", Toast.LENGTH_SHORT).show()
                                        e.printStackTrace()
                                    }
                            }
                        }// añadimos una excepción en caso de que falle el la validación del dominio
                        .addOnFailureListener { e ->
                            Toast.makeText(this, "Error validando dominio", Toast.LENGTH_SHORT).show()
                            e.printStackTrace()
                        }
                }
            }
            // añadimos una excepción en caso de que falle el la validación del nombre de empresa
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error validando nombre de empresa", Toast.LENGTH_SHORT).show()
                e.printStackTrace()
            }
    }

    // Funcionalidad de la toolbar para ir a la pantalla anterior
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
