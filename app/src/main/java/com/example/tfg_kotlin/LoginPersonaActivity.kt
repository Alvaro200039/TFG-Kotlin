package com.example.tfg_kotlin

import android.R
import android.os.Bundle
import android.view.MenuItem
import android.widget.Toast
import android.content.Intent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.room.Room
import com.example.tfg_kotlin.BBDD.BBDD
import com.example.tfg_kotlin.BBDD.MIGRATION_1_2
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
            //----------Validaciones editText-----------
            if (Validaciones.validarLoginPersona(
                    binding.tilCorreo,   binding.etCorreo,
                    binding.tilContrasena, binding.etContrasena,
                    binding.tilNumEmpresa, binding.etNumEmpresa
                )) {
                // TODOS los campos tienen texto, continúa el registro
            }
            //----------------------------------------
            // ----LOGIN EMPLEADO, COMPRUEBA SI LOS DATOS DEL ENPLEADO COINCIDEN CON LOS REGISTRADOS EN LA BBDD
            val db = Room.databaseBuilder(
                applicationContext,
                BBDD::class.java,
                "reservas_db"
            ).addMigrations(MIGRATION_1_2).allowMainThreadQueries().build()

            val correo = binding.etCorreo.text.toString()
            val contrasena = binding.etContrasena.text.toString()
            val nifEmpresa = binding.etNumEmpresa.text.toString()

            val empleado = db.appDao().loginEmpleado(correo, contrasena)
            val empresa = db.appDao().existeEmpresaConNif(nifEmpresa)

            if (empleado != null && empresa != null) {
                // Login correcto: empleado existe y empresa también
                startActivity(Intent(this, MainActivity::class.java))
                finish()
                Toast.makeText(this, "Bienvenido  ${empleado.nombre} ", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Datos incorrectos o empresa no registrada", Toast.LENGTH_SHORT).show()
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