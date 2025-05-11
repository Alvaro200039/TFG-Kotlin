package com.example.tfg_kotlin

import android.os.Bundle
import android.view.MenuItem
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.tfg_kotlin.databinding.ActivityLoginEmpresaBinding
import com.example.tfg_kotlin.databinding.ActivityRegistroPersonaBinding

class RegistroPersonaActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRegistroPersonaBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

    //Flecha TOOLBAR "ATRAS"
        binding = ActivityRegistroPersonaBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Asocia tu Toolbar como ActionBar
        setSupportActionBar(binding.toolbar)

        //Muestra la flecha
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        // (opcional) cambiar el icono por defecto:
        // supportActionBar?.setHomeAsUpIndicator(R.drawable.ic_arrow_back)
        binding.btnFinalizarRegistro.setOnClickListener {
            if (Validaciones.validarRegistroPersona(
                    binding.tilNombre, binding.etNombre,
                    binding.tilApellidos, binding.etApellidos,
                    binding.tilCorreo, binding.etCorreo,
                    binding.tilContrasena, binding.etContrasena,
                    binding.tilRepContrasena, binding.etRepContrasena
                )) {
                // Registro válido → continúa
            }
        }
    }
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when(item.itemId) {
            android.R.id.home -> {
                //Vuelve a la anterior Activity
                onBackPressed()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
    //Fin Flecha TOOLBAR "ATRAS"

}