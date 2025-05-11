package com.example.tfg_kotlin

import android.os.Bundle
import android.view.MenuItem
import androidx.activity.enableEdgeToEdge
import com.example.tfg_kotlin.Validaciones
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.tfg_kotlin.databinding.ActivityLoginEmpresaBinding

class LoginEmpresaActivity : AppCompatActivity() {
    private lateinit var binding: ActivityLoginEmpresaBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        //Flecha TOOLBAR "ATRAS"
        binding = ActivityLoginEmpresaBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Asocia tu Toolbar como ActionBar
        setSupportActionBar(binding.toolbar)

        //Muestra la flecha
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        // (opcional) cambiar el icono por defecto:
        // supportActionBar?.setHomeAsUpIndicator(R.drawable.ic_arrow_back)
        //Fin Flecha TOOLBAR "ATRAS"

        binding.btnFinalizarLogin.setOnClickListener {
            if (Validaciones.validarLoginEmpresa(
                    binding.tilCorreo, binding.etCorreo,
                    binding.tilContrasena, binding.etContrasena,
                    binding.tilNumEmpresa, binding.etNumEmpresa

                )
            ) {
                // Login válido → continúa
            }

        }
    }//Fin onCreate


    //Flecha TOOLBAR "ATRAS"
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                //Vuelve a la anterior Activity
                onBackPressed()
                true
            }

            else -> super.onOptionsItemSelected(item)
        }
    }//Fin Flecha TOOLBAR "ATRAS"
}




