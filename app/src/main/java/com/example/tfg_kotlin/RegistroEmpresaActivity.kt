package com.example.tfg_kotlin

import android.os.Bundle
import android.view.MenuItem
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.room.Room
import com.example.tfg_kotlin.BBDD.BBDD
import com.example.tfg_kotlin.BBDD.Empleados
import com.example.tfg_kotlin.databinding.ActivityRegistroEmpresaBinding
import android.widget.Toast
import com.example.tfg_kotlin.databinding.ActivityRegistroPersonaBinding


class RegistroEmpresaActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRegistroEmpresaBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)


    //Flecha TOOLBAR "ATRAS"
        binding = ActivityRegistroEmpresaBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Asocia tu Toolbar como ActionBar
        setSupportActionBar(binding.toolbar)

        //Muestra la flecha
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        // (opcional) cambiar el icono por defecto:
        // supportActionBar?.setHomeAsUpIndicator(R.drawable.ic_arrow_back)

        binding.btnFinalizarRegistro.setOnClickListener {
            //----------Validaciones editText-----------

            val esValido = Validaciones.validarRegistroEmpresa(
                    binding.tilNombreEmpresa, binding.etNombreEmpresa,
                    binding.tilCorreo, binding.etCorreo,
                    binding.tilContrasena, binding.etContrasena,
                    binding.tilRepContrasena, binding.etRepContrasena,
                    binding.tilNumEmpresa, binding.etNumEmpresa
                )
            //----------------------------------------
            // --------GUARDA LOS DATOS DE LA EMPRESA EN LA BBDD----------
           if (esValido){
               val db = Room.databaseBuilder(
                   applicationContext,
                   BBDD::class.java,
                   "reservas_db"
               ).allowMainThreadQueries().build()

               //Comprobar si el correo ya esta registrado
               val correo = binding.etCorreo.text.toString()
               val usuarioExistente = db.appDao().buscarEmpleadoPorCorreo(correo)

               if (usuarioExistente != null) {
                   binding.tilCorreo.error = "Este correo ya está registrado"
                   binding.etCorreo.requestFocus()
                   return@setOnClickListener
               }//Fin Comprobación

               val jefe = Empleados(
                   nombre = binding.etNombreEmpresa.text.toString(),
                   correo = binding.etCorreo.text.toString(),
                   contrasena = binding.etContrasena.text.toString(),
                   nif = binding.etNumEmpresa.text.toString(),
                   esJefe = true // acceso corporativo
               )

               db.appDao().insertarEmpleado(jefe)
               Toast.makeText(this, "Empresa registrada correctamente", Toast.LENGTH_SHORT).show()
           }
            //------------------------------------------------------------------
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