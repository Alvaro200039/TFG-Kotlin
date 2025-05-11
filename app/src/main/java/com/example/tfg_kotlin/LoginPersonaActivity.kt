package com.example.tfg_kotlin

import android.R
import android.os.Bundle
import android.view.MenuItem
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.tfg_kotlin.databinding.ActivityLoginPersonaBinding
import com.example.tfg_kotlin.databinding.ActivityMainBinding
import com.example.tfg_kotlin.Validaciones



class LoginPersonaActivity : AppCompatActivity() {
    private lateinit var binding: ActivityLoginPersonaBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

    //Flecha TOOLBAR "ATRAS"
        binding = ActivityLoginPersonaBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Asocia tu Toolbar como ActionBar
        setSupportActionBar(binding.toolbar)

        //Muestra la flecha
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        // (opcional) cambiar el icono por defecto:
        // supportActionBar?.setHomeAsUpIndicator(R.drawable.ic_arrow_back)
        //Fin Flecha TOOLBAR "ATRAS"------------------

        binding.btnFinalizarLogin.setOnClickListener {
            if (Validaciones.validarLoginPersona(
                    binding.tilCorreo,   binding.etCorreo,
                    binding.tilContrasena,binding.etContrasena
                )) {
                // TODOS los campos tienen texto → continúa el registro
            }
        }
    }

    //Flecha TOOLBAR "ATRAS"
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when(item.itemId) {
            R.id.home -> {
                //Vuelve a la anterior Activity
                onBackPressed()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
    //Fin Flecha TOOLBAR "ATRAS"

}