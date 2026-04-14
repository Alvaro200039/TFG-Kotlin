package com.example.tfg_kotlin

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.ArrayAdapter
import android.widget.CheckBox
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.example.tfg_kotlin.data.model.Sesion
import com.example.tfg_kotlin.data.model.Sala
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isGone
import com.example.tfg_kotlin.databinding.ActivityCreacionBinding
import com.example.tfg_kotlin.ui.viewmodel.CreacionViewModel

class CreacionActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCreacionBinding
    private val viewModel: CreacionViewModel by viewModels()

    private var imagen: ByteArray? = null
    private var activeTool: com.example.tfg_kotlin.ui.view.PlanoEditorView.EditorMode = com.example.tfg_kotlin.ui.view.PlanoEditorView.EditorMode.DRAW_WALL

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCreacionBinding.inflate(layoutInflater)
        setContentView(binding.root)

        enableEdgeToEdge()
        ViewCompat.setOnApplyWindowInsetsListener(binding.creacion) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        setupToolbar()
        setupObservers()
        setupListeners()

        // Modo inicial
        actualizarModoUI(com.example.tfg_kotlin.ui.view.PlanoEditorView.EditorMode.SELECT)

        val intentAction = intent.getStringExtra("ACTION")
        val intentPisoId = intent.getStringExtra("PISO_ID")
        viewModel.loadPisos(intentAction, intentPisoId)
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.myToolbar)
        supportActionBar?.apply {
            setDisplayShowTitleEnabled(false)
            setDisplayHomeAsUpEnabled(true)
            setHomeAsUpIndicator(R.drawable.ic_adagora_nav)
        }
    }

    private fun setupObservers() {
        viewModel.pisos.observe(this) { pisos ->
            val listaDisplay = pisos.map { it.nombre }.toMutableList()
            listaDisplay.add(getString(R.string.opt_nuevo_piso))
            
            val adapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, listaDisplay)
            binding.spinnerPisos.setAdapter(adapter)
            
            // Si el piso actual ya existe, seleccionarlo (manteniendo el trigger táctil sin texto)
            val current = viewModel.pisoActual.value
            if (current?.id != null) {
                val index = pisos.indexOfFirst { it.id == current.id }
                if (index != -1) {
                    binding.spinnerPisos.setText("", false) // Solo queremos ver la flecha
                }
            } else {
                binding.spinnerPisos.setText("", false)
            }
            
            // Configurar el desplegable para que ocupe todo el ancho anclándose a la barra superior
            binding.spinnerPisos.setDropDownAnchor(R.id.my_toolbar)
            binding.spinnerPisos.dropDownWidth = android.view.ViewGroup.LayoutParams.MATCH_PARENT
        }

        viewModel.pisoActual.observe(this) { piso ->
            // Actualizar el nombre en el editor
            if (binding.etNombrePiso.text.toString() != (piso?.nombre ?: "")) {
                binding.etNombrePiso.setText(piso?.nombre ?: "")
            }
            
            piso?.let {
                binding.planoEditor.setMuros(it.muros)
                
                // Actualizar etiqueta del botón de eliminación/reset
                if (it.id != null) {
                    binding.tvLabelEliminar.text = "Borrar Piso"
                } else {
                    binding.tvLabelEliminar.text = "Reset"
                }
            }
        }

        viewModel.salas.observe(this) { salas ->
            binding.planoEditor.setElementos(salas)
        }

        binding.planoEditor.setOnElementSelectedListener { sala, _, _ ->
            if (sala != null) {
                showFloatingOptions(sala)
            } else {
                binding.fabElementOptions.isGone = true
                binding.fabCornerOptions.isGone = true
                binding.llCornerActions.isGone = true
            }
        }

        binding.planoEditor.setOnElementMovedOrResizedListener { sala ->
            binding.planoEditor.syncToViewModel(viewModel)
            if (sala.idPiso.isNotEmpty()) viewModel.updateSala(sala, sala) 
        }

        binding.planoEditor.setOnElementScreenPositionChangedListener { sala, x, y ->
            val screenX = x + binding.planoEditor.left
            val screenY = y + binding.planoEditor.top
            
            binding.fabElementOptions.visibility = View.VISIBLE
            binding.fabElementOptions.translationX = screenX - binding.fabElementOptions.width / 2f
            binding.fabElementOptions.translationY = screenY - binding.fabElementOptions.height / 2f
            
            binding.fabCornerOptions.isGone = true
        }

        viewModel.error.observe(this) { msg ->
            Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
        }


        viewModel.saveStatus.observe(this) { success ->
            if (success) {
                Toast.makeText(this, getString(R.string.msg_distribucion_guardada), Toast.LENGTH_SHORT).show()
                imagen = null
            }
        }

        viewModel.confirmacionRequerida.observe(this) { requiresConfirmation ->
            if (requiresConfirmation) {
                MaterialAlertDialogBuilder(this)
                    .setTitle("¡Aviso Importante!")
                    .setMessage("Existen empleados con reservas activas en estas salas.\n\nSi continúas, las reservas de las salas eliminadas se marcarán como inválidas en el perfil del usuario, y las de las salas modificadas se actualizarán automáticamente.\n\n¿Estás completamente seguro de aplicar los cambios?")
                    .setPositiveButton("Sí, aplicar cambios") { _, _ -> viewModel.confirmarGuardado(imagen) }
                    .setNegativeButton(getString(R.string.btn_cancelar), null)
                    .show()
            }
        }

        viewModel.confirmacionRequeridaEliminar.observe(this) { piso ->
            piso?.let {
                MaterialAlertDialogBuilder(this)
                    .setTitle("Eliminar Piso con Reservas")
                    .setMessage("El piso '${it.nombre}' tiene reservas activas.\n\nSi lo eliminas, los usuarios que reservaron verán sus reservas como 'Sitio inexistente' pero se mantendrá el registro de que tenían una reserva.\n\n¿Deseas eliminar el piso de todos modos?")
                    .setPositiveButton("Sí, eliminar") { _, _ -> viewModel.eliminarPiso(it) }
                    .setNegativeButton(getString(R.string.btn_cancelar)) { _, _ -> 
                        // Limpiar el estado de confirmación en el ViewModel si es necesario
                    }
                    .show()
            }
        }
    }

    private fun setupListeners() {
        // Seleccionar Toggle
        binding.llSelect.setOnClickListener { binding.btnSelectToggle.performClick() }
        binding.btnSelectToggle.setOnClickListener {
            if (binding.btnSelectToggle.isChecked) {
                actualizarModoUI(com.example.tfg_kotlin.ui.view.PlanoEditorView.EditorMode.SELECT)
            } else {
                actualizarModoUI(activeTool)
            }
        }

        // Borrar Toggle
        binding.llBorrar.setOnClickListener { binding.btnBorrarToggle.performClick() }
        binding.btnBorrarToggle.setOnClickListener {
            if (binding.btnBorrarToggle.isChecked) {
                actualizarModoUI(com.example.tfg_kotlin.ui.view.PlanoEditorView.EditorMode.DELETE)
            } else {
                actualizarModoUI(com.example.tfg_kotlin.ui.view.PlanoEditorView.EditorMode.SELECT)
            }
        }
        
        // Herramientas Toggle (Panel integrado)
        binding.llTools.setOnClickListener { binding.btnTools.performClick() }
        binding.btnTools.setOnClickListener { toggleHerramientas() }

        // Listeners del panel integrado
        binding.panelHerramientas.root.findViewById<View>(R.id.btn_tool_muros).setOnClickListener {
            activeTool = com.example.tfg_kotlin.ui.view.PlanoEditorView.EditorMode.DRAW_WALL
            actualizarModoUI(activeTool)
            toggleHerramientas()
        }
        binding.panelHerramientas.root.findViewById<View>(R.id.btn_tool_sala).setOnClickListener {
            activeTool = com.example.tfg_kotlin.ui.view.PlanoEditorView.EditorMode.ADD_SALA
            actualizarModoUI(activeTool)
            toggleHerramientas()
        }
        binding.panelHerramientas.root.findViewById<View>(R.id.btn_tool_puesto).setOnClickListener {
            activeTool = com.example.tfg_kotlin.ui.view.PlanoEditorView.EditorMode.ADD_PUESTO
            actualizarModoUI(activeTool)
            toggleHerramientas()
        }

        binding.llUndo.setOnClickListener { binding.btnUndoNew.performClick() }
        binding.btnUndoNew.setOnClickListener { binding.planoEditor.undo() }
        
        binding.btnGuardarNew.setOnClickListener { 
            if (binding.planoEditor.hasOpenSalas()) {
                MaterialAlertDialogBuilder(this@CreacionActivity)
                    .setTitle(getString(R.string.title_salas_abiertas))
                    .setMessage(getString(R.string.msg_salas_abiertas_aviso))
                    .setPositiveButton(R.string.btn_aceptar, null)
                    .create()
                    .show()
            } else {
                binding.planoEditor.syncToViewModel(viewModel)
                viewModel.verificarReservasYGuardar(imagen) 
            }
        }

        binding.btnEliminarPisoActual.setOnClickListener { manejarEliminacionOReset() }

        binding.etNombrePiso.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: android.text.Editable?) {
                viewModel.pisoActual.value?.let { it.nombre = s.toString() }
            }
        })
        
        binding.spinnerPisos.setOnItemClickListener { _, _, pos, _ ->
            val pisos = viewModel.pisos.value ?: emptyList()
            if (pos < pisos.size) {
                val seleccionado = pisos[pos]
                if (seleccionado.id != viewModel.pisoActual.value?.id) {
                    viewModel.setPisoActual(seleccionado)
                }
            } else if (pos == pisos.size) {
                val current = viewModel.pisoActual.value
                if (current?.id != null) {
                    val count = pisos.size + 1
                    viewModel.crearNuevoPiso("Piso nº $count")
                }
            }
        }
    }

    private fun toggleHerramientas() {
        val panel = binding.panelHerramientas.root
        if (panel.isGone) {
            panel.visibility = View.VISIBLE
            panel.post {
                panel.translationY = panel.height.toFloat()
                panel.animate()
                    .translationY(0f)
                    .setDuration(300)
                    .setInterpolator(android.view.animation.DecelerateInterpolator())
                    .start()
            }
        } else {
            panel.animate()
                .translationY(panel.height.toFloat())
                .setDuration(300)
                .setInterpolator(android.view.animation.AccelerateInterpolator())
                .withEndAction { panel.isGone = true }
                .start()
        }
    }

    private fun actualizarModoUI(modo: com.example.tfg_kotlin.ui.view.PlanoEditorView.EditorMode) {
        binding.planoEditor.setMode(modo)
        
        // Reset visual state of all toggles
        binding.btnSelectToggle.isChecked = (modo == com.example.tfg_kotlin.ui.view.PlanoEditorView.EditorMode.SELECT)
        binding.btnBorrarToggle.isChecked = (modo == com.example.tfg_kotlin.ui.view.PlanoEditorView.EditorMode.DELETE)
        
        // El botón de herramientas está "chequeado" si el modo es una de las herramientas internas
        val esHerramienta = modo == com.example.tfg_kotlin.ui.view.PlanoEditorView.EditorMode.DRAW_WALL ||
                           modo == com.example.tfg_kotlin.ui.view.PlanoEditorView.EditorMode.ADD_SALA ||
                           modo == com.example.tfg_kotlin.ui.view.PlanoEditorView.EditorMode.ADD_PUESTO
        binding.btnTools.isChecked = esHerramienta
        
        updateButtonStyle(binding.btnSelectToggle, binding.tvSelect)
        updateButtonStyle(binding.btnBorrarToggle, binding.tvBorrar)
        updateButtonStyle(binding.btnTools, binding.tvTools)
        
        if (esHerramienta) {
            activeTool = modo
            // Actualizar Icono y Texto del botón de herramientas para reflejar la selección actual
            when (modo) {
                com.example.tfg_kotlin.ui.view.PlanoEditorView.EditorMode.DRAW_WALL -> {
                    binding.btnTools.setIconResource(R.drawable.ic_wall)
                    binding.tvTools.text = getString(R.string.label_muros)
                }
                com.example.tfg_kotlin.ui.view.PlanoEditorView.EditorMode.ADD_SALA -> {
                    binding.btnTools.setIconResource(R.drawable.ic_meeting_room)
                    binding.tvTools.text = getString(R.string.label_sala)
                }
                com.example.tfg_kotlin.ui.view.PlanoEditorView.EditorMode.ADD_PUESTO -> {
                    binding.btnTools.setIconResource(R.drawable.ic_puesto)
                    binding.tvTools.text = getString(R.string.label_puesto)
                }
                else -> {}
            }
        } else {
            // Resetear al icono y texto original de "Herramientas"
            binding.btnTools.setIconResource(R.drawable.ic_add)
            binding.tvTools.text = getString(R.string.label_herramientas)
        }
    }
    
    private fun updateButtonStyle(button: com.google.android.material.button.MaterialButton, label: TextView) {
        if (button.isChecked) {
            button.setBackgroundColor(getColor(R.color.aura_primary_container))
            button.iconTint = android.content.res.ColorStateList.valueOf(getColor(R.color.aura_on_primary_container))
            label.setTextColor(getColor(R.color.aura_primary)) 
            label.setTypeface(null, android.graphics.Typeface.BOLD)
        } else {
            button.setBackgroundColor(android.graphics.Color.TRANSPARENT)
            button.iconTint = android.content.res.ColorStateList.valueOf(getColor(R.color.aura_on_surface))
            label.setTextColor(getColor(R.color.aura_on_surface))
            label.setTypeface(null, android.graphics.Typeface.NORMAL)
        }
    }

    private fun manejarEliminacionOReset() {
        val current = viewModel.pisoActual.value
        if (current?.id != null) {
            // Piso existente -> Verificar reservas antes de borrar
            viewModel.verificarReservasYEliminarPiso(current)
        } else {
            // Piso nuevo -> Resetear al punto inicial
            val count = (viewModel.pisos.value?.size ?: 0) + 1
            viewModel.crearNuevoPiso("Piso nº $count")
            imagen = null
            Toast.makeText(this, "Creación reseteada", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_drag, menu)
        return true
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

    private fun showFloatingOptions(sala: Sala) {
        binding.fabElementOptions.apply {
            visibility = View.VISIBLE
            // El posicionamiento ahora se delega totalmente al listener de cambio de posición
            // para evitar el salto desde el punto de toque hasta la esquina real.
            setOnClickListener { 
                showEditElementDialog(sala)
            }
        }
    }

    private fun showEditElementDialog(sala: Sala) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_editar_sala, findViewById(android.R.id.content), false)
        val etNombre = dialogView.findViewById<EditText>(R.id.etNombreSala).apply { setText(sala.nombre) }
        val spinnerTamano = dialogView.findViewById<Spinner>(R.id.spinnerTamanoSala)
        val containerExtras = dialogView.findViewById<LinearLayout>(R.id.containerExtras)

        // Dynamic Extras based on type
        val checkBoxList = mutableListOf<CheckBox>()
        val sesion = Sesion.datos
        val configExtras = if (sala.tipo == "SALA") {
            sesion?.empresa?.extrasSalas ?: listOf("WiFi", "Proyector", "Pizarra")
        } else {
            sesion?.empresa?.extrasPuestos ?: listOf("Monitor Dual", "Teclado", "Ratón")
        }

        configExtras.forEach { extra ->
            val cb = CheckBox(this).apply {
                text = extra
                isChecked = sala.extras.contains(extra)
            }
            containerExtras.addView(cb)
            checkBoxList.add(cb)
        }

        val tamanos = arrayOf("Pequeña", "Mediana", "Grande")
        spinnerTamano.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, tamanos)
        spinnerTamano.setSelection(tamanos.indexOf(sala.tamano).coerceAtLeast(0))

        MaterialAlertDialogBuilder(this)
            .setTitle(getString(R.string.title_editar_detalles_sala))
            .setView(dialogView)
            .setPositiveButton(getString(R.string.btn_guardar)) { _, _ ->
                val finalExtras = checkBoxList.filter { it.isChecked }.map { it.text.toString() }
                
                val newSala = sala.copy(
                    nombre = etNombre.text.toString(),
                    tamano = spinnerTamano.selectedItem.toString(),
                    extras = finalExtras
                )
                viewModel.updateSala(sala, newSala)
            }
            .setNegativeButton(getString(R.string.btn_cancelar), null)
            .show()
    }
}
