package com.example.tfg_kotlin

import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import android.widget.Button
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.room.Room
import com.example.tfg_kotlin.BBDD.BBDD
import com.example.tfg_kotlin.BBDD.MIGRATION_1_2
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout

class LoginPersonaActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_login_persona)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.loginpersona)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Asocia Toolbar como ActionBar
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        // Referencias a vistas
        val btnFinalizarLogin = findViewById<Button>(R.id.btnFinalizarLogin)
        val tilCorreo = findViewById<TextInputLayout>(R.id.tilCorreo)
        val tilContrasena = findViewById<TextInputLayout>(R.id.tilContrasena)
        val tilNumEmpresa = findViewById<TextInputLayout>(R.id.tilNumEmpresa)

        val etCorreo = findViewById<TextInputEditText>(R.id.etCorreo)
        val etContrasena = findViewById<TextInputEditText>(R.id.etContrasena)
        val etNumEmpresa = findViewById<TextInputEditText>(R.id.etNumEmpresa)

        btnFinalizarLogin.setOnClickListener {
            val validado = Validaciones.validarLoginPersona(
                tilCorreo, etCorreo,
                tilContrasena, etContrasena,
                tilNumEmpresa, etNumEmpresa
            )

            if (validado) {
                val db = Room.databaseBuilder(
                    applicationContext,
                    BBDD::class.java,
                    "reservas_db"
                ).addMigrations(MIGRATION_1_2)
                    .allowMainThreadQueries()
                    .build()

                val correo = etCorreo.text.toString()
                val contrasena = etContrasena.text.toString()
                val nifEmpresa = etNumEmpresa.text.toString()

                val empleado = db.appDao().loginEmpleado(correo, contrasena)
                val empresa = db.appDao().existeEmpresaConNif(nifEmpresa)

                if (empleado != null && empresa != null) {
                    startActivity(Intent(this, MainActivity::class.java))
                    finish()
                    Toast.makeText(this, "Bienvenido ${empleado.nombre}", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "Datos incorrectos o empresa no registrada", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    // Flecha TOOLBAR "ATRÁS"
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
