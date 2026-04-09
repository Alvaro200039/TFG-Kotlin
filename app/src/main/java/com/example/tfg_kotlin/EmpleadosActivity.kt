package com.example.tfg_kotlin

import android.graphics.Color
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Button
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
import com.example.tfg_kotlin.data.model.Sesion
import com.example.tfg_kotlin.databinding.ActivityEmpleadosBinding
import com.example.tfg_kotlin.ui.viewmodel.EmpleadosViewModel
import com.google.android.material.snackbar.Snackbar
import java.util.*

class EmpleadosActivity : AppCompatActivity() {

    private lateinit var binding: ActivityEmpleadosBinding
    private val viewModel: EmpleadosViewModel by viewModels()

    private var snackbarActivo: Snackbar? = null
    private var dialogDetallesActivo: androidx.appcompat.app.AlertDialog? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEmpleadosBinding.inflate(layoutInflater)
        setContentView(binding.root)

        enableEdgeToEdge()
        ViewCompat.setOnApplyWindowInsetsListener(binding.empleados) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        setupToolbar()
        setupObservers()
        setupListeners()

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
                if (viewModel.fechaSeleccionada.value.isNullOrEmpty()) {
                    mostrarDialogoFecha()
                }
            }
        }

        viewModel.pisos.observe(this) { pisos ->
            val adapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, pisos.map { it.nombre })
            binding.spinnerPisos.setAdapter(adapter)
            
            // Si hay pisos y no hay nada seleccionado aún, cargar el primero
            if (pisos.isNotEmpty() && binding.spinnerPisos.text.isEmpty()) {
                binding.spinnerPisos.setText(pisos[0].nombre, false)
                cargarPiso(pisos[0])
            }
        }

        viewModel.salas.observe(this) { salas ->
            actualizarVistaSalas(salas)
        }

        viewModel.reservas.observe(this) {
            actualizarColoresSalas()
        }

        viewModel.fechaSeleccionada.observe(this) { fecha ->
            binding.btnFecha.text = if (fecha.isEmpty()) getString(R.string.btn_seleccionar_fecha) else getString(R.string.label_fecha_con_valor, fecha)
            actualizarColoresSalas()
        }

        viewModel.horaSeleccionada.observe(this) { hora ->
            binding.btnHora.text = when {
                hora.isEmpty() -> getString(R.string.btn_cambiar_hora)
                hora == EmpleadosViewModel.SLOT_PUESTO -> hora
                else -> getString(R.string.label_hora_con_valor, hora)
            }
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
            }
        }
    }

    private fun setupListeners() {
        binding.spinnerPisos.setOnItemClickListener { _, _, position, _ ->
            viewModel.pisos.value?.getOrNull(position)?.let { cargarPiso(it) }
        }

        binding.btnReservas.setOnClickListener { mostrarDialogoFecha() }
        binding.btnFranja.setOnClickListener { mostrarDialogoHoras() }
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
            if (viewModel.fechaSeleccionada.value.isNullOrEmpty()) {
                mostrarSnackbarFecha()
            } else if (sala.tipo == "SALA" && (viewModel.horaSeleccionada.value.isNullOrEmpty() || viewModel.horaSeleccionada.value == EmpleadosViewModel.SLOT_PUESTO)) {
                mostrarDialogoHoras()
            } else if (sala.tipo == "PUESTO") {
                if (viewModel.horaSeleccionada.value != EmpleadosViewModel.SLOT_PUESTO) {
                    viewModel.updateHora(EmpleadosViewModel.SLOT_PUESTO)
                }
                mostrarDialogoDetallesSala(sala)
            } else {
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
        
        salas.forEach { sala ->
            val isPuesto = sala.tipo == "PUESTO"
            val slotBusqueda = if (isPuesto) "$fecha ${EmpleadosViewModel.SLOT_PUESTO}" else "$fecha $hora"
            val reserva = reservas.find { it.idSala == sala.id && it.fechaHora == slotBusqueda }
            
            val colorId = when {
                fecha.isEmpty() -> Color.LTGRAY
                !isPuesto && (hora.isEmpty() || hora == EmpleadosViewModel.SLOT_PUESTO) -> Color.LTGRAY
                reserva == null -> ContextCompat.getColor(this, android.R.color.holo_green_light)
                reserva.idUsuario == uid -> ContextCompat.getColor(this, android.R.color.holo_orange_light)
                else -> ContextCompat.getColor(this, android.R.color.holo_red_light)
            }
            binding.planoReservasView.setStatusColor(sala.id ?: "", colorId)
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
                        val dateKey = java.text.SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(date.time)
                        val isFestivo = currentBlocked.contains(dateKey)

                        val isDarkMode = (resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK) == 
                                        android.content.res.Configuration.UI_MODE_NIGHT_YES
                        val circleTextColor = if (isDarkMode) Color.WHITE else Color.BLACK

                        when {
                            isNonLaborable -> {
                                viewHighlight.visibility = View.VISIBLE
                                viewHighlight.setBackgroundResource(R.drawable.aura_calendar_dot_blue)
                                holder.itemView.setOnClickListener(null)
                                tvDay.setTextColor(circleTextColor)
                                tvDay.alpha = 1.0f
                            }
                            isFestivo -> {
                                viewHighlight.visibility = View.VISIBLE
                                viewHighlight.setBackgroundResource(R.drawable.aura_calendar_dot)
                                holder.itemView.setOnClickListener(null)
                                tvDay.setTextColor(circleTextColor)
                                tvDay.alpha = 1.0f
                            }
                            else -> {
                                viewHighlight.visibility = View.GONE
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
            Toast.makeText(this, "No quedan huecos disponibles para hoy.", Toast.LENGTH_SHORT).show()
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
        
        val isPuesto = sala.tipo == "PUESTO"
        val fecha = viewModel.fechaSeleccionada.value ?: ""
        val hora = viewModel.horaSeleccionada.value ?: ""
        val slotBusqueda = if (isPuesto) "$fecha ${EmpleadosViewModel.SLOT_PUESTO}" else "$fecha $hora"
        
        val reservaActual = viewModel.reservas.value?.find { it.idSala == sala.id && it.fechaHora == slotBusqueda }
        val uid = Sesion.datos?.usuario?.uid ?: ""

        if (reservaActual == null) {
            tvEstado.text = getString(R.string.label_libre)
            tvEstado.setTextColor(Color.GREEN)
            btnAccion.text = getString(R.string.btn_reservar_mayuscula)
            btnAccion.setOnClickListener {
                viewModel.reservarSala(sala, viewModel.pisos.value?.find { p -> p.id == sala.idPiso }?.nombre ?: "")
            }
        } else {
            tvEstado.text = getString(R.string.label_ocupada_por, reservaActual.nombreUsuario)
            tvEstado.setTextColor(Color.RED)
            if (reservaActual.idUsuario == uid) {
                btnAccion.text = getString(R.string.btn_cancelar_mi_reserva)
                btnAccion.setOnClickListener {
                    viewModel.cancelReserva(reservaActual.id ?: "")
                }
            } else {
                btnAccion.visibility = View.GONE
            }
        }

        dialogDetallesActivo?.dismiss()
        dialogDetallesActivo = MaterialAlertDialogBuilder(this)
            .setView(dialogView)
            .create()
        
        dialogDetallesActivo?.show()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            onBackPressedDispatcher.onBackPressed()
            return true
        }
        return super.onOptionsItemSelected(item)
    }
}