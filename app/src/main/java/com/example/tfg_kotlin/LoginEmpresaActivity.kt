package com.example.tfg_kotlin

import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import com.example.tfg_kotlin.Validaciones
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.room.Room
import com.example.tfg_kotlin.BBDD.BBDD
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
            //----------Validaciones editText-----------
            if (Validaciones.validarLoginEmpresa(
                    binding.tilCorreo, binding.etCorreo,
                    binding.tilContrasena, binding.etContrasena,
                    binding.tilNumEmpresa, binding.etNumEmpresa

                )
            ) {
                // Login válido, continúa
            }
            //----------------------------------------
            // ----LOGIN EMPRESA, COMPRUEBA SI LOS DATOS DE LA EMPRESA COINCIDEN CON LOS REGISTRADOS EN LA BBDD
            val db = Room.databaseBuilder(
                applicationContext,
                BBDD::class.java,
                "reservas_db"
            ).allowMainThreadQueries().build()

            val correo = binding.etCorreo.text.toString()
            val contrasena = binding.etContrasena.text.toString()
            val nifEmpresa = binding.etNumEmpresa.text.toString()

            val jefe = db.appDao().loginJefe(correo, contrasena, nifEmpresa)

            if (jefe != null){
                // Inicio de sesión correcto, la empresa existe
                startActivity(Intent(this, MainActivity::class.java))
                finish()
            }else {
                Toast.makeText(this, "Datos incorrectos", Toast.LENGTH_SHORT).show()
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




