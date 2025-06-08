package com.example.tfg_kotlin

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class Principal : AppCompatActivity() {
    private lateinit var btnEmpresa: Button
    private lateinit var btnEmpleado: Button
    private lateinit var btnLogin: Button

    //Actividad principal, redirige con los botonea a las distintas activides
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_principal)

        enableEdgeToEdge()
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.principalLayout)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        btnEmpresa = findViewById(R.id.btnRegistroEmpresa)
        btnEmpleado = findViewById(R.id.btnRegistroEmpleado)
        btnLogin = findViewById(R.id.btnIniciarSesion)

        btnEmpresa.setOnClickListener {
            startActivity(Intent(this, RegistroEmpresa::class.java))
        }

        btnEmpleado.setOnClickListener {
            startActivity(Intent(this, RegistroEmpleado::class.java))
        }

        btnLogin.setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
        }
    }
}