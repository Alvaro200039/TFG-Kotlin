package com.example.tfg_kotlin

import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.view.ViewCompat
import androidx.activity.viewModels
import com.example.tfg_kotlin.databinding.ActivityRegistroEmpresaBinding
import com.example.tfg_kotlin.ui.viewmodel.RegistroEmpresaViewModel

class RegistroEmpresaActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRegistroEmpresaBinding
    private val viewModel: RegistroEmpresaViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegistroEmpresaBinding.inflate(layoutInflater)
        setContentView(binding.root)

        enableEdgeToEdge()
        ViewCompat.setOnApplyWindowInsetsListener(binding.registroempresaLayout) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        setupToolbar()
        setupObservers()
        setupListeners()
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            title = getString(R.string.title_registro_empresas)
            setHomeAsUpIndicator(R.drawable.ic_adagora)
        }
    }

    private fun setupObservers() {
        viewModel.registroState.observe(this) { state ->
            when (state) {
                is RegistroEmpresaViewModel.RegistroEmpresaState.Loading -> {
                    binding.btnRegistrarEmpresa.isEnabled = false
                }
                is RegistroEmpresaViewModel.RegistroEmpresaState.Success -> {
                    binding.btnRegistrarEmpresa.isEnabled = true
                    Toast.makeText(this, getString(R.string.msg_empresa_registrada), Toast.LENGTH_SHORT).show()
                    limpiarCampos()
                }
                is RegistroEmpresaViewModel.RegistroEmpresaState.Error -> {
                    binding.btnRegistrarEmpresa.isEnabled = true
                    handleError(state.field, state.message)
                }
            }
        }
    }

    private fun setupListeners() {
        binding.btnRegistrarEmpresa.setOnClickListener { registrarEmpresa() }
    }

    private fun registrarEmpresa() {
        val nombre = binding.editNombreEmpresa.text.toString().trim()
        val dominio = binding.editDominio.text.toString().trim()
        val cif = binding.editCif.text.toString().trim().uppercase()

        if (nombre.isEmpty() || dominio.isEmpty() || cif.isEmpty()) {
            Toast.makeText(this, getString(R.string.err_completar_campos), Toast.LENGTH_SHORT).show()
            return
        }

        if (!dominio.startsWith("@") || !dominio.contains(".")) {
            Toast.makeText(this, getString(R.string.err_dominio_invalido_ej), Toast.LENGTH_SHORT).show()
            return
        }

        viewModel.registrarEmpresa(nombre, dominio, cif)
    }

    private fun handleError(field: String, message: String) {
        when (field) {
            "nombre" -> {
                Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
                binding.editNombreEmpresa.requestFocus()
            }
            "dominio" -> {
                Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
                binding.editDominio.requestFocus()
            }
            "cif" -> {
                Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
                binding.editCif.requestFocus()
            }
            else -> Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
        }
    }

    private fun limpiarCampos() {
        binding.editCif.text?.clear()
        binding.editNombreEmpresa.text?.clear()
        binding.editDominio.text?.clear()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                onBackPressedDispatcher.onBackPressed()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
}


    // Funcionalidad de la toolbar para ir a la pantalla anterior
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                onBackPressed()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
}
