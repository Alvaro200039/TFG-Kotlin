import androidx.activity.viewModels
import com.example.tfg_kotlin.databinding.ActivityEmpleadosBinding
import com.example.tfg_kotlin.ui.viewmodel.EmpleadosViewModel

class EmpleadosActivity : AppCompatActivity() {

    private lateinit var binding: ActivityEmpleadosBinding
    private val viewModel: EmpleadosViewModel by viewModels()

    private var snackbarActivo: Snackbar? = null

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
            title = ""
            setHomeAsUpIndicator(R.drawable.ic_adagora)
        }
        
        // Add dynamic UI components to toolbar
        setupToolbarDynamicUI()
    }

    private fun setupToolbarDynamicUI() {
        val spinner = Spinner(this).apply {
            setPopupBackgroundDrawable(ContextCompat.getDrawable(context, R.drawable.spinner_dropdown_background))
            layoutParams = Toolbar.LayoutParams(Toolbar.LayoutParams.WRAP_CONTENT, Toolbar.LayoutParams.WRAP_CONTENT).apply {
                gravity = Gravity.START
            }
        }
        binding.toolbar.addView(spinner)

        viewModel.pisos.observe(this) { pisos ->
            val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, pisos.map { it.nombre })
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            spinner.adapter = adapter
            
            spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                    val piso = pisos[position]
                    viewModel.loadSalas(piso.id ?: piso.nombre)
                    if (piso.imagenUrl != null) {
                        cargarImagenFondo(piso.imagenUrl!!)
                    }
                }
                override fun onNothingSelected(parent: AdapterView<*>) {}
            }
        }

        val btnFecha = ImageButton(this).apply {
            setImageResource(R.drawable.ic_calendar)
            setBackgroundColor(Color.TRANSPARENT)
            setOnClickListener { mostrarDialogoFecha() }
            layoutParams = Toolbar.LayoutParams(Toolbar.LayoutParams.WRAP_CONTENT, Toolbar.LayoutParams.WRAP_CONTENT).apply {
                gravity = Gravity.END
                marginEnd = 20
            }
        }
        binding.toolbar.addView(btnFecha)
    }

    private fun setupObservers() {
        viewModel.salas.observe(this) { salas ->
            actualizarVistaSalas(salas)
        }

        viewModel.reservas.observe(this) { 
            // Trigger UI update for button colors when reservations change
            actualizarColoresSalas()
        }

        viewModel.fechaSeleccionada.observe(this) { fecha ->
            // Update UI label
        }

        viewModel.error.observe(this) { msg ->
            Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupListeners() {
        binding.btnReservas.setOnClickListener { mostrarDialogoReservas() }
        binding.btnFranja.setOnClickListener { 
            if (viewModel.fechaSeleccionada.value.isNullOrEmpty()) {
                mostrarSnackbarFecha()
            } else {
                mostrarDialogoHoras()
            }
        }
    }

    private fun actualizarVistaSalas(salas: List<Sala>) {
        binding.contentLayout.removeAllViews()
        salas.forEach { sala ->
            val button = crearBotonSala(sala)
            binding.contentLayout.addView(button)
        }
    }

    private fun crearBotonSala(sala: Sala): Button {
        return Button(this).apply {
            text = formatearTextoSala(sala)
            tag = sala
            background = GradientDrawable().apply {
                setColor(Color.GRAY)
                cornerRadius = 50f
            }
            
            val lp = ConstraintLayout.LayoutParams(sala.ancho.toInt(), sala.alto.toInt()).apply {
                topToTop = ConstraintLayout.LayoutParams.PARENT_ID
                startToStart = ConstraintLayout.LayoutParams.PARENT_ID
                topMargin = sala.y.toInt()
                leftMargin = sala.x.toInt()
            }
            layoutParams = lp
            
            setOnClickListener {
                if (viewModel.fechaSeleccionada.value.isNullOrEmpty() || viewModel.horaSeleccionada.value.isNullOrEmpty()) {
                    mostrarSnackbarFecha()
                } else {
                    mostrarDialogoDetallesSala(sala)
                }
            }
        }
    }

    private fun actualizarColoresSalas() {
        val reservas = viewModel.reservas.value ?: return
        val uid = Sesion.datos?.usuario?.uid ?: ""
        
        for (i in 0 until binding.contentLayout.childCount) {
            val view = binding.contentLayout.getChildAt(i)
            if (view is Button && view.tag is Sala) {
                val sala = view.tag as Sala
                val reserva = reservas.find { it.idSala == sala.id }
                
                val color = when {
                    reserva == null -> R.color.green
                    reserva.idusuario == uid -> R.color.orange
                    else -> R.color.red
                }
                view.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(this, color))
            }
        }
    }

    private fun mostrarSnackbarFecha() {
        snackbarActivo?.dismiss()
        snackbarActivo = Snackbar.make(binding.contentLayout, getString(R.string.msg_selecciona_fecha_primero), Snackbar.LENGTH_LONG)
            .setAction(getString(R.string.btn_seleccionar_fecha)) { mostrarDialogoFecha() }
        snackbarActivo?.show()
    }

    private fun mostrarDialogoFecha() {
        val cal = Calendar.getInstance()
        DatePickerDialog(this, android.R.style.Theme_Material_Dialog_MinWidth, 
            { _, y, m, d ->
                val f = String.format("%02d/%02d/%d", d, m + 1, y)
                viewModel.updateFecha(f)
                mostrarDialogoHoras()
            }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)
        ).apply {
            datePicker.minDate = cal.timeInMillis
            window?.setBackgroundDrawableResource(R.drawable.datepicker_background)
            show()
        }
    }

    private fun mostrarDialogoHoras() {
        val franjas = viewModel.franjas.value ?: emptyList()
        if (franjas.isEmpty()) {
            Toast.makeText(this, getString(R.string.msg_no_franjas_disponibles), Toast.LENGTH_SHORT).show()
            return
        }
        
        AlertDialog.Builder(this)
            .setTitle(getString(R.string.title_selecciona_franja_para, viewModel.fechaSeleccionada.value))
            .setItems(franjas.toTypedArray()) { _, which ->
                viewModel.updateHora(franjas[which])
            }
            .show()
    }

    private fun cargarImagenFondo(url: String) {
        Glide.with(this).load(url).centerCrop().into(object : CustomTarget<Drawable>() {
            override fun onResourceReady(r: Drawable, t: Transition<in Drawable>?) {
                binding.contentLayout.background = r
            }
            override fun onLoadCleared(p: Drawable?) {}
        })
    }

    private fun formatearTextoSala(sala: Sala): String {
        val b = StringBuilder(sala.nombre)
        if (sala.extras.isNotEmpty()) {
            b.append("\n")
            sala.extras.forEach { e ->
                b.append(when(e) { "WiFi" -> "📶 "; "Proyector" -> "📽️ "; "Pizarra" -> "🖍️ "; else -> "" })
            }
        }
        return b.toString()
    }

    private fun mostrarDialogoDetallesSala(sala: Sala) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_detalles_sala, null)
        val tvNombre = dialogView.findViewById<TextView>(R.id.tvNombreSalaDetalle)
        val tvEstado = dialogView.findViewById<TextView>(R.id.tvEstadoSalaDetalle)
        val tvEquipamiento = dialogView.findViewById<TextView>(R.id.tvEquipamientoDetalle)
        val btnAccion = dialogView.findViewById<Button>(R.id.btnAccionReserva)

        tvNombre.text = sala.nombre
        tvEquipamiento.text = if (sala.extras.isEmpty()) getString(R.string.label_ninguno) else sala.extras.joinToString(", ")
        
        val reservaActual = viewModel.reservas.value?.find { it.idSala == sala.id }
        val uid = Sesion.datos?.usuario?.uid ?: ""

        if (reservaActual == null) {
            tvEstado.text = getString(R.string.label_libre)
            tvEstado.setTextColor(Color.GREEN)
            btnAccion.text = getString(R.string.btn_reservar_mayus)
            btnAccion.setOnClickListener {
                viewModel.reservarSala(sala, viewModel.pisos.value?.find { p -> p.id == sala.idPiso }?.nombre ?: "")
            }
        } else {
            tvEstado.text = getString(R.string.label_ocupada_por, reservaActual.nombreUsuario)
            tvEstado.setTextColor(Color.RED)
            if (reservaActual.idusuario == uid) {
                btnAccion.text = getString(R.string.btn_cancelar_mi_reserva)
                btnAccion.setOnClickListener {
                    viewModel.cancelReserva(reservaActual.id ?: "")
                }
            } else {
                btnAccion.visibility = View.GONE
            }
        }

        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .create()
        
        viewModel.reservaStatus.observe(this) { success ->
            if (success) {
                dialog.dismiss()
                Toast.makeText(this, getString(R.string.msg_operacion_exito), Toast.LENGTH_SHORT).show()
            }
        }

        dialog.show()
    }

    private fun mostrarDialogoReservas() {
        // ... (This can be shared or moved to VM but it involves complex UI building)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            onBackPressedDispatcher.onBackPressed()
            return true
        }
        return super.onOptionsItemSelected(item)
    }
}