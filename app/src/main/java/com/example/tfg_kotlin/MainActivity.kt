package com.example.tfg_kotlin

import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.tfg_kotlin.databinding.ActivityMainBinding
import com.example.tfg_kotlin.databinding.BottomsheetTipoUsuarioBinding
import com.example.tfg_kotlin.databinding.BottomsheetTipoUsuarioRegistroBinding
import com.google.android.material.bottomsheet.BottomSheetDialog

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)

        // 2. Establecer el layout raíz
        setContentView(binding.root)

        // 3. Acciones de los botones
        binding.btnIniciarSesion.setOnClickListener {
            val bottomSheetDialog = BottomSheetDialog(this)
            val bottomSheetBinding = BottomsheetTipoUsuarioBinding.inflate(layoutInflater)
            bottomSheetDialog.setContentView(bottomSheetBinding.root)

            bottomSheetBinding.btnPersona.setOnClickListener {
                bottomSheetDialog.dismiss()
                startActivity(Intent(this, LoginPersonaActivity::class.java))
            }

            bottomSheetBinding.btnEmpresa.setOnClickListener {
                bottomSheetDialog.dismiss()
                startActivity(Intent(this, LoginEmpresaActivity::class.java))
            }

            bottomSheetDialog.show()
        }

        binding.btnRegistrarse.setOnClickListener {
            val bottomSheetDialog = BottomSheetDialog(this)
            val bottomSheetBinding = BottomsheetTipoUsuarioRegistroBinding.inflate(layoutInflater)
            bottomSheetDialog.setContentView(bottomSheetBinding.root)

            bottomSheetBinding.btnPersona.setOnClickListener {
                bottomSheetDialog.dismiss()
                startActivity(Intent(this, RegistroPersonaActivity::class.java))
            }

            bottomSheetBinding.btnEmpresa.setOnClickListener {
                bottomSheetDialog.dismiss()
                startActivity(Intent(this, RegistroEmpresaActivity::class.java))
            }

            bottomSheetDialog.show()
        }


    }
}