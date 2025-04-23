package com.example.tfg_kotlin

import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        // Ajustes de padding para las barras del sistema
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // 🔥 Configurar la Toolbar
        val toolbar = findViewById<Toolbar>(R.id.my_toolbar)
        setSupportActionBar(toolbar)

        // 🔥 Activar el botón de volver atrás (flecha)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
    }

    // 🔥 Inflar el menú de la Toolbar
    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_drag, menu)
        Log.d("MainActivity", "Menú inflado correctamente")
        return true
    }

    // 🔥 Manejar las acciones de los botones de la Toolbar
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                onBackPressedDispatcher.onBackPressed()
                true
            }
            R.id.action_add_image -> {
                Toast.makeText(this, "Añadir imagen pulsado", Toast.LENGTH_SHORT).show()
                true
            }
            R.id.action_add -> {
                Toast.makeText(this, "Añadir elemento pulsado", Toast.LENGTH_SHORT).show()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
}