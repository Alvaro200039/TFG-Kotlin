package com.example.tfg_kotlin

import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.MotionEvent
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.SeekBar
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.graphics.toColorInt
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.bumptech.glide.Glide
import com.example.tfg_kotlin.data.model.Sala
import com.example.tfg_kotlin.databinding.ActivityCreacionBinding
import com.example.tfg_kotlin.ui.viewmodel.CreacionViewModel
import kotlin.math.abs

class CreacionActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCreacionBinding
    private val viewModel: CreacionViewModel by viewModels()

    private var imagen: ByteArray? = null

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

        viewModel.loadPisos()
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.myToolbar)
        supportActionBar?.apply {
            setDisplayShowTitleEnabled(false)
            setDisplayHomeAsUpEnabled(true)
            setHomeAsUpIndicator(R.drawable.ic_adagora)
        }
        binding.toolbarTitle.setOnClickListener { showChangeTitleDialog() }
    }

    private fun setupObservers() {
        viewModel.pisoActual.observe(this) { piso ->
            binding.toolbarTitle.text = piso?.nombre ?: getString(R.string.title_piso_n)
        }

        viewModel.salas.observe(this) { salas ->
            actualizarVistaSalas(salas)
        }

        viewModel.error.observe(this) { msg ->
            Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
        }

        viewModel.franjas.observe(this) {
            // Si el diálogo de franjas está abierto, esto lo actualizaría si tuviéramos una referencia
        }

        viewModel.saveStatus.observe(this) { success ->
            if (success) {
                Toast.makeText(this, getString(R.string.msg_distribucion_guardada), Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun setupListeners() {
        binding.btnHoras.setOnClickListener { cargarFranjas() }
        binding.btnPisos.setOnClickListener { mostrarDialogoEliminarPisos() }
        binding.btnPlano.setOnClickListener { openGallery() }
        binding.btnSala.setOnClickListener { addMovableButton() }
    }

    private fun actualizarVistaSalas(salas: List<Sala>) {
        binding.container.removeAllViews()
        // Mantener la imagen de fondo si existe
        binding.container.addView(binding.imageFondo)
        
        salas.forEach { sala ->
            val button = crearBotonSala(sala)
            binding.container.addView(button)
        }
    }

    private fun crearBotonSala(sala: Sala): Button {
        return Button(this).apply {
            text = sala.nombre
            background = GradientDrawable().apply {
                setColor("#BEBEBE".toColorInt())
                cornerRadius = 50f
            }
            setPadding(50, 20, 50, 20)
            tag = sala
            
            val lp = ConstraintLayout.LayoutParams(sala.ancho.toInt(), sala.alto.toInt()).apply {
                topMargin = sala.y.toInt()
                leftMargin = sala.x.toInt()
                topToTop = ConstraintLayout.LayoutParams.PARENT_ID
                startToStart = ConstraintLayout.LayoutParams.PARENT_ID
            }
            layoutParams = lp
            
            setOnTouchListener(MovableTouchListener())
            setOnClickListener { showButtonOptions(this) }
            
            actualizarBotonConSala(this, sala)
        }
    }

    private fun addMovableButton() {
        val nuevaSala = Sala(
            nombre = getString(R.string.label_sala_defecto) + " " + ((viewModel.salas.value?.size ?: 0) + 1),
            x = 100f,
            y = 100f,
            ancho = 300f,
            alto = 200f
        )
        viewModel.addSala(nuevaSala)
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
            R.id.action_save -> {
                viewModel.guardarDistribucion()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun cargarFranjas() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_franjas_horas, null)
        val layoutFranjas = dialogView.findViewById<LinearLayout>(R.id.layoutFranjas)
        val etHoraInicio = dialogView.findViewById<EditText>(R.id.etHoraInicio)
        val etMinInicio = dialogView.findViewById<EditText>(R.id.etMinInicio)
        val etHoraFin = dialogView.findViewById<EditText>(R.id.etHoraFin)
        val etMinFin = dialogView.findViewById<EditText>(R.id.etMinFin)
        val btnAdd = dialogView.findViewById<Button>(R.id.btnAddFranja)

        val dialog = AlertDialog.Builder(this)
            .setTitle(getString(R.string.title_franjas_horarias))
            .setView(dialogView)
            .setNegativeButton(getString(R.string.btn_cerrar), null)
            .create()

        viewModel.franjas.observe(this) { franjas ->
            layoutFranjas.removeAllViews()
            franjas.forEach { franja ->
                val tv = TextView(this).apply {
                    text = franja
                    textSize = 18f
                    setPadding(10, 10, 10, 10)
                    setOnClickListener {
                        AlertDialog.Builder(this@CreacionActivity)
                            .setTitle(getString(R.string.title_eliminar_franja))
                            .setMessage(getString(R.string.msg_confirmar_eliminar_franja, franja))
                            .setPositiveButton(getString(R.string.btn_si)) { _, _ -> viewModel.removeFranja(franja) }
                            .setNegativeButton(getString(R.string.btn_no), null)
                            .show()
                    }
                }
                layoutFranjas.addView(tv)
            }
        }

        btnAdd.setOnClickListener {
            val h1 = etHoraInicio.text.toString().padStart(2, '0')
            val m1 = etMinInicio.text.toString().padStart(2, '0')
            val h2 = etHoraFin.text.toString().padStart(2, '0')
            val m2 = etMinFin.text.toString().padStart(2, '0')
            
            if (h1.length == 2 && m1.length == 2 && h2.length == 2 && m2.length == 2) {
                viewModel.addFranja("$h1:$m1-$h2:$m2")
            } else {
                Toast.makeText(this, getString(R.string.err_formato_hora_invalido), Toast.LENGTH_SHORT).show()
            }
        }

        viewModel.loadFranjas()
        dialog.show()
    }

    private fun mostrarDialogoEliminarPisos() {
        val pisos = viewModel.pisos.value ?: return
        val nombres = pisos.map { it.nombre }.toTypedArray()
        
        AlertDialog.Builder(this)
            .setTitle(getString(R.string.title_selecciona_piso_eliminar))
            .setItems(nombres) { _, which ->
                val piso = pisos[which]
                AlertDialog.Builder(this)
                    .setTitle(getString(R.string.title_confirmar_eliminacion))
                    .setMessage(getString(R.string.msg_confirmar_eliminar_piso, piso.nombre))
                    .setPositiveButton(getString(R.string.btn_si)) { _, _ -> viewModel.eliminarPiso(piso) }
                    .setNegativeButton(getString(R.string.btn_no), null)
                    .show()
            }
            .show()
    }

    private fun openGallery() {
        getImage.launch("image/*")
    }

    private val getImage = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            imagen = contentResolver.openInputStream(uri)?.use { it.readBytes() }
            Glide.with(this).load(uri).fitCenter().into(binding.imageFondo)
        }
    }

    private fun showChangeTitleDialog() {
        val currentPiso = viewModel.pisoActual.value
        val editText = EditText(this).apply {
            setText(currentPiso?.nombre ?: getString(R.string.title_piso_n))
            setSelection(text.length)
        }

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(50, 40, 50, 10)
            addView(editText)
        }

        AlertDialog.Builder(this)
            .setTitle(if (currentPiso == null) getString(R.string.title_crear_nuevo_piso) else getString(R.string.title_editar_nombre_piso))
            .setView(layout)
            .setPositiveButton(getString(R.string.btn_guardar)) { _, _ ->
                val nuevoTitulo = editText.text.toString().trim().take(11)
                if (nuevoTitulo.isNotEmpty()) {
                    viewModel.updatePisoActual(nuevoTitulo)
                }
            }
            .setNegativeButton(getString(R.string.btn_cancelar), null)
            .show()
    }

    private fun showButtonOptions(button: Button) {
        val options = arrayOf(getString(R.string.opt_editar), getString(R.string.opt_eliminar), getString(R.string.opt_cambiar_tamano))
        AlertDialog.Builder(this)
            .setTitle(getString(R.string.title_opciones_sala))
            .setItems(options) { _, which ->
                val sala = button.tag as Sala
                when (which) {
                    0 -> showEditButtonDialog(button)
                    1 -> viewModel.removeSala(sala)
                    2 -> mostrarDialogoCambiarTamano(sala)
                }
            }
            .show()
    }

    private fun mostrarDialogoCambiarTamano(sala: Sala) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_cambiar_tamanio, null)
        val seekBarAncho = dialogView.findViewById<SeekBar>(R.id.seekBarAncho)
        val seekBarAlto = dialogView.findViewById<SeekBar>(R.id.seekBarAlto)
        val tvAncho = dialogView.findViewById<TextView>(R.id.tvAnchoValor)
        val tvAlto = dialogView.findViewById<TextView>(R.id.tvAltoValor)

        seekBarAncho.progress = sala.ancho.toInt()
        seekBarAlto.progress = sala.alto.toInt()
        tvAncho.text = sala.ancho.toInt().toString()
        tvAlto.text = sala.alto.toInt().toString()

        seekBarAncho.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(s: SeekBar?, p: Int, b: Boolean) { tvAncho.text = p.toString() }
            override fun onStartTrackingTouch(s: SeekBar?) {}
            override fun onStopTrackingTouch(s: SeekBar?) {}
        })

        seekBarAlto.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(s: SeekBar?, p: Int, b: Boolean) { tvAlto.text = p.toString() }
            override fun onStartTrackingTouch(s: SeekBar?) {}
            override fun onStopTrackingTouch(s: SeekBar?) {}
        })

        AlertDialog.Builder(this)
            .setTitle(getString(R.string.title_cambiar_tamano_sala))
            .setView(dialogView)
            .setPositiveButton(getString(R.string.btn_guardar)) { _, _ ->
                val newSala = sala.copy(ancho = seekBarAncho.progress.toFloat(), alto = seekBarAlto.progress.toFloat())
                viewModel.updateSala(sala, newSala)
            }
            .setNegativeButton(getString(R.string.btn_cancelar), null)
            .show()
    }

    private fun showEditButtonDialog(button: Button) {
        val sala = button.tag as Sala
        val dialogView = layoutInflater.inflate(R.layout.dialog_editar_sala, null)
        val etNombre = dialogView.findViewById<EditText>(R.id.etNombreSala).apply { setText(sala.nombre) }
        val spinnerTamano = dialogView.findViewById<Spinner>(R.id.spinnerTamanoSala)
        val cbWiFi = dialogView.findViewById<CheckBox>(R.id.cbWiFi).apply { isChecked = sala.extras.contains("WiFi") }
        val cbProyector = dialogView.findViewById<CheckBox>(R.id.cbProyector).apply { isChecked = sala.extras.contains("Proyector") }
        val cbPizarra = dialogView.findViewById<CheckBox>(R.id.cbPizarra).apply { isChecked = sala.extras.contains("Pizarra") }

        val tamanos = arrayOf("Pequeña", "Mediana", "Grande")
        spinnerTamano.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, tamanos)
        spinnerTamano.setSelection(tamanos.indexOf(sala.tamano).coerceAtLeast(0))

        AlertDialog.Builder(this)
            .setTitle(getString(R.string.title_editar_detalles_sala))
            .setView(dialogView)
            .setPositiveButton(getString(R.string.btn_guardar)) { _, _ ->
                val extras = mutableListOf<String>()
                if (cbWiFi.isChecked) extras.add("WiFi")
                if (cbProyector.isChecked) extras.add("Proyector")
                if (cbPizarra.isChecked) extras.add("Pizarra")
                
                val newSala = sala.copy(
                    nombre = etNombre.text.toString(),
                    tamano = spinnerTamano.selectedItem.toString(),
                    extras = extras
                )
                viewModel.updateSala(sala, newSala)
            }
            .setNegativeButton(getString(R.string.btn_cancelar), null)
            .show()
    }

    private fun actualizarBotonConSala(button: Button, sala: Sala) {
        button.text = buildString {
            append(sala.nombre)
            if (sala.extras.isNotEmpty()) {
                append("\n")
                sala.extras.forEach { extra ->
                    when (extra) {
                        "WiFi" -> append("📶 ")
                        "Proyector" -> append("📽️ ")
                        "Pizarra" -> append("🖍️ ")
                    }
                }
            }
        }
    }

    inner class MovableTouchListener : View.OnTouchListener {
        private var dX = 0f
        private var dY = 0f
        private var startX = 0f
        private var startY = 0f
        private val clickThreshold = 10

        override fun onTouch(view: View, event: MotionEvent): Boolean {
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    dX = view.x - event.rawX
                    dY = view.y - event.rawY
                    startX = event.rawX
                    startY = event.rawY
                }
                MotionEvent.ACTION_MOVE -> {
                    view.animate().x(event.rawX + dX).y(event.rawY + dY).setDuration(0).start()
                }
                MotionEvent.ACTION_UP -> {
                    val deltaX = abs(event.rawX - startX)
                    val deltaY = abs(event.rawY - startY)

                    if (deltaX < clickThreshold && deltaY < clickThreshold) {
                        view.performClick()
                    } else {
                        val oldSala = view.tag as Sala
                        val newSala = oldSala.copy(x = view.x, y = view.y)
                        viewModel.updateSala(oldSala, newSala)
                    }
                }
            }
            return true
        }
    }
}
