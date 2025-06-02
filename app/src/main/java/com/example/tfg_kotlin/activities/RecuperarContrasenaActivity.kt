package com.example.tfg_kotlin.activities

import android.os.Bundle
import android.view.MenuItem
import android.widget.*
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.ViewModelProvider
import com.example.tfg_kotlin.R
import com.example.tfg_kotlin.utils.Validaciones.construirNombreBD
import com.example.tfg_kotlin.database.BBDDMaestra
import androidx.lifecycle.lifecycleScope
import com.example.tfg_kotlin.database.AppDatabase
import com.example.tfg_kotlin.repository.RecuperarContrasenaRepository
import com.example.tfg_kotlin.viewmodel.RecuperarContrasenaViewModel
import com.example.tfg_kotlin.viewmodel.RecuperarContrasenaViewModelFactory
import kotlinx.coroutines.launch

class RecuperarContrasenaActivity : AppCompatActivity() {

    private lateinit var layoutPaso1: LinearLayout
    private lateinit var layoutPaso2: LinearLayout
    private lateinit var etCorreo: EditText
    private lateinit var btnEnviarCodigo: Button
    private lateinit var etCodigo: EditText
    private lateinit var etNuevaPass: EditText
    private lateinit var etRepetirPass: EditText
    private lateinit var btnCambiar: Button

    private var codigoGenerado: String = ""
    private var correo: String = ""
    private lateinit var db: BBDDMaestra
    private lateinit var viewModel: RecuperarContrasenaViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_recuperar_contrasena)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.recuperarcontrasena)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        // Toolbar
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        inicializarVistas()
        configurarPaso1()
        configurarPaso2()
    }

    private fun inicializarVistas() {
        layoutPaso1 = findViewById(R.id.layoutPaso1)
        layoutPaso2 = findViewById(R.id.layoutPaso2)

        etCorreo = findViewById(R.id.etCorreoRec)
        btnEnviarCodigo = findViewById(R.id.btnEnviarCodigo)

        etCodigo = findViewById(R.id.etCodigo)
        etNuevaPass = findViewById(R.id.etNuevaPass)
        etRepetirPass = findViewById(R.id.etRepetirPass)
        btnCambiar = findViewById(R.id.btnCambiarPass)
    }


    private fun configurarPaso1() {
        val dominio = correo.substringAfter("@")
        val nombreBD = construirNombreBD(dominio)
        val db = AppDatabase.getInstance(applicationContext, nombreBD)
        val repository = RecuperarContrasenaRepository(db.recuperarContrasenaDao())
        val factory = RecuperarContrasenaViewModelFactory(repository)
        viewModel = ViewModelProvider(this, factory)[RecuperarContrasenaViewModel::class.java]

        btnEnviarCodigo.setOnClickListener {
            correo = etCorreo.text.toString().trim()

            if (correo.isEmpty() || !correo.contains("@")) {
                etCorreo.error = "Introduce un correo válido"
                return@setOnClickListener
            }

            viewModel.verificarCorreo(correo)
        }

        viewModel.codigoGenerado.observe(this) { codigo ->
            codigo?.let {
                Toast.makeText(this, "Tu código es: $it", Toast.LENGTH_LONG).show()
                layoutPaso1.visibility = LinearLayout.GONE
                layoutPaso2.visibility = LinearLayout.VISIBLE
            }
        }

        viewModel.error.observe(this) {
            if (it != null) {
                etCorreo.error = it
                viewModel.limpiarEstado()
            }
        }
        }


    private fun configurarPaso2() {
        btnCambiar.setOnClickListener {
            val codigoIngresado = etCodigo.text.toString().trim()
            val nuevaPass = etNuevaPass.text.toString().trim()
            val repetirPass = etRepetirPass.text.toString().trim()
            val codigoEsperado = viewModel.codigoGenerado.value

            if (codigoIngresado != codigoEsperado) {
                etCodigo.error = "El código no es correcto"
                return@setOnClickListener
            }

            if (nuevaPass.length < 4) {
                etNuevaPass.error = "La contraseña debe tener al menos 4 caracteres"
                return@setOnClickListener
            }

            if (nuevaPass != repetirPass) {
                etRepetirPass.error = "Las contraseñas no coinciden"
                return@setOnClickListener
            }
            viewModel.cambiarContrasena(correo, nuevaPass)

            viewModel.cambioExitoso.observe(this) {
                if (it) {
                    Toast.makeText(this, "Contraseña actualizada correctamente", Toast.LENGTH_SHORT).show()
                    finish()
                }
            }
        }
    }


    // Flecha "Atrás"
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