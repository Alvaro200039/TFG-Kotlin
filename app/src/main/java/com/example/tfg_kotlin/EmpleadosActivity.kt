package com.example.tfg_kotlin

import android.graphics.Color
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import com.example.tfg_kotlin.data.model.Sala
import com.example.tfg_kotlin.data.model.Piso
import com.example.tfg_kotlin.data.model.Reserva
import com.example.tfg_kotlin.data.model.Sesion
import com.example.tfg_kotlin.data.model.TipoElemento
import com.example.tfg_kotlin.databinding.ActivityEmpleadosBinding
import com.example.tfg_kotlin.ui.viewmodel.EmpleadosViewModel
import com.example.tfg_kotlin.util.DateFormats
import com.google.android.material.snackbar.Snackbar
import androidx.activity.addCallback
import java.util.*


class EmpleadosActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "EmpleadosActivity"
    }

    private lateinit var binding: ActivityEmpleadosBinding
    private val viewModel: EmpleadosViewModel by viewModels()

    private var snackbarActivo: Snackbar? = null
    private var dialogDetallesActivo: androidx.appcompat.app.AlertDialog? = null

    private var editReservaId: String? = null
    private var editOriginalTime: String? = null
    private var editFocusedSalaId: String? = null
    private var cachedSelectedPiso: Piso? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEmpleadosBinding.inflate(layoutInflater)
        setContentView(binding.root)

        enableEdgeToEdge()

        editReservaId = intent.getStringExtra(BaseMenuActivity.EXTRA_EDIT_RESERVA_ID)
        editFocusedSalaId = intent.getStringExtra(BaseMenuActivity.EXTRA_EDIT_ROOM_ID)
        
        val eDate = intent.getStringExtra(BaseMenuActivity.EXTRA_EDIT_DATE)
        val eStart = intent.getStringExtra(BaseMenuActivity.EXTRA_EDIT_START_TIME)
        val eEnd = intent.getStringExtra(BaseMenuActivity.EXTRA_EDIT_END_TIME)
        if (eDate != null && eStart != null && eEnd != null) {
            editOriginalTime = "$eDate $eStart - $eEnd"
        }

        editReservaId?.let { id ->
            viewModel.setEditingReservaId(id)
            viewModel.setFocusedSalaId(editFocusedSalaId)
        }

        ViewCompat.setOnApplyWindowInsetsListener(binding.empleados) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        setupToolbar()
        setupObservers()
        setupListeners()

        onBackPressedDispatcher.addCallback(this) {
            handleSalida()
        }

        val editId = intent.getStringExtra(BaseMenuActivity.EXTRA_EDIT_RESERVA_ID)
        if (editId != null) {
            // Reconstruir reserva original para backup
            val originalReserva = Reserva(
                id = editId,
                nombreSala = intent.getStringExtra(BaseMenuActivity.EXTRA_EDIT_SALA_NOMBRE) ?: "",
                idSala = intent.getStringExtra(BaseMenuActivity.EXTRA_EDIT_ROOM_ID) ?: "",
                fechaHora = "${intent.getStringExtra(BaseMenuActivity.EXTRA_EDIT_DATE)} ${intent.getStringExtra(BaseMenuActivity.EXTRA_EDIT_START_TIME)} - ${intent.getStringExtra(BaseMenuActivity.EXTRA_EDIT_END_TIME)}",
                nombreUsuario = intent.getStringExtra(BaseMenuActivity.EXTRA_EDIT_USER_NOMBRE) ?: "",
                idUsuario = intent.getStringExtra(BaseMenuActivity.EXTRA_EDIT_USER_ID) ?: "",
                piso = intent.getStringExtra(BaseMenuActivity.EXTRA_EDIT_PISO_NAME) ?: "",
                tipo = intent.getStringExtra(BaseMenuActivity.EXTRA_EDIT_TIPO) ?: "SALA"
            )
            viewModel.startEditingSession(originalReserva)
        }

        viewModel.loadPisos()
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setHomeAsUpIndicator(R.drawable.ic_adagora_nav)
            setDisplayShowTitleEnabled(false)
        }
    }

    private fun setupObservers() {
        viewModel.empresa.observe(this) { empresa ->
            if (empresa != null) {
                binding.textEmpresa.text = empresa.nombre
                
                // 1. Determinar fecha primero
                val dateToUse = if (viewModel.fechaSeleccionada.value.isNullOrEmpty()) {
                    val editDate = intent.getStringExtra(BaseMenuActivity.EXTRA_EDIT_DATE)
                    if (editDate != null) {
                        viewModel.updateFecha(editDate)
                        editDate
                    } else {
                        val closureTime = viewModel.timeToFloat(empresa.cierre)
                        val nowCal = Calendar.getInstance()
                        val currentTime = nowCal.get(Calendar.HOUR_OF_DAY) + nowCal.get(Calendar.MINUTE)/60f
                        val tDate = if (currentTime >= closureTime) {
                            nowCal.add(Calendar.DAY_OF_YEAR, 1)
                            DateFormats.dayFormat.format(nowCal.time)
                        } else {
                            DateFormats.dayFormat.format(nowCal.time)
                        }
                        viewModel.updateFecha(tDate)
                        tDate
                    }
                } else {
                    viewModel.fechaSeleccionada.value!!
                }

                // 2. Configurar límites del slider
                val start = viewModel.timeToFloat(empresa.apertura)
                val end = viewModel.timeToFloat(empresa.cierre)
                currentMaxDuration = empresa.maxDuration.toFloat()
                currentStepSize = empresa.stepSize
                
                // Calcular próximo slot disponible basado en hora actual
                val now = Calendar.getInstance()
                val currentTime = now.get(Calendar.HOUR_OF_DAY) + now.get(Calendar.MINUTE)/60f
                val nextSlot = (Math.ceil(currentTime / currentStepSize.toDouble()) * currentStepSize).toFloat()
                
                // Evitar crash interno del RangeSlider al cambiar valores y límites:
                // 1. Ampliar límites temporales al máximo.
                binding.rangeSliderHoras.valueFrom = 0f 
                binding.rangeSliderHoras.valueTo = 24f
                binding.rangeSliderHoras.stepSize = currentStepSize
                try { binding.rangeSliderHoras.setMinSeparationValue(currentStepSize) } catch (_: Exception) {}
                
                // 2. Establecer valores seguros que estarán dentro de los límites finales.
                binding.rangeSliderHoras.values = listOf(start, end)
                
                // 3. Establecer los límites finales de apertura y cierre.
                binding.rangeSliderHoras.valueFrom = start
                binding.rangeSliderHoras.valueTo = end
                val eStart = intent.getStringExtra(BaseMenuActivity.EXTRA_EDIT_START_TIME)
                val eEnd = intent.getStringExtra(BaseMenuActivity.EXTRA_EDIT_END_TIME)
                
                if (eStart != null && eEnd != null) {
                    val sVal = viewModel.timeToFloat(eStart).coerceIn(start, end)
                    val fVal = viewModel.timeToFloat(eEnd).coerceIn(start, end)
                    if (sVal < fVal) {
                        binding.rangeSliderHoras.values = listOf(sVal, fVal)
                        viewModel.updateRange(sVal, fVal)
                    } else {
                        val defaultEnd = minOf(sVal + 1f, end)
                        binding.rangeSliderHoras.values = listOf(sVal, defaultEnd)
                        viewModel.updateRange(sVal, defaultEnd)
                    }
                } else {
                    val hoyStr = DateFormats.dayFormat.format(Date())
                    
                    val initialStart = if (dateToUse == hoyStr) maxOf(start, nextSlot) else start
                    val initialEnd = minOf(initialStart + currentMaxDuration, end)
                    
                    if (initialStart >= end) {
                        binding.rangeSliderHoras.values = listOf(start, minOf(start + currentMaxDuration, end))
                    } else {
                        binding.rangeSliderHoras.values = listOf(initialStart, initialEnd)
                        viewModel.updateRange(initialStart, initialEnd)
                    }
                }
                
                setupSliderLabels(start.toInt(), end.toInt())
            }
        }

        viewModel.pisos.observe(this) { pisos ->
            val adapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, pisos.map { it.nombre })
            binding.spinnerPisos.setAdapter(adapter)
            
            val editPisoName = intent.getStringExtra(BaseMenuActivity.EXTRA_EDIT_PISO_NAME)
            if (!editPisoName.isNullOrEmpty()) {
                pisos.find { it.nombre == editPisoName }?.let {
                    binding.spinnerPisos.setText(it.nombre, false)
                    cargarPiso(it)
                }
            } else if (pisos.isNotEmpty() && binding.spinnerPisos.text.isEmpty()) {
                binding.spinnerPisos.setText(pisos[0].nombre, false)
                cargarPiso(pisos[0])
            }
        }

        viewModel.salas.observe(this) { salas ->
            actualizarVistaSalas(salas)
            
            // Si venimos de edición, autoseleccionar sala y horario
            val editRoomId = intent.getStringExtra(BaseMenuActivity.EXTRA_EDIT_ROOM_ID)
            if (!editRoomId.isNullOrEmpty()) {
                val sStr = intent.getStringExtra(BaseMenuActivity.EXTRA_EDIT_START_TIME)
                val eStr = intent.getStringExtra(BaseMenuActivity.EXTRA_EDIT_END_TIME)
                if (sStr != null && eStr != null) {
                    val startF = viewModel.timeToFloat(sStr)
                    val endF = viewModel.timeToFloat(eStr)
                    
                    val boundsStart = binding.rangeSliderHoras.valueFrom
                    val boundsEnd = binding.rangeSliderHoras.valueTo
                    
                    val clampedStart = startF.coerceIn(boundsStart, boundsEnd)
                    val clampedEnd = endF.coerceIn(boundsStart, boundsEnd)
                    
                    if (clampedStart < clampedEnd) {
                        binding.rangeSliderHoras.values = listOf(clampedStart, clampedEnd)
                        viewModel.updateRange(clampedStart, clampedEnd)
                    }
                    
                    // Mostrar slider si no se ve
                    if (binding.cardSliderHorario.visibility == View.GONE) {
                        toggleSliderHorario()
                    }
                }
                // Limpiar intent para no re-ejecutar en rotaciones
                intent.removeExtra(BaseMenuActivity.EXTRA_EDIT_ROOM_ID)
            }
        }

        viewModel.reservas.observe(this) {
            actualizarColoresSalas()
        }

        viewModel.fechaSeleccionada.observe(this) { fecha ->
            binding.btnFecha.text = if (fecha.isEmpty()) getString(R.string.btn_seleccionar_fecha) else getString(R.string.label_fecha_con_valor, fecha)
            actualizarColoresSalas()

            // --- Ajuste de slider según fecha (fusión de observers duplicados) ---
            val hoyStr = DateFormats.dayFormat.format(Date())
            val empresa = viewModel.empresa.value ?: return@observe
            
            val currentVals = binding.rangeSliderHoras.values
            if (currentVals.size < 2) return@observe
            
            val opening = viewModel.timeToFloat(empresa.apertura)
            val closing = viewModel.timeToFloat(empresa.cierre)
            
            binding.rangeSliderHoras.valueFrom = 0f
            binding.rangeSliderHoras.valueTo = 24f
            binding.rangeSliderHoras.valueFrom = opening
            binding.rangeSliderHoras.valueTo = closing
            
            if (fecha == hoyStr) {
                val now = Calendar.getInstance()
                val currentTime = now.get(Calendar.HOUR_OF_DAY) + now.get(Calendar.MINUTE)/60f
                val nextSlot = (Math.ceil(currentTime / empresa.stepSize.toDouble()) * empresa.stepSize).toFloat()
                
                if (currentVals[0] < nextSlot) {
                    val duration = currentVals[1] - currentVals[0]
                    val nS = nextSlot.coerceAtMost(closing - empresa.stepSize)
                    val nE = (nS + duration).coerceAtMost(closing)
                    binding.rangeSliderHoras.values = listOf(nS, nE)
                    viewModel.updateRange(nS, nE)
                }
            }
        }

        viewModel.horaSeleccionada.observe(this) { hora ->
            binding.btnHora.text = when {
                hora.isEmpty() -> getString(R.string.btn_cambiar_hora)
                hora == EmpleadosViewModel.SLOT_PUESTO -> hora
                else -> getString(R.string.label_hora_con_valor, hora)
            }
            if (hora != EmpleadosViewModel.SLOT_PUESTO && hora.isNotEmpty()) {
                val labels = hora.split(" - ")
                if (labels.size == 2) {
                    binding.tvRangeLabel.text = "${formatTo12h(labels[0])} - ${formatTo12h(labels[1])}"
                } else {
                    binding.tvRangeLabel.text = hora
                }
            }
            actualizarColoresSalas()
        }

        viewModel.error.observe(this) { msg ->
            Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
        }

        viewModel.loading.observe(this) { loading ->
            binding.progressBar.visibility = if (loading) View.VISIBLE else View.GONE
        }

        viewModel.reservaStatus.observe(this) { success ->
            if (success) {
                dialogDetallesActivo?.dismiss()
                Toast.makeText(this, getString(R.string.msg_operacion_exito), Toast.LENGTH_SHORT).show()
                viewModel.clearReservaStatus()
                finish()
            }
        }

        viewModel.overlaps.observe(this) { overlaps ->
            binding.planoReservasView.setOverlaps(overlaps)
            actualizarColoresSalas()
        }
    }

    private var currentMaxDuration = 2f
    private var currentStepSize = 0.5f

    private fun setupListeners() {
        binding.spinnerPisos.setOnItemClickListener { _, _, position, _ ->
            viewModel.pisos.value?.getOrNull(position)?.let {
                cachedSelectedPiso = it
                cargarPiso(it)
            }
        }

        binding.btnReservas.setOnClickListener { mostrarDialogoFecha() }
        
        binding.btnFranja.setOnClickListener { toggleSliderHorario() }
        
        binding.btnHora.setOnClickListener { toggleSliderHorario() }

        binding.rangeSliderHoras.addOnSliderTouchListener(object : com.google.android.material.slider.RangeSlider.OnSliderTouchListener {
            override fun onStartTrackingTouch(slider: com.google.android.material.slider.RangeSlider) {
                if (slider.stepSize == 0f) {
                    var s = (Math.round(slider.values[0] / currentStepSize) * currentStepSize).toFloat()
                    var e = (Math.round(slider.values[1] / currentStepSize) * currentStepSize).toFloat()
                    
                    if (e <= s) {
                        e = s + currentStepSize
                        if (e > slider.valueTo) {
                            e = slider.valueTo
                            s = e - currentStepSize
                        }
                    }
                    
                    slider.values = listOf(s, e)
                    slider.stepSize = currentStepSize
                    try { slider.setMinSeparationValue(currentStepSize) } catch (_: Exception) {}
                }
            }
            override fun onStopTrackingTouch(slider: com.google.android.material.slider.RangeSlider) {}
        })

        binding.rangeSliderHoras.addOnChangeListener { slider, _, fromUser ->
            if (fromUser) {
                var s = slider.values[0]
                var e = slider.values[1]
                
                // Restringir a futuro si es hoy
                val hoyStr = DateFormats.dayFormat.format(Date())
                if (viewModel.fechaSeleccionada.value == hoyStr) {
                    val empresa = viewModel.empresa.value
                    if (empresa != null) {
                        val now = Calendar.getInstance()
                        val currentTime = now.get(Calendar.HOUR_OF_DAY) + now.get(Calendar.MINUTE)/60f
                        val nextSlot = (Math.ceil(currentTime / empresa.stepSize.toDouble()) * empresa.stepSize).toFloat()
                        
                        if (s < nextSlot) {
                            s = nextSlot
                            if (e <= s) e = s + empresa.stepSize
                        }
                        if (s != slider.values[0] || e != slider.values[1]) {
                            slider.values = listOf(s, e)
                        }
                    }
                }

                val duration = e - s
                
                val isInvalid = duration > currentMaxDuration
                
                val color = if (isInvalid) Color.RED else ContextCompat.getColor(this, R.color.aura_primary)
                
                slider.trackActiveTintList = android.content.res.ColorStateList.valueOf(color)
                slider.thumbTintList = android.content.res.ColorStateList.valueOf(color)
                slider.haloTintList = android.content.res.ColorStateList.valueOf(if (isInvalid) 0x1AEE0000 else 0x26888888)
                
                if (isInvalid) {
                    binding.tvLimitWarning.text = getString(R.string.msg_limite_reserva_horas, currentMaxDuration.toInt())
                    binding.tvLimitWarning.visibility = View.VISIBLE
                    binding.tvRangeLabel.setTextColor(Color.RED)
                } else {
                    binding.tvLimitWarning.visibility = View.GONE
                    binding.tvRangeLabel.setTextColor(ContextCompat.getColor(this, R.color.aura_on_surface))
                }

                val t1 = viewModel.formatTime(s)
                val t2 = viewModel.formatTime(e)
                binding.tvRangeLabel.text = "${formatTo12h(t1)} - ${formatTo12h(t2)}"
                
                viewModel.updateRange(s, e)
            }
        }

        binding.cardHoraInfo.setOnClickListener {
            mostrarDialogoEdicionManual()
        }
    }

    private fun mostrarDialogoEdicionManual() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_manual_time, null)
        val etStartH = dialogView.findViewById<EditText>(R.id.et_start_h)
        val etStartM = dialogView.findViewById<EditText>(R.id.et_start_m)
        val etEndH = dialogView.findViewById<EditText>(R.id.et_end_h)
        val etEndM = dialogView.findViewById<EditText>(R.id.et_end_m)
        val tvError = dialogView.findViewById<TextView>(R.id.tv_error_manual)

        val empresa = viewModel.empresa.value
        val opener = empresa?.apertura ?: "08:00"
        val closer = empresa?.cierre ?: "20:00"
        val maxDuration = empresa?.maxDuration ?: 2

        // Pre-rellenar con valores actuales
        val currentVals = binding.rangeSliderHoras.values
        if (currentVals.size < 2) {
            Toast.makeText(this, getString(R.string.msg_horario_no_disponible), Toast.LENGTH_SHORT).show()
            return
        }
        val sVal = currentVals[0]
        val eVal = currentVals[1]
        
        fun splitToTime(v: Float): Pair<String, String> {
            val h = v.toInt()
            val m = ((v - h) * 60).toInt()
            return "%02d".format(h) to "%02d".format(m)
        }

        val (sh, sm) = splitToTime(sVal)
        val (eh, em) = splitToTime(eVal)
        etStartH.setText(sh); etStartM.setText(sm); etEndH.setText(eh); etEndM.setText(em)

        fun validateAndShow() {
            try {
                val h1 = etStartH.text.toString().let { if(it.length == 1) "0$it" else it }.ifEmpty { "00" }.toFloat()
                val m1 = etStartM.text.toString().let { if(it.length == 1) "0$it" else it }.ifEmpty { "00" }.toFloat()
                val h2 = etEndH.text.toString().let { if(it.length == 1) "0$it" else it }.ifEmpty { "00" }.toFloat()
                val m2 = etEndM.text.toString().let { if(it.length == 1) "0$it" else it }.ifEmpty { "00" }.toFloat()
                
                val f1 = h1 + m1/60f
                val f2 = h2 + m2/60f
                
                val limitOpen = viewModel.timeToFloat(opener)
                val limitClose = viewModel.timeToFloat(closer)
                
                val hoyStr = DateFormats.dayFormat.format(Date())
                val isToday = viewModel.fechaSeleccionada.value == hoyStr
                val now = Calendar.getInstance()
                val currentTime = now.get(Calendar.HOUR_OF_DAY) + now.get(Calendar.MINUTE)/60f

                when {
                    f1 < limitOpen || f2 > limitClose -> {
                        tvError.text = getString(R.string.err_fuera_horario_oficina, opener, closer)
                        tvError.visibility = View.VISIBLE
                    }
                    isToday && f1 < currentTime -> {
                        tvError.text = getString(R.string.err_hora_inicio_pasada)
                        tvError.visibility = View.VISIBLE
                    }
                    f2 - f1 > maxDuration -> {
                        tvError.text = getString(R.string.err_excede_duracion_maxima, maxDuration)
                        tvError.visibility = View.VISIBLE
                    }
                    f2 <= f1 -> {
                        tvError.text = getString(R.string.err_hora_fin_posterior)
                        tvError.visibility = View.VISIBLE
                    }
                    f2 - f1 < 0.25f - 0.01f -> {
                        tvError.text = getString(R.string.err_duracion_minima_15)
                        tvError.visibility = View.VISIBLE
                    }
                    else -> {
                        tvError.visibility = View.GONE
                    }
                }
            } catch (_: Exception) {
                tvError.visibility = View.GONE
            }
        }

        // Auto-focus y formateo natural
        val watcher = object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                if (s?.length == 2) {
                    when {
                        etStartH.isFocused -> etStartM.requestFocus()
                        etStartM.isFocused -> etEndH.requestFocus()
                        etEndH.isFocused -> etEndM.requestFocus()
                    }
                }
                validateAndShow()
            }
            override fun afterTextChanged(s: android.text.Editable?) {}
        }

        listOf(etStartH, etStartM, etEndH, etEndM).forEach { it.addTextChangedListener(watcher) }

        val dialog = MaterialAlertDialogBuilder(this)
            .setView(dialogView)
            .setPositiveButton(R.string.btn_aplicar, null)
            .setNeutralButton(R.string.btn_reset, null)
            .setNegativeButton(R.string.btn_cancelar, null)
            .create()

        dialog.show()

        // Override Reset button to clear everything without dismissing
        dialog.getButton(android.app.AlertDialog.BUTTON_NEUTRAL).setOnClickListener {
            etStartH.text.clear()
            etStartM.text.clear()
            etEndH.text.clear()
            etEndM.text.clear()
            tvError.visibility = View.GONE
            etStartH.requestFocus()
        }

        // Override positive button...
        dialog.getButton(android.app.AlertDialog.BUTTON_POSITIVE).setOnClickListener {
            validateAndShow()
            if (tvError.visibility == View.GONE) {
                try {
                    val sH = etStartH.text.toString().let { if(it.length == 1) "0$it" else it }.ifEmpty { "00" }
                    val sM = etStartM.text.toString().let { if(it.length == 1) "0$it" else it }.ifEmpty { "00" }
                    val eH = etEndH.text.toString().let { if(it.length == 1) "0$it" else it }.ifEmpty { "00" }
                    val eM = etEndM.text.toString().let { if(it.length == 1) "0$it" else it }.ifEmpty { "00" }
                    
                    val f1 = sH.toFloat() + sM.toFloat()/60f
                    val f2 = eH.toFloat() + eM.toFloat()/60f

                    // Deshabilitar snapping temporalmente para tolerar inserción manual fina (1 minuto)
                    binding.rangeSliderHoras.stepSize = 0f
                    try { binding.rangeSliderHoras.setMinSeparationValue(0f) } catch (_: Exception) {}
                    
                    binding.rangeSliderHoras.values = listOf(f1, f2)
                    viewModel.updateRange(f1, f2)
                    dialog.dismiss()
                } catch (_: Exception) {}
            }
        }
    }

    private fun toggleSliderHorario() {
        try {
            val panel = binding.cardSliderHorario
            val topCard = binding.cardHoraInfo
            
            if (panel.visibility == View.GONE) {
                val currentVals = binding.rangeSliderHoras.values
                if (currentVals.size < 2) return
                
                if (viewModel.horaSeleccionada.value == EmpleadosViewModel.SLOT_PUESTO || viewModel.horaSeleccionada.value.isNullOrEmpty()) {
                    viewModel.updateRange(currentVals[0], currentVals[1])
                }
                
                panel.visibility = View.VISIBLE
                topCard.visibility = View.VISIBLE
                
                panel.post {
                    panel.translationY = panel.height.toFloat() + 100f
                    panel.animate()
                        .translationY(0f)
                        .setDuration(400)
                        .setInterpolator(android.view.animation.DecelerateInterpolator())
                        .start()
                }
                
                topCard.scaleX = 0f
                topCard.scaleY = 0f
                topCard.alpha = 0f
                topCard.animate()
                    .scaleX(1.0f)
                    .scaleY(1.0f)
                    .alpha(1.0f)
                    .setDuration(500)
                    .setInterpolator(android.view.animation.OvershootInterpolator(1.4f))
                    .start()
            } else {
                panel.animate()
                    .translationY(panel.height.toFloat() + 100f)
                    .setDuration(300)
                    .setInterpolator(android.view.animation.AccelerateInterpolator())
                    .withEndAction { panel.visibility = View.GONE }
                    .start()
                    
                topCard.animate()
                    .scaleX(0f)
                    .scaleY(0f)
                    .alpha(0f)
                    .setDuration(300)
                    .withEndAction { topCard.visibility = View.GONE }
                    .start()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error in toggleSliderHorario", e)
            // Si hay un error, al menos asegurar visibilidad para no bloquear al usuario
            binding.cardSliderHorario.visibility = View.VISIBLE
            binding.cardHoraInfo.visibility = View.VISIBLE
        }
    }

    private fun cargarPiso(piso: Piso) {
        viewModel.loadSalas(piso.id ?: "")
        val url = piso.imagenUrl
        if (!url.isNullOrEmpty()) {
            cargarImagenFondo(url)
        } else {
            binding.planoReservasView.background = null
        }
    }

    private fun getSelectedPiso(): Piso? {
        if (cachedSelectedPiso != null) return cachedSelectedPiso
        val currentText = binding.spinnerPisos.text.toString()
        return viewModel.pisos.value?.find { it.nombre == currentText }
    }

    private fun actualizarVistaSalas(salas: List<Sala>) {
        val piso = getSelectedPiso()
        
        binding.planoReservasView.setElementos(salas)
        piso?.let {
            binding.planoReservasView.setMuros(it.muros)
        }
        
        binding.planoReservasView.setOnElementClickedListener { sala ->
            if (editReservaId != null) {
                if (sala.id == editFocusedSalaId) {
                    val fecha = viewModel.fechaSeleccionada.value ?: ""
                    val hora = viewModel.horaSeleccionada.value ?: ""
                    val currentSelection = "$fecha $hora"
                    
                    val uStart = viewModel.startTime.value ?: 0f
                    val uEnd = viewModel.endTime.value ?: 0f
                    
                    val hasOverlap = viewModel.reservas.value?.filter { it.idSala == sala.id && it.id != editReservaId }?.any { res ->
                        val (rs, re) = viewModel.parseReservaRange(res.fechaHora) ?: return@any false
                        maxOf(uStart, rs) < minOf(uEnd, re)
                    } ?: false

                    when {
                        currentSelection == editOriginalTime -> {
                            // Opción de confirmar lo mismo o ELIMINAR (User request)
                            MaterialAlertDialogBuilder(this)
                                .setTitle(R.string.title_reserva_original)
                                .setMessage(R.string.msg_reserva_original_opciones)
                                .setPositiveButton(R.string.btn_mantener) { _, _ -> finish() }
                                .setNegativeButton(R.string.btn_eliminar_reserva) { _, _ ->
                                    viewModel.cancelarReservaDirecto(editReservaId!!)
                                    finish()
                                }
                                .show()
                        }
                        hasOverlap -> {
                            Toast.makeText(this, getString(R.string.msg_ya_tienes_sala_reservada), Toast.LENGTH_SHORT).show()
                        }
                        else -> {
                            // VERDE: Confirmar cambios
                            viewModel.reservarSala(sala, getSelectedPiso()?.nombre ?: "")
                        }
                    }
                }
                return@setOnElementClickedListener
            }

            if (viewModel.fechaSeleccionada.value.isNullOrEmpty()) {
                mostrarSnackbarFecha()
            } else if (sala.tipo == TipoElemento.SALA.valor) {
                val horaActual = viewModel.horaSeleccionada.value ?: ""
                val noHayHoraValida = horaActual.isNullOrEmpty() || horaActual == EmpleadosViewModel.SLOT_PUESTO
                
                if (noHayHoraValida) {
                    val currentVals = binding.rangeSliderHoras.values
                    if (currentVals.size >= 2) {
                        // Si estaba en gris (sin hora válida para sala), abrir slider pero no mostrar diálogo de reserva todavía
                        viewModel.updateRange(currentVals[0], currentVals[1])
                        if (binding.cardSliderHorario.visibility == View.GONE) {
                            toggleSliderHorario()
                        }
                    } else {
                        // Si no hay horario de empresa cargado todavía
                        Toast.makeText(this, getString(R.string.msg_cargando_horario), Toast.LENGTH_SHORT).show()
                        if (binding.cardSliderHorario.visibility == View.GONE) {
                            toggleSliderHorario()
                        }
                    }
                } else {
                    // Si ya tiene una hora seleccionada (no está en gris), mostrar detalles
                    if (binding.cardSliderHorario.visibility == View.GONE) {
                        toggleSliderHorario()
                    }
                    mostrarDialogoDetallesSala(sala)
                }
            } else if (sala.tipo == TipoElemento.PUESTO.valor) {
                if (binding.cardSliderHorario.visibility == View.VISIBLE) {
                    toggleSliderHorario()
                }
                if (viewModel.horaSeleccionada.value != EmpleadosViewModel.SLOT_PUESTO) {
                    viewModel.updateHora(EmpleadosViewModel.SLOT_PUESTO)
                }
                mostrarDialogoDetallesSala(sala)
            }
        }
        
        actualizarColoresSalas()
        binding.planoReservasView.fitToScreen()
    }

    private fun actualizarColoresSalas() {
        val reservas = viewModel.reservas.value ?: emptyList()
        val uid = Sesion.datos?.usuario?.uid ?: ""
        val fecha = viewModel.fechaSeleccionada.value ?: ""
        val hora = viewModel.horaSeleccionada.value ?: ""
        val salas = viewModel.salas.value ?: emptyList()
        
        // Optimización: agrupar reservas por sala una sola vez
        val reservasPorSala = reservas.groupBy { it.idSala }
        
        binding.planoReservasView.setFocusedSala(editFocusedSalaId)

        salas.forEach { sala ->
            val isPuesto = sala.tipo == TipoElemento.PUESTO.valor
            val salaId = sala.id ?: return@forEach
            
            val colorId = when {
                fecha.isEmpty() -> Color.LTGRAY
                editReservaId != null && salaId == editFocusedSalaId -> {
                    // MODO EDICIÓN: Colores específicos (Naranja, Verde, Azul)
                    val currentSelection = "$fecha $hora"
                    val uStart = viewModel.startTime.value ?: 0f
                    val uEnd = viewModel.endTime.value ?: 0f
                    val rSala = reservasPorSala[salaId] ?: emptyList()
                    val hasOverlap = rSala.filter { it.id != editReservaId }.any { res ->
                        val rRange = viewModel.parseReservaRange(res.fechaHora) ?: return@any false
                        maxOf(uStart, rRange.first) < minOf(uEnd, rRange.second)
                    }
                    
                    when {
                        currentSelection == editOriginalTime -> ContextCompat.getColor(this, android.R.color.holo_orange_dark)
                        hasOverlap -> Color.parseColor("#2196F3") // AZUL (User request)
                        else -> ContextCompat.getColor(this, android.R.color.holo_green_light)
                    }
                }
                isPuesto -> {
                    val slotBusqueda = "$fecha ${EmpleadosViewModel.SLOT_PUESTO}"
                    val rSala = reservasPorSala[salaId] ?: emptyList()
                    val reserva = rSala.find { it.fechaHora == slotBusqueda }
                    when {
                        reserva == null -> ContextCompat.getColor(this, android.R.color.holo_green_light)
                        reserva.idUsuario == uid -> ContextCompat.getColor(this, android.R.color.holo_orange_light)
                        else -> ContextCompat.getColor(this, android.R.color.holo_red_light)
                    }
                }
                else -> { // SALA
                    if (hora.isEmpty() || hora == EmpleadosViewModel.SLOT_PUESTO) {
                        Color.LTGRAY
                    } else {
                        val uStart = viewModel.startTime.value ?: 0f
                        val uEnd = viewModel.endTime.value ?: 24f
                        val rSala = reservasPorSala[salaId] ?: emptyList()
                        
                        val overlapping = rSala.filter { res ->
                            val rRange = viewModel.parseReservaRange(res.fechaHora) ?: return@filter false
                            maxOf(uStart, rRange.first) < minOf(uEnd, rRange.second)
                        }
                        
                        when {
                            overlapping.isEmpty() -> ContextCompat.getColor(this, android.R.color.holo_green_light)
                            overlapping.any { it.idUsuario == uid } -> ContextCompat.getColor(this, android.R.color.holo_orange_light)
                            else -> ContextCompat.getColor(this, android.R.color.holo_red_light)
                        }
                    }
                }
            }
            binding.planoReservasView.setStatusColor(salaId, colorId)
        }
    }

    private fun mostrarSnackbarFecha() {
        snackbarActivo?.dismiss()
        snackbarActivo = Snackbar.make(binding.planoReservasView, getString(R.string.msg_selecciona_fecha_primero), Snackbar.LENGTH_LONG)
            .setAction(getString(R.string.btn_seleccionar_fecha)) { mostrarDialogoFecha() }
        snackbarActivo?.show()
    }

    private fun mostrarDialogoFecha() {
        val empresa = viewModel.empresa.value ?: return
        val dialogueView = layoutInflater.inflate(R.layout.dialog_calendar, findViewById(android.R.id.content), false)
        val rvCalendar = dialogueView.findViewById<RecyclerView>(R.id.rvCalendar)
        val tvMonthName = dialogueView.findViewById<TextView>(R.id.tvMonthName)
        val btnPrev = dialogueView.findViewById<View>(R.id.btnPrevMonth)
        val btnNext = dialogueView.findViewById<View>(R.id.btnNextMonth)

        val calendarInstance = Calendar.getInstance()
        val currentBlocked = empresa.diasBloqueados
        val currentWeekly = empresa.diasApertura

        val dialog = MaterialAlertDialogBuilder(this)
            .setView(dialogueView)
            .setNegativeButton(R.string.btn_cancelar, null)
            .create()

        fun refreshCalendar() {
            val monthYearSDF = java.text.SimpleDateFormat("MMMM yyyy", Locale.getDefault())
            tvMonthName.text = monthYearSDF.format(calendarInstance.time).replaceFirstChar { it.uppercase() }

            val dates = mutableListOf<Calendar?>()
            val tempCal = calendarInstance.clone() as Calendar
            tempCal.set(Calendar.DAY_OF_MONTH, 1)

            val firstDayOfWeek = tempCal.get(Calendar.DAY_OF_WEEK)
            val blanks = if (firstDayOfWeek == Calendar.SUNDAY) 6 else firstDayOfWeek - 2
            for (i in 0 until blanks) dates.add(null)

            val maxDays = tempCal.getActualMaximum(Calendar.DAY_OF_MONTH)
            for (i in 1..maxDays) {
                val dayCal = tempCal.clone() as Calendar
                dayCal.set(Calendar.DAY_OF_MONTH, i)
                dates.add(dayCal)
            }

            val today = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
            }
            val oneMonthAhead = Calendar.getInstance().apply {
                add(Calendar.MONTH, 1); set(Calendar.HOUR_OF_DAY, 23); set(Calendar.MINUTE, 59); set(Calendar.SECOND, 59); set(Calendar.MILLISECOND, 999)
            }

            rvCalendar.adapter = object : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
                override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
                    val view = LayoutInflater.from(parent.context).inflate(R.layout.item_calendar_day, parent, false)
                    return object : RecyclerView.ViewHolder(view) {}
                }

                override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
                    val date = dates[position]
                    val tvDay = holder.itemView.findViewById<TextView>(R.id.tvDay)
                    val viewHighlight = holder.itemView.findViewById<View>(R.id.viewHighlight)

                    if (date == null) {
                        tvDay.text = ""
                        viewHighlight.visibility = View.GONE
                    } else {
                        val dayNum = date.get(Calendar.DAY_OF_MONTH)
                        tvDay.text = dayNum.toString()
                        
                        val firstDayOfWeekDate = date.get(Calendar.DAY_OF_WEEK)
                        val dayIndex = if (firstDayOfWeekDate == Calendar.SUNDAY) 6 else firstDayOfWeekDate - 2
                        
                        val isNonLaborable = !currentWeekly.contains(dayIndex)
                        val dateKey = DateFormats.dayFormat.format(date.time)
                        val isFestivo = currentBlocked.contains(dateKey)
                        
                        val isPast = date.before(today)
                        val isTooFuture = date.after(oneMonthAhead)
                        
                        val isTodayClosed = dateKey == DateFormats.dayFormat.format(Date()) && 
                                          (Calendar.getInstance().get(Calendar.HOUR_OF_DAY) + Calendar.getInstance().get(Calendar.MINUTE)/60f) >= viewModel.timeToFloat(empresa.cierre)

                        val isDarkMode = (resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK) == 
                                        android.content.res.Configuration.UI_MODE_NIGHT_YES
                        val circleTextColor = if (isDarkMode) Color.WHITE else Color.BLACK

                        // Mostrar puntos siempre para dar contexto (festivos/no laborables)
                        when {
                            isNonLaborable -> {
                                viewHighlight.visibility = View.VISIBLE
                                viewHighlight.setBackgroundResource(R.drawable.aura_calendar_dot_blue)
                            }
                            isFestivo -> {
                                viewHighlight.visibility = View.VISIBLE
                                viewHighlight.setBackgroundResource(R.drawable.aura_calendar_dot)
                            }
                            else -> { viewHighlight.visibility = View.GONE }
                        }

                        when {
                            isPast || isTooFuture || isTodayClosed -> {
                                holder.itemView.setOnClickListener(null)
                                tvDay.setTextColor(Color.LTGRAY)
                                tvDay.alpha = 0.5f
                            }
                            isNonLaborable -> {
                                holder.itemView.setOnClickListener(null)
                                tvDay.setTextColor(circleTextColor)
                                tvDay.alpha = 1.0f
                            }
                            isFestivo -> {
                                holder.itemView.setOnClickListener(null)
                                tvDay.setTextColor(circleTextColor)
                                tvDay.alpha = 1.0f
                            }
                            else -> {
                                tvDay.alpha = 1.0f
                                // Restaurar color por defecto del tema
                                val typedValue = android.util.TypedValue()
                                theme.resolveAttribute(com.google.android.material.R.attr.colorOnSurface, typedValue, true)
                                tvDay.setTextColor(typedValue.data)
                                
                                holder.itemView.setOnClickListener {
                                    viewModel.updateFecha(dateKey)
                                    dialog.dismiss()
                                }
                            }
                        }
                    }
                }
                override fun getItemCount() = dates.size
            }
        }

        btnPrev.setOnClickListener { calendarInstance.add(Calendar.MONTH, -1); refreshCalendar() }
        btnNext.setOnClickListener { calendarInstance.add(Calendar.MONTH, 1); refreshCalendar() }

        refreshCalendar()
        dialog.show()
    }

    private fun mostrarDialogoHoras() {
        val franjas = viewModel.franjas.value ?: emptyList()
        if (franjas.isEmpty()) {
            Toast.makeText(this, getString(R.string.msg_no_franjas_disponibles), Toast.LENGTH_SHORT).show()
            return
        }
        
        val fechaSel = viewModel.fechaSeleccionada.value ?: ""
        
        val isHoy = try {
            val partes = fechaSel.split("/")
            if (partes.size == 3) {
                val d = partes[0].replace(Regex("[^0-9]"), "").toInt()
                val m = partes[1].replace(Regex("[^0-9]"), "").toInt()
                val y = partes[2].replace(Regex("[^0-9]"), "").toInt()
                val now = Calendar.getInstance()
                d == now.get(Calendar.DAY_OF_MONTH) && m == (now.get(Calendar.MONTH) + 1) && y == now.get(Calendar.YEAR)
            } else false
        } catch (_: Exception) { false }
        
        val franjasFiltradas = if (isHoy) {
            val now = Calendar.getInstance()
            val horaActual = now.get(Calendar.HOUR_OF_DAY)
            val minutoActual = now.get(Calendar.MINUTE)
            franjas.filter { franja ->
                try {
                    val partes = franja.split("-")
                    val inicio = partes[0].trim()
                    val horasMinutos = inicio.split(":")
                    if (horasMinutos.size >= 2) {
                        val hInicio = horasMinutos[0].replace(Regex("[^0-9]"), "").toInt()
                        val mInicio = horasMinutos[1].replace(Regex("[^0-9]"), "").toInt()
                        hInicio > horaActual || (hInicio == horaActual && mInicio > minutoActual)
                    } else {
                        true
                    }
                } catch (e: Exception) {
                    true
                }
            }
        } else {
            franjas
        }

        // Asegurar que "Día completo" esté siempre disponible para volver a puestos
        val finalSlots = if (franjasFiltradas.contains(EmpleadosViewModel.SLOT_PUESTO)) {
            franjasFiltradas
        } else {
            listOf(EmpleadosViewModel.SLOT_PUESTO) + franjasFiltradas
        }

        if (finalSlots.isEmpty()) {
            Toast.makeText(this, getString(R.string.msg_no_quedan_huecos), Toast.LENGTH_SHORT).show()
            return
        }
        
        MaterialAlertDialogBuilder(this)
            .setTitle(getString(R.string.title_selecciona_franja_para, fechaSel))
            .setItems(finalSlots.toTypedArray()) { _, which ->
                viewModel.updateHora(finalSlots[which])
            }
            .show()
    }

    private fun cargarImagenFondo(url: String) {
        Glide.with(this).load(url).centerCrop().into(object : CustomTarget<Drawable>() {
            override fun onResourceReady(resource: Drawable, transition: Transition<in Drawable>?) {
                binding.planoReservasView.background = resource
            }
            override fun onLoadCleared(placeholder: Drawable?) {}
        })
    }

    private fun mostrarDialogoDetallesSala(sala: Sala) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_detalles_sala, findViewById(android.R.id.content), false)
        val tvNombre = dialogView.findViewById<TextView>(R.id.tvNombreSalaDetalle)
        val tvEstado = dialogView.findViewById<TextView>(R.id.tvEstadoSalaDetalle)
        val tvEquipamiento = dialogView.findViewById<TextView>(R.id.tvEquipamientoDetalle)
        val tvPiso = dialogView.findViewById<TextView>(R.id.tvValorPiso)
        val tvTamano = dialogView.findViewById<TextView>(R.id.tvValorTamano)
        val tvFecha = dialogView.findViewById<TextView>(R.id.tvValorFecha)
        val tvHora = dialogView.findViewById<TextView>(R.id.tvValorHora)
        val btnAccion = dialogView.findViewById<Button>(R.id.btnAccionReserva)

        tvNombre.text = sala.nombre
        tvEquipamiento.text = if (sala.extras.isEmpty()) getString(R.string.label_ninguno) else sala.extras.joinToString(", ")
        tvPiso.text = viewModel.pisos.value?.find { it.id == sala.idPiso }?.nombre ?: ""
        tvTamano.text = sala.tamano
        tvFecha.text = viewModel.fechaSeleccionada.value
        tvHora.text = viewModel.horaSeleccionada.value
        
        val tvTituloHora = dialogView.findViewById<TextView>(R.id.tvTituloHora)
        if (viewModel.horaSeleccionada.value == EmpleadosViewModel.SLOT_PUESTO) {
            tvTituloHora.visibility = View.GONE
        } else {
            tvTituloHora.visibility = View.VISIBLE
        }
        
        val isPuesto = sala.tipo == TipoElemento.PUESTO.valor
        val fecha = viewModel.fechaSeleccionada.value ?: ""
        val hora = viewModel.horaSeleccionada.value ?: ""
        
        // Cargar reservas solapadas para mostrar en el diálogo
        val uStart = viewModel.startTime.value ?: 0f
        val uEnd = viewModel.endTime.value ?: 24f
        val reservasSala = viewModel.reservas.value?.filter { it.idSala == sala.id } ?: emptyList()
        val solapadas = if (isPuesto) {
            reservasSala.filter { it.fechaHora == "$fecha ${EmpleadosViewModel.SLOT_PUESTO}" }
        } else {
            reservasSala.filter { res ->
                val rRange = viewModel.parseReservaRange(res.fechaHora) ?: return@filter false
                maxOf(uStart, rRange.first) < minOf(uEnd, rRange.second)
            }
        }
        
        val uid = Sesion.datos?.usuario?.uid ?: ""
        val miReserva = solapadas.find { it.idUsuario == uid }

        if (solapadas.isEmpty() && !isPuesto && hora.isNotEmpty()) {
            tvEstado.text = getString(R.string.label_libre)
            tvEstado.setTextColor(Color.GREEN)
            btnAccion.visibility = View.VISIBLE
            btnAccion.text = getString(R.string.btn_reservar_mayuscula)
            
            // Validar límite de duración para SALA
            val duration = uEnd - uStart
            if (duration > currentMaxDuration) {
                btnAccion.isEnabled = false
                btnAccion.alpha = 0.5f
                tvEstado.text = getString(R.string.msg_limite_duracion_excedido)
                tvEstado.setTextColor(Color.RED)
            } else {
                btnAccion.isEnabled = true
                btnAccion.alpha = 1.0f
                btnAccion.setOnClickListener {
                    viewModel.reservarSala(sala, viewModel.pisos.value?.find { p -> p.id == sala.idPiso }?.nombre ?: "")
                }
            }
        } else if (solapadas.isNotEmpty()) {
            if (isPuesto) {
                if (miReserva != null) {
                    tvEstado.text = miReserva.nombreUsuario
                    tvEstado.setTextColor(Color.parseColor("#FFA500")) // Naranja
                } else {
                    // "Puesto ocupado" y debajo el nombre
                    tvEstado.text = getString(R.string.label_puesto_ocupado) + "\n" + solapadas[0].nombreUsuario
                    tvEstado.setTextColor(Color.RED)
                }
            } else {
                val exactMatch = solapadas.find {
                    val rRange = viewModel.parseReservaRange(it.fechaHora)
                    rRange != null && Math.abs(rRange.first - uStart) < 0.01f && Math.abs(rRange.second - uEnd) < 0.01f
                }

                if (exactMatch != null && solapadas.size == 1) {
                    if (exactMatch.idUsuario == uid) {
                        tvEstado.text = exactMatch.nombreUsuario + " (${viewModel.formatTime(uStart)} - ${viewModel.formatTime(uEnd)})"
                        tvEstado.setTextColor(Color.parseColor("#FFA500")) // Naranja
                    } else {
                        tvEstado.text = getString(R.string.label_sala_ocupada) + "\n" + exactMatch.nombreUsuario
                        tvEstado.setTextColor(Color.RED)
                    }
                } else {
                    val nombres = solapadas.joinToString("\n") {
                        val rRange = viewModel.parseReservaRange(it.fechaHora)
                        val horario = if (rRange != null) " (${viewModel.formatTime(rRange.first)} - ${viewModel.formatTime(rRange.second)})" else ""
                        "${it.nombreUsuario}$horario"
                    }
                    tvEstado.text = getString(R.string.msg_solapamientos_detectados) + "\n" + nombres
                    tvEstado.setTextColor(Color.parseColor("#FFA500"))
                }
            }

            if (miReserva != null) {
                btnAccion.visibility = View.VISIBLE
                btnAccion.text = getString(R.string.btn_cancelar_mi_reserva)
                btnAccion.setOnClickListener {
                    viewModel.cancelReserva(miReserva.id ?: "")
                }
            } else {
                btnAccion.visibility = View.GONE
            }
        } else {
            // Caso sin franja seleccionada para sala o puesto libre
            if (isPuesto) {
                tvEstado.text = getString(R.string.label_libre)
                tvEstado.setTextColor(Color.GREEN)
                btnAccion.visibility = View.VISIBLE
                btnAccion.text = getString(R.string.btn_reservar_mayuscula)
                btnAccion.setOnClickListener {
                    viewModel.reservarSala(sala, viewModel.pisos.value?.find { p -> p.id == sala.idPiso }?.nombre ?: "")
                }
            } else {
                tvEstado.text = getString(R.string.msg_selecciona_horario_ver)
                tvEstado.setTextColor(Color.GRAY)
                btnAccion.visibility = View.GONE
            }
        }

        dialogDetallesActivo?.dismiss()
        dialogDetallesActivo = MaterialAlertDialogBuilder(this)
            .setView(dialogView)
            .create()
        
        dialogDetallesActivo?.show()
    }

    private fun formatTo12h(time24: String): String {
        return try {
            val parts = time24.split(":")
            val h = parts[0].toInt()
            val m = parts[1].toInt()
            val suffix = if (h >= 12) "PM" else "AM"
            val h12 = when {
                h == 0 -> 12
                h > 12 -> h - 12
                else -> h
            }
            "%d:%02d %s".format(h12, m, suffix)
        } catch (_: Exception) {
            time24
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            handleSalida()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    private fun handleSalida() {
        if (viewModel.editingReservaId.value != null && viewModel.reservaStatus.value != true) {
            binding.progressBar.visibility = View.VISIBLE
            viewModel.restoreBackup { 
                finish() 
            }
        } else {
            finish()
        }
    }

    private fun setupSliderLabels(startInclusive: Int, endInclusive: Int) {
        binding.llSliderLabels.removeAllViews()
        for (h in startInclusive..endInclusive) {
            val tv = TextView(this).apply {
                layoutParams = android.widget.LinearLayout.LayoutParams(0, android.widget.LinearLayout.LayoutParams.WRAP_CONTENT, 1.0f)
                val label = when {
                    h == 0 -> "12"
                    h > 12 -> (h - 12).toString()
                    else -> h.toString()
                }
                text = label
                textSize = 9f
                gravity = android.view.Gravity.CENTER
                alpha = 0.6f
                val typedValue = android.util.TypedValue()
                theme.resolveAttribute(com.google.android.material.R.attr.colorOutline, typedValue, true)
                setTextColor(typedValue.data)
            }
            binding.llSliderLabels.addView(tv)
        }
    }
}