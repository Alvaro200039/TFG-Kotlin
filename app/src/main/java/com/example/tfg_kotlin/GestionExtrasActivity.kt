package com.example.tfg_kotlin

import android.os.Bundle
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.example.tfg_kotlin.databinding.ActivityGestionExtrasBinding
import com.example.tfg_kotlin.ui.viewmodel.MenuViewModel
import com.google.android.material.chip.Chip

class GestionExtrasActivity : AppCompatActivity() {

    private lateinit var binding: ActivityGestionExtrasBinding
    private val viewModel: MenuViewModel by viewModels()
    
    private val extrasSalas = mutableListOf<String>()
    private val extrasPuestos = mutableListOf<String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityGestionExtrasBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupToolbar()
        setupListeners()
        setupObservers()

        viewModel.loadEmpresaSettings()
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setHomeAsUpIndicator(R.drawable.ic_adagora_nav)
        }
        binding.toolbar.setNavigationOnClickListener { finish() }
    }

    private fun setupObservers() {
        viewModel.empresa.observe(this) { empresa ->
            empresa?.let {
                extrasSalas.clear()
                extrasSalas.addAll(it.extrasSalas)
                
                extrasPuestos.clear()
                extrasPuestos.addAll(it.extrasPuestos)
                
                updateUI()
            }
        }

        viewModel.loading.observe(this) { isLoading ->
            binding.btnSaveExtras.isEnabled = !isLoading
        }

        viewModel.error.observe(this) { msg ->
            if (msg.isNotEmpty()) Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupListeners() {
        binding.btnAddExtraSala.setOnClickListener {
            val extra = binding.etExtraSala.text.toString().trim()
            if (extra.isNotEmpty() && !extrasSalas.contains(extra)) {
                extrasSalas.add(extra)
                updateUI()
                binding.etExtraSala.text?.clear()
            }
        }

        binding.btnAddExtraPuesto.setOnClickListener {
            val extra = binding.etExtraPuesto.text.toString().trim()
            if (extra.isNotEmpty() && !extrasPuestos.contains(extra)) {
                extrasPuestos.add(extra)
                updateUI()
                binding.etExtraPuesto.text?.clear()
            }
        }

        binding.btnSaveExtras.setOnClickListener {
            val empresa = viewModel.empresa.value ?: return@setOnClickListener
            
            viewModel.saveScheduleSettings(
                apertura = empresa.apertura,
                cierre = empresa.cierre,
                diasApertura = empresa.diasApertura,
                diasBloqueados = empresa.diasBloqueados,
                extrasSalas = extrasSalas.toList(),
                extrasPuestos = extrasPuestos.toList()
            )
            Toast.makeText(this, "Amenidades actualizadas", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    private fun updateUI() {
        // Rooms
        binding.chipGroupSalas.removeAllViews()
        extrasSalas.forEach { extra ->
            val chip = Chip(this).apply {
                text = extra
                isCloseIconVisible = true
                setOnCloseIconClickListener {
                    extrasSalas.remove(extra)
                    updateUI()
                }
            }
            binding.chipGroupSalas.addView(chip)
        }

        // Workstations
        binding.chipGroupPuestos.removeAllViews()
        extrasPuestos.forEach { extra ->
            val chip = Chip(this).apply {
                text = extra
                isCloseIconVisible = true
                setOnCloseIconClickListener {
                    extrasPuestos.remove(extra)
                    updateUI()
                }
            }
            binding.chipGroupPuestos.addView(chip)
        }
    }
}
