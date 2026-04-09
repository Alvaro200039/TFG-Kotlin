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

    // Actividad principal, redirige con los botones a las distintas actividades
    override fun onCreate(savedInstanceState: Bundle?) {
        val prefs = getSharedPreferences("ajustes_usuario", MODE_PRIVATE)
        val savedTheme = prefs.getInt("tema_app", androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
        androidx.appcompat.app.AppCompatDelegate.setDefaultNightMode(savedTheme)

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
            startActivity(Intent(this, RegistroEmpresaActivity::class.java))
        }

        btnEmpleado.setOnClickListener {
            startActivity(Intent(this, RegistroEmpleadoActivity::class.java))
        }

        btnLogin.setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
        }
    }
}