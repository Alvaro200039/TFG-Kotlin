package com.example.tfg_kotlin

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.room.Room
import com.example.tfg_kotlin.BBDD.DB_Global
import com.example.tfg_kotlin.BBDD.Operaciones

class LoginActivity : AppCompatActivity() {
    lateinit var database: Operaciones

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        database = Room.databaseBuilder(
            applicationContext,
            DB_Global::class.java,
            "reservas_db"
        ).allowMainThreadQueries() // Solo para pruebas
            .build().appDao()


        val etNombre = findViewById<EditText>(R.id.etNombre)
        val etContrasena = findViewById<EditText>(R.id.etContrasena)
        val btnLogin = findViewById<Button>(R.id.btnLogin)
        val btnRegistro = findViewById<Button>(R.id.btnRegistro)


        btnLogin.setOnClickListener {
            val nombre = etNombre.text.toString()
            val contrasena = etContrasena.text.toString()

            val usuario = database.login(nombre, contrasena)
            if (usuario != null) {
                if (usuario.esJefe) {
                    startActivity(Intent(this, PantallaJefeActivity::class.java))
                } else {
                    startActivity(Intent(this, PantallaEmpleadoActivity::class.java))
                }
                finish()
            } else {
                Toast.makeText(this, "Usuario o contraseña incorrectos", Toast.LENGTH_SHORT).show()
            }
        }
        btnRegistro.setOnClickListener {
            startActivity(Intent(this, RegistroActivity::class.java))
        }
    }
}
