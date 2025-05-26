package com.example.tfg_kotlin

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.Button
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.bottomsheet.BottomSheetDialog

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayShowTitleEnabled(false)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        val btnIniciarSesion = findViewById<Button>(R.id.btnIniciarSesion)
        val btnRegistrarse = findViewById<Button>(R.id.btnRegistrarse)

        btnIniciarSesion.setOnClickListener {
            val bottomSheetDialog = BottomSheetDialog(this)
            val view = LayoutInflater.from(this).inflate(R.layout.bottomsheet_tipo_usuario, null)
            bottomSheetDialog.setContentView(view)

            val btnPersona = view.findViewById<Button>(R.id.btnPersona)
            val btnEmpresa = view.findViewById<Button>(R.id.btnEmpresa)

            btnPersona.setOnClickListener {
                bottomSheetDialog.dismiss()
                startActivity(Intent(this, LoginPersonaActivity::class.java))
            }

            btnEmpresa.setOnClickListener {
                bottomSheetDialog.dismiss()
                startActivity(Intent(this, LoginEmpresaActivity::class.java))
            }

            bottomSheetDialog.show()
        }

        btnRegistrarse.setOnClickListener {
            val bottomSheetDialog = BottomSheetDialog(this)
            val view = LayoutInflater.from(this).inflate(R.layout.bottomsheet_tipo_usuario_registro, null)
            bottomSheetDialog.setContentView(view)

            val btnPersona = view.findViewById<Button>(R.id.btnPersona)
            val btnEmpresa = view.findViewById<Button>(R.id.btnEmpresa)

            btnPersona.setOnClickListener {
                bottomSheetDialog.dismiss()
                startActivity(Intent(this, RegistroPersonaActivity::class.java))
            }

            btnEmpresa.setOnClickListener {
                bottomSheetDialog.dismiss()
                startActivity(Intent(this, RegistroEmpresaActivity::class.java))
            }

            bottomSheetDialog.show()
        }
    }
}