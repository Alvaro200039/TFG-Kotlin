package com.example.tfg_kotlin

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import androidx.room.Room
import com.example.tfg_kotlin.BBDD_Global.Database.GlobalDB
import com.example.tfg_kotlin.BBDD_Maestra.Database.MasterDB
import kotlinx.coroutines.launch

class LoginActivity : AppCompatActivity() {
    private lateinit var etCorreo: EditText
    private lateinit var etContrasena: EditText
    private lateinit var btnLogin: Button
    private lateinit var btnRegistro: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        etContrasena = findViewById(R.id.etContrasena)
        etCorreo = findViewById(R.id.etCorreo)
        btnLogin = findViewById(R.id.btnLogin)
        btnRegistro = findViewById(R.id.btnRegistro)


        btnLogin.setOnClickListener {
            iniciarSesion()
        }
        btnRegistro.setOnClickListener{
            val registro = Intent(this, RegistroEmpleado::class.java)
            startActivity(registro)

        }
    }
    private fun iniciarSesion(){
        val correo = etCorreo.text.toString()
        val contrasena = etContrasena.text.toString()
        if (correo.isEmpty() || contrasena.isEmpty()) {
            Toast.makeText(this, "Completa todos los campos", Toast.LENGTH_SHORT).show()
            return
        }

        if (!correo.contains("@")) {
            Toast.makeText(this, "Correo inválido", Toast.LENGTH_SHORT).show()
            return
        }

        val dominio = correo.substringAfter("@")
        val dbMaestra = Room.databaseBuilder(
            applicationContext,
            MasterDB::class.java,
            "db_maestra.db"
        ).build()

        lifecycleScope.launch {
            val empresa = dbMaestra.empresaDao().buscarPorDominio("@$dominio")

            if (empresa == null) {
                runOnUiThread {
                    Toast.makeText(this@LoginActivity, "Dominio no registrado", Toast.LENGTH_SHORT).show()
                }
                return@launch
            }

            val nombreEmpresa = empresa.nombre.lowercase().replace(" ", "_")
            val dbNombre = "db_$nombreEmpresa.db"

            val dbEmpresa = Room.databaseBuilder(
                applicationContext,
                GlobalDB::class.java,
                dbNombre
            ).build()

            val empleado = dbEmpresa.usuarioDao().obtenerPorCorreo(correo)

            runOnUiThread {
                if (empleado != null && empleado.contrasena == contrasena) {
                    if (empleado.esJefe) {
                        Toast.makeText(this@LoginActivity, "Bienvenido Jefe", Toast.LENGTH_SHORT).show()
                        val intent = Intent(this@LoginActivity, activity_menu_creador::class.java)
                        startActivity(intent)
                    } else {
                        Toast.makeText(this@LoginActivity, "Bienvenido Empleado", Toast.LENGTH_SHORT).show()
                        val intent = Intent(this@LoginActivity, activity_menu_empleado::class.java)
                        startActivity(intent)
                    }
                } else {
                    Toast.makeText(this@LoginActivity, "Credenciales incorrectas", Toast.LENGTH_SHORT).show()
                    // Aquí podrías redirigir al registro si quisieras, por ejemplo

                    // startActivity(registro)
                }
            }
        }
    }
}