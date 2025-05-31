package com.example.tfg_kotlin

import android.R.attr.id
import android.content.res.ColorStateList
import android.content.res.Resources
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Bundle
import android.os.Looper
import android.text.Editable
import android.text.InputFilter
import android.text.TextWatcher
import android.util.Log
import android.view.Gravity
import android.view.Menu
import android.view.MenuItem
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
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
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.graphics.toColorInt
import com.bumptech.glide.Glide
import com.google.android.material.snackbar.Snackbar
import androidx.core.view.children
import androidx.lifecycle.lifecycleScope
import com.example.tfg_kotlin.Utils.naturalOrderKey
import com.example.tfg_kotlin.Utils.compareNaturalKeys
import com.example.tfg_kotlin.dao.PisoDao
import com.example.tfg_kotlin.dao.SalaDao
import com.example.tfg_kotlin.database.AppDatabase
import com.example.tfg_kotlin.entities.Empresa
import com.example.tfg_kotlin.entities.FranjaHoraria
import com.example.tfg_kotlin.entities.Piso
import com.example.tfg_kotlin.entities.Salas
import com.example.tfg_kotlin.repository.AppRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.logging.Handler
import androidx.core.net.toUri


class Activity_creacion : AppCompatActivity() {

    private lateinit var container: ConstraintLayout
    private lateinit var repository: AppRepository
    private var pisoActual: Piso? = null
    private var empresaId: Int = -1  // Declaras la propiedad aquí



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_creacion)

        enableEdgeToEdge()
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.creacion)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        val toolbar = findViewById<Toolbar>(R.id.my_toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayShowTitleEnabled(false)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)



        val titleView = findViewById<TextView>(R.id.toolbar_title)
        titleView.text = "Piso nº" // valor provisional hasta que cargue desde Room
        titleView.setOnClickListener {
            showChangeTitleDialog()
        }

        val db = AppDatabase.getDatabase(applicationContext)
        repository = AppRepository(
            db.usuarioDao(),
            db.salaDao(),
            db.reservaDao(),
            db.franjahorariaDao(),
            db.pisoDao(),
            db.empresaDao()
        )

        lifecycleScope.launch {
            val empresaPorDefecto = repository.empresaDao.obtenerEmpresaPorId(1)
            if (empresaPorDefecto == null) {
                repository.empresaDao.insertarEmpresa(
                    Empresa(
                        id = 1, nombre = "Empresa por defecto", creadorId = 1, cif = "A1111111")
                )
            }

            // Ahora que sabemos que la empresa 1 existe, obtenemos empresaId de prefs o asignamos 1
            val prefs = getSharedPreferences("usuario_prefs", MODE_PRIVATE)
            var empresaId = prefs.getInt("empresa_id", -1)
            if (empresaId == -1) empresaId = 1

            // Guardar en variable global para usar en la Activity
            this@Activity_creacion.empresaId = empresaId

            // Aquí puedes continuar con cualquier carga o inicialización que dependa de empresaId
        }

        lifecycleScope.launch {
            repository.pisoDao.obtenerTodosLosPisos().collectLatest { pisos ->
                if (pisos.isNotEmpty()) {
                    pisoActual = pisos.last()
                    titleView.text = pisoActual?.nombre ?: "Sin piso"
                } else {
                    Toast.makeText(this@Activity_creacion, "No hay pisos creados", Toast.LENGTH_SHORT).show()
                }
            }
        }

        // Enlace botones
        val btnHoras = findViewById<LinearLayout>(R.id.btn_horas)
        val btnPlano = findViewById<LinearLayout>(R.id.btn_plano)
        val btnSala = findViewById<LinearLayout>(R.id.btn_sala)
        val btnPisos = findViewById<LinearLayout>(R.id.btn_pisos)

        btnHoras.setOnClickListener { mostrarDialogoFranjas() }
        btnPisos.setOnClickListener { mostrarDialogoEliminarPisos() }
        btnPlano.setOnClickListener { openGallery() }
        btnSala.setOnClickListener { addMovableButton() }

        container = findViewById(R.id.container)
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
                lifecycleScope.launch {
                    try {
                        val pisoNombre = findViewById<TextView>(R.id.toolbar_title).text.toString()
                        val prefs = getSharedPreferences("usuario_prefs", MODE_PRIVATE)
                        val empresaId = prefs.getInt("empresa_id", 1)

                        guardarDistribucion(
                            empresaId = empresaId,
                            pisoNombre = pisoNombre,
                            imagen = imagen,
                            container = container,
                            pisoDao = repository.pisoDao,
                            salaDao = repository.salaDao
                        )
                    } catch (e: Exception) {
                        Log.e("Activity_creacion", "Error guardando: ${e.message}", e)
                        withContext(Dispatchers.Main) {
                            Toast.makeText(this@Activity_creacion, "Error guardando: ${e.message}", Toast.LENGTH_LONG).show()
                        }
                    }
                }
                true
            }

            else -> super.onOptionsItemSelected(item)
        }
    }

private fun mostrarDialogoFranjas() {
    val dialogView = layoutInflater.inflate(R.layout.dialog_franjas_horas, null)

    val layoutFranjas = dialogView.findViewById<LinearLayout>(R.id.layoutFranjas)
    val botonAgregar = dialogView.findViewById<Button>(R.id.btnAddFranja)

    val editHoraInicio = dialogView.findViewById<EditText>(R.id.etHoraInicio)
    val editMinutoInicio = dialogView.findViewById<EditText>(R.id.etMinInicio)
    val editHoraFin = dialogView.findViewById<EditText>(R.id.etHoraFin)
    val editMinutoFin = dialogView.findViewById<EditText>(R.id.etMinFin)

    editHoraInicio.autoAdvanceTo(editMinutoInicio)
    editMinutoInicio.autoAdvanceTo(editHoraFin)
    editHoraFin.autoAdvanceTo(editMinutoFin)
    editMinutoFin.autoAdvanceTo(null)

    val dialog = AlertDialog.Builder(this)
        .setTitle("Añadir franjas horarias")
        .setView(dialogView)
        .setNegativeButton("Cerrar", null)
        .create()

    dialog.window?.setBackgroundDrawableResource(R.drawable.dialog_background)
    dialog.show()

    // Función para actualizar la UI con franjas que vienen de DB
    fun actualizarListaFranjasUI(franjas: List<FranjaHoraria>) {
        layoutFranjas.removeAllViews()
        for (franja in franjas.sortedBy { it.hora }) {
            val franjaView = LinearLayout(dialogView.context).apply {
                orientation = LinearLayout.HORIZONTAL

                val textview = TextView(dialogView.context).apply {
                    text = franja.hora
                    textSize = 16f
                    setPadding(8, 4, 8, 4)
                }

                val botonEliminar = Button(dialogView.context).apply {
                    text = "❌"
                    textSize = 14f
                    setBackgroundColor(Color.TRANSPARENT)
                    setPadding(16, 0, 16, 0)
                    setOnClickListener {
                        lifecycleScope.launch {
                            repository.franjaHorariaDao.eliminarFranja(franja)
                            // Volver a cargar la lista y actualizar UI
                            val nuevasFranjas = withContext(Dispatchers.IO) {
                                repository.franjaHorariaDao.getTodasFranjas()
                            }
                            actualizarListaFranjasUI(nuevasFranjas)
                        }
                    }
                }

                addView(textview)
                addView(botonEliminar)
            }
            layoutFranjas.addView(franjaView)
        }
    }


    // Observar cambios en las franjas con Flow y actualizar UI
    lifecycleScope.launch {
        val franjas = withContext(Dispatchers.IO) {
            repository.franjaHorariaDao.getTodasFranjas()

        }
        actualizarListaFranjasUI(franjas)
    }

    botonAgregar.setOnClickListener {
        val hInicioStr = editHoraInicio.text.toString()
        val mInicioStr = editMinutoInicio.text.toString()
        val hFinStr = editHoraFin.text.toString()
        val mFinStr = editMinutoFin.text.toString()

        // Validar que no estén vacíos
        if (hInicioStr.isBlank() || mInicioStr.isBlank() || hFinStr.isBlank() || mFinStr.isBlank()) {
            Toast.makeText(this, "Por favor, rellena todos los campos.", Toast.LENGTH_SHORT).show()
            return@setOnClickListener
        }

        // Convertir a enteros y validar rangos
        val hInicio = hInicioStr.toIntOrNull()
        val mInicio = mInicioStr.toIntOrNull()
        val hFin = hFinStr.toIntOrNull()
        val mFin = mFinStr.toIntOrNull()

        if (hInicio == null || mInicio == null || hFin == null || mFin == null) {
            Toast.makeText(this, "Introduce números válidos.", Toast.LENGTH_SHORT).show()
            return@setOnClickListener
        }

        if (hInicio !in 1..24) {
            Toast.makeText(this, "La hora de inicio debe estar entre 1 y 24.", Toast.LENGTH_SHORT).show()
            return@setOnClickListener
        }
        if (hFin !in 1..24) {
            Toast.makeText(this, "La hora de fin debe estar entre 1 y 24.", Toast.LENGTH_SHORT).show()
            return@setOnClickListener
        }
        if (mInicio !in 0..59) {
            Toast.makeText(this, "Los minutos de inicio deben estar entre 0 y 59.", Toast.LENGTH_SHORT).show()
            return@setOnClickListener
        }
        if (mFin !in 0..59) {
            Toast.makeText(this, "Los minutos de fin deben estar entre 0 y 59.", Toast.LENGTH_SHORT).show()
            return@setOnClickListener
        }

        // Comparar hora inicio y fin
        val inicioEnMinutos = hInicio * 60 + mInicio
        val finEnMinutos = hFin * 60 + mFin
        if (inicioEnMinutos >= finEnMinutos) {
            Toast.makeText(this, "La hora de inicio debe ser anterior a la hora de fin.", Toast.LENGTH_SHORT).show()
            return@setOnClickListener
        }

        val horaInicioStr = "%02d:%02d".format(hInicio, mInicio)
        val horaFinStr = "%02d:%02d".format(hFin, mFin)
        val nuevaFranja = "$horaInicioStr - $horaFinStr"

        // Insertar nueva franja en DB
        lifecycleScope.launch {
            repository.franjaHorariaDao.insertarFranja(FranjaHoraria(nuevaFranja))
            val nuevasFranjas = withContext(Dispatchers.IO) {
                repository.franjaHorariaDao.getTodasFranjas()
            }
            actualizarListaFranjasUI(nuevasFranjas)
        }

        // Limpiar campos
        editHoraInicio.text.clear()
        editMinutoInicio.text.clear()
        editHoraFin.text.clear()
        editMinutoFin.text.clear()
    }


    val btnCerrar = dialog.getButton(AlertDialog.BUTTON_NEGATIVE)
    btnCerrar.setTextColor(Color.BLACK)
}


private fun openGallery() {
        getImage.launch("image/*")
    }

    private val salasEnMemoria = mutableListOf<Salas>() // Lista temporal en memoria

    private fun addMovableButton() {
        // No compruebo pisoActual porque aún no existe piso guardado

        val sala = Salas(
            nombre = "Sala",
            tamaño = "pequeña", // o "grande"
            pisoId = -1, // provisional, sin piso asignado aún
            x = 100f,
            y = 100f,
            ancho = 300f,
            alto = 200f,
            extras = emptyList()
        )

        val button = Button(this).apply {
            text = sala.nombre
            background = GradientDrawable().apply {
                setColor("#BEBEBE".toColorInt())
                cornerRadius = 50f
            }
            setPadding(50, 20, 50, 20)
            setOnTouchListener(MovableTouchListener())
            setOnClickListener {
                showButtonOptions(this)
            }
            tag = sala
        }

        container.addView(button)
        val layoutParams = ConstraintLayout.LayoutParams(
            ConstraintLayout.LayoutParams.WRAP_CONTENT,
            ConstraintLayout.LayoutParams.WRAP_CONTENT
        ).apply {
            topMargin = sala.y.toInt()
            leftMargin = sala.x.toInt()
        }
        button.layoutParams = layoutParams

        // Guardamos la sala en memoria, no en Room todavía
        salasEnMemoria.add(sala)
    }



    private fun showChangeTitleDialog() {
        val piso = pisoActual

        val editText = EditText(this).apply {
            setText(piso?.nombre ?: "Piso nº ")  // Si no hay piso, campo inicial
            setSelection(text.length)
        }

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(50, 40, 50, 10)
            addView(editText)
        }

        val title = if (piso == null) "Crear nuevo piso" else "Editar nombre del piso"

        val dialog = AlertDialog.Builder(this)
            .setTitle(title)
            .setView(layout)
            .setPositiveButton("Guardar") { _, _ ->
                val maxTitleLength = 11
                val nuevoTitulo = editText.text.toString().trim().take(maxTitleLength)

                if (nuevoTitulo.isEmpty() || nuevoTitulo.equals("Piso nº", ignoreCase = true) || nuevoTitulo.equals("Piso nº ", ignoreCase = true)) {
                    showToast("Por favor, cambie el nombre del piso antes de guardar")
                } else {
                    // SOLO actualizamos el nombre en memoria y en UI, no guardamos en base de datos
                    pisoActual = if (piso == null) {
                        // Piso nuevo en memoria, sin ID aún (id=0 o -1)
                        Piso(id, nombre = nuevoTitulo, imagen = imagen, empresaId = 1)
                    } else {
                        piso.copy(nombre = nuevoTitulo)
                    }

                    // Actualizamos el título en la toolbar o donde lo muestres
                    findViewById<TextView>(R.id.toolbar_title).text = nuevoTitulo
                    showToast("Nombre del piso modificado (pendiente de guardar)")
                }
            }
            .setNegativeButton("Cancelar", null)
            .create()

        dialog.setOnShowListener {
            dialog.window?.setBackgroundDrawableResource(R.drawable.dialog_background)
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(Color.BLACK)
            dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(Color.RED)
        }

        dialog.show()
    }



    private fun showToast(msg: String) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
    }

    private fun showButtonOptions(button: Button) {
        val options = arrayOf("Editar", "Eliminar", "Cambiar tamaño")

        val builder = AlertDialog.Builder(this)
        builder.setTitle("Opciones de la sala")

        builder.setItems(options) { dialog, which ->
            when (which) {
                0 -> showEditButtonDialog(button) // Editar texto
                1 -> container.removeView(button) // Eliminar botón
                2 -> {
                    val sala = button.tag as? Salas
                    if (sala != null) {
                        mostrarDialogoCambiarTamanio(button, sala)
                    } else {
                        Toast.makeText(this, "Modifica priemero la sala", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }

        val dialog = builder.create()

        // Cambiar fondo de el dialog
        dialog.window?.setBackgroundDrawableResource(R.drawable.dialog_background) // Aquí se aplica el fondo

        dialog.setOnShowListener {
            // Opcional: personalizar las opciones dentro del diálogo (si lo deseas)
            val listView = dialog.listView
            for (i in 0 until listView.count) {
                val itemView = listView.getChildAt(i)
                itemView?.let {
                    val textView = it.findViewById<TextView>(android.R.id.text1)
                    textView?.setTextColor(Color.BLACK)  // Cambiar el color del texto de las opciones
                }
            }
        }
        dialog.show()
    }


    private fun mostrarDialogoCambiarTamanio(salaButton: Button, sala: Salas) {
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(50, 40, 50, 10)

        }

        val anchoSeekBar = SeekBar(this).apply {
            max = 1070
            progress = salaButton.width
            thumbTintList = ColorStateList.valueOf(Color.DKGRAY)
            progressTintList = ColorStateList.valueOf(Color.DKGRAY)
        }

        val anchoValue = TextView(this).apply {
            text = "Ancho: ${anchoSeekBar.progress}px"
        }

        val altoSeekBar = SeekBar(this).apply {
            max = 1750
            progress = salaButton.height
            thumbTintList = ColorStateList.valueOf(Color.DKGRAY)
            progressTintList = ColorStateList.valueOf(Color.DKGRAY)
        }

        val altoValue = TextView(this).apply {
            text = "Alto: ${altoSeekBar.progress}px"
        }

        anchoSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                anchoValue.text = "Ancho: ${seekBar?.progress}px"
                val params = salaButton.layoutParams
                params.width = seekBar?.progress ?: salaButton.width
                salaButton.layoutParams = params
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        altoSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                altoValue.text = "Alto: ${seekBar?.progress}px"
                val params = salaButton.layoutParams
                params.height = seekBar?.progress ?: salaButton.height
                salaButton.layoutParams = params
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        layout.addView(anchoValue)
        layout.addView(anchoSeekBar)
        layout.addView(altoValue)
        layout.addView(altoSeekBar)

        val dialog = AlertDialog.Builder(this)
            .setTitle("Cambiar tamaño de ${sala.nombre}")
            .setView(layout)
            .setPositiveButton("Aplicar") { _, _ ->
                val nuevoAncho = anchoSeekBar.progress
                val nuevoAlto = altoSeekBar.progress
                val params = salaButton.layoutParams
                params.width = nuevoAncho
                params.height = nuevoAlto
                salaButton.layoutParams = params
                actualizarTamanioSalaGuardada(sala.nombre, nuevoAncho, nuevoAlto)
            }
            .setNegativeButton("Cancelar", null)
            .create()

        dialog.setOnShowListener {
            // Calculamos posición del botón
            val location = IntArray(2)
            salaButton.getLocationOnScreen(location)
            val botonY = location[1]
            val screenHeight = Resources.getSystem().displayMetrics.heightPixels

            val layoutParams = dialog.window?.attributes
            layoutParams?.gravity = if (botonY > screenHeight / 2) Gravity.TOP else Gravity.BOTTOM
            dialog.window?.attributes = layoutParams

            dialog.window?.setBackgroundDrawableResource(R.drawable.dialog_background)
            dialog.getButton(AlertDialog.BUTTON_POSITIVE)?.setTextColor(Color.BLACK)
            dialog.getButton(AlertDialog.BUTTON_NEGATIVE)?.setTextColor(Color.RED)
        }
        dialog.show()
    }

    private fun actualizarTamanioSalaGuardada(nombreSala: String, nuevoAncho: Int, nuevoAlto: Int) {
        val pisoId = pisoActual?.id ?: return

        lifecycleScope.launch {
            val sala = repository.salaDao.obtenerSalaPorNombreYPiso(nombreSala, pisoId)
            if (sala != null && (sala.ancho != nuevoAncho.toFloat() || sala.alto != nuevoAlto.toFloat())) {
                val salaActualizada = sala.copy(
                    ancho = nuevoAncho.toFloat(),
                    alto = nuevoAlto.toFloat()
                )
                repository.salaDao.actualizar(salaActualizada)
            }
        }
    }


    private fun showEditButtonDialog(button: Button) {
        val sala = button.tag as? Salas
        if (sala == null) {
            Toast.makeText(this, "No se encontró la sala asociada al botón", Toast.LENGTH_SHORT).show()
            return
        }

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(50, 40, 50, 10)
        }

        val editTextNombre = EditText(this).apply {
            hint = "Nuevo nombre"
            setText(sala.nombre)
            val maxLength = 20
            val filter = InputFilter.LengthFilter(maxLength)
            filters = arrayOf(filter)
        }

        val charCountTextView = TextView(this).apply {
            text = "${sala.nombre.length}/20"
            setTextColor("#000000".toColorInt())
        }

        editTextNombre.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                val currentLength = s?.length ?: 0
                charCountTextView.text = "$currentLength/20"
            }
        })

        val tamanios = listOf("Pequeño", "Grande")

        val adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_dropdown_item,
            tamanios
        )
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)

        val spinnerTamanio = Spinner(this).apply {
            this.adapter = adapter
            setSelection(tamanios.indexOf(sala.tamaño).takeIf { it >= 0 } ?: 0)
            background = ContextCompat.getDrawable(context, R.drawable.spinner_dropdown_background)
            setPopupBackgroundDrawable(ContextCompat.getDrawable(context, R.drawable.spinner_dropdown_background))
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = 16
                bottomMargin = 16
            }
        }


        val checkWifi = CheckBox(this).apply {
            text = "WiFi"
            isChecked = sala.extras.contains("WiFi")
            buttonTintList = ColorStateList.valueOf(Color.GRAY)
        }
        val checkProyector = CheckBox(this).apply {
            text = "Proyector"
            isChecked = sala.extras.contains("Proyector")
            buttonTintList = ColorStateList.valueOf(Color.GRAY)
        }
        val checkPizarra = CheckBox(this).apply {
            text = "Pizarra"
            isChecked = sala.extras.contains("Pizarra")
            buttonTintList = ColorStateList.valueOf(Color.GRAY)
        }

        layout.apply {
            addView(editTextNombre)
            addView(charCountTextView)
            addView(spinnerTamanio)
            addView(checkWifi)
            addView(checkProyector)
            addView(checkPizarra)
        }

        val builder = AlertDialog.Builder(this)
            .setTitle("Editar sala")
            .setView(layout)
            .setPositiveButton("Guardar", null) // Controlamos el click manualmente
            .setNegativeButton("Cancelar", null)

        val dialog = builder.create()
        dialog.window?.setBackgroundDrawableResource(R.drawable.dialog_background)

        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE)?.setTextColor(Color.BLACK)
            dialog.getButton(AlertDialog.BUTTON_NEGATIVE)?.setTextColor(Color.RED)

            val positiveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE)
            positiveButton.setOnClickListener {
                val nuevoNombre = editTextNombre.text.toString().trim()

                if (nuevoNombre.isEmpty()) {
                    editTextNombre.error = "El nombre no puede estar vacío"
                    return@setOnClickListener
                }

                val nombreRepetido = container.children
                    .filterIsInstance<Button>()
                    .filter { it != button }
                    .mapNotNull { (it.tag as? Salas)?.nombre }
                    .any { it.equals(nuevoNombre, ignoreCase = true) }

                if (nombreRepetido) {
                    editTextNombre.error = "Ese nombre ya está en uso"
                    return@setOnClickListener
                }

                // Crear una copia modificada de la sala
                val salaEditada = sala.copy(
                    nombre = nuevoNombre,
                    tamaño = spinnerTamanio.selectedItem?.toString().toString(),
                    extras = mutableListOf<String>().apply {
                        if (checkWifi.isChecked) add("WiFi")
                        if (checkProyector.isChecked) add("Proyector")
                        if (checkPizarra.isChecked) add("Pizarra")
                    }
                )

                // Guardar en Room en coroutine
                lifecycleScope.launch {
                    repository.salaDao.actualizar(salaEditada)
                    actualizarBotonConSala(button, salaEditada)
                    button.tag = salaEditada
                    Toast.makeText(this@Activity_creacion, "Sala actualizada", Toast.LENGTH_SHORT).show()
                    dialog.dismiss()
                }

            }
        }

        dialog.show()
    }


    private fun actualizarBotonConSala(button: Button, sala: Salas) {
        val builder = StringBuilder()
        builder.append(sala.nombre)

        if (sala.extras.isNotEmpty()) {
            builder.append("\n") // Salto de línea
            sala.extras.forEach { extra ->
                when (extra) {
                    "WiFi" -> builder.append("📶 ")
                    "Proyector" -> builder.append("📽️ ")
                    "Pizarra" -> builder.append("🖍️ ")
                }
            }
        }
        button.text = builder.toString()
    }


    suspend fun guardarDistribucion(
        empresaId: Int,
        pisoNombre: String,
        imagen: ByteArray?,
        container: ViewGroup,
        pisoDao: PisoDao,
        salaDao: SalaDao
    ) {
        val salasGuardadas = mutableListOf<Salas>()

        for (i in 0 until container.childCount) {
            val view = container.getChildAt(i)
            if (view is Button) {
                val sala = view.tag as? Salas ?: continue
                salasGuardadas.add(
                    Salas(
                        id = sala.id,
                        nombre = sala.nombre,
                        tamaño = sala.tamaño,
                        pisoId = -1,
                        x = view.x,
                        y = view.y,
                        ancho = view.width.toFloat(),
                        alto = view.height.toFloat(),
                        extras = sala.extras
                    )
                )
            }
        }

        if (salasGuardadas.isEmpty()) {
            withContext(Dispatchers.Main) {
                Toast.makeText(container.context, "Debes colocar al menos una sala antes de guardar", Toast.LENGTH_SHORT).show()
            }
            return
        }

        if (pisoNombre.isBlank()) {
            withContext(Dispatchers.Main) {
                Snackbar.make(container, "Por favor, asigna un nombre al piso", Snackbar.LENGTH_LONG)
                    .setAction("Editar") {
                        // Abrir diálogo de edición si quieres
                    }
                    .show()
            }
            return
        }

        // Aquí cambiamos a contexto IO porque accedemos a base de datos
        withContext(Dispatchers.IO) {
            val pisoExistente = pisoDao.obtenerPisoPorNombreYEmpresa(pisoNombre, empresaId)

            val pisoId = if (pisoExistente == null) {
                val nuevoPiso = Piso(
                    nombre = pisoNombre,
                    empresaId = empresaId,
                    imagen = imagen // ← CAMBIADO
                )
                pisoDao.insertarPiso(nuevoPiso).toInt()
            } else {
                val pisoActualizado = pisoExistente.copy(
                    nombre = pisoNombre,
                    empresaId = empresaId,
                    imagen = imagen // ← CAMBIADO
                )
                pisoDao.actualizarPiso(pisoActualizado)
                pisoExistente.id
            }

            salaDao.eliminarPorPiso(pisoId)

            salasGuardadas.forEach { sala ->
                sala.pisoId = pisoId
                salaDao.insertar(sala)
            }
        }
        withContext(Dispatchers.Main) {
            Toast.makeText(container.context, "Distribución guardada correctamente", Toast.LENGTH_SHORT).show()
        }
    }


    private fun eliminarPisoPorNombre(nombrePiso: String, empresaId: Int?) {
        lifecycleScope.launch {
            val piso = repository.pisoDao.obtenerPisoPorNombreYEmpresa(nombrePiso, empresaId)

            if (piso != null) {
                repository.pisoDao.eliminarPiso(piso)

                val prefNumeroPiso = getSharedPreferences("mi_preferencia", MODE_PRIVATE)
                val pisoActual = prefNumeroPiso.getString("numero_piso", null)

                if (pisoActual == nombrePiso) {
                    val pisosRestantes = repository.pisoDao.obtenerTodosLosPisos().firstOrNull() ?: emptyList()
                    val nombresPisos = pisosRestantes.map { it.nombre }

                    prefNumeroPiso.edit().apply {
                        if (nombresPisos.isNotEmpty()) {
                            putString("numero_piso", nombresPisos.first())
                        } else {
                            remove("numero_piso")
                        }
                        apply()
                    }
                }

                withContext(Dispatchers.Main) {
                    Toast.makeText(this@Activity_creacion, "Piso eliminado", Toast.LENGTH_SHORT).show()
                    // Aquí volvemos a mostrar el diálogo actualizado
                    mostrarDialogoEliminarPisos()
                }
            } else {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@Activity_creacion, "Piso no encontrado", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }


    private fun mostrarDialogoEliminarPisos() {
        lifecycleScope.launch {
            val pisos = repository.pisoDao.obtenerTodosLosPisos().firstOrNull() ?: emptyList()
            val nombresPisos = pisos.map { it.nombre }.toMutableList()

            if (nombresPisos.isEmpty()) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@Activity_creacion, "No hay pisos guardados.", Toast.LENGTH_SHORT).show()
                }
                return@launch
            }

            nombresPisos.sortWith { piso1, piso2 ->
                val key1 = naturalOrderKey(piso1)
                val key2 = naturalOrderKey(piso2)
                compareNaturalKeys(key1, key2)
            }

            withContext(Dispatchers.Main) {
                val pisosArray = nombresPisos.toTypedArray()

                val dialogPrincipal = AlertDialog.Builder(this@Activity_creacion)
                    .setTitle("Eliminar piso")
                    .setItems(pisosArray) { dialogInterface, which ->
                        val pisoSeleccionado = pisosArray[which]

                        val dialogConfirmacion = AlertDialog.Builder(this@Activity_creacion)
                            .setTitle("¿Eliminar '$pisoSeleccionado'?")
                            .setMessage("Esta acción eliminará el piso y todas sus salas.")
                            .setPositiveButton("Eliminar") { dialogConfirm, _ ->
                                val empresaId = empresaId
                                eliminarPisoPorNombre(pisoSeleccionado, empresaId)
                                dialogConfirm.dismiss()
                                dialogInterface.dismiss()  // Cerramos el diálogo principal también
                            }
                            .setNegativeButton("Cancelar", null)
                            .create()

                        dialogConfirmacion.setOnShowListener {
                            dialogConfirmacion.window?.setBackgroundDrawableResource(R.drawable.dialog_background)
                            dialogConfirmacion.getButton(AlertDialog.BUTTON_NEGATIVE)?.setTextColor(Color.RED)
                        }
                        dialogConfirmacion.show()
                    }
                    .setNegativeButton("Cancelar", null)
                    .create()

                dialogPrincipal.setOnShowListener {
                    dialogPrincipal.window?.setBackgroundDrawableResource(R.drawable.dialog_background)
                    dialogPrincipal.getButton(AlertDialog.BUTTON_NEGATIVE)?.setTextColor(Color.RED)
                }
                dialogPrincipal.show()
            }
        }
    }



    private var imagen: ByteArray? = null // esto lo usarás al guardar en la base de datos

    private val getImage = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            imagen = contentResolver.openInputStream(uri)?.use { it.readBytes() } // convierte a byte array

            Glide.with(this)
                .load(uri)
                .fitCenter()
                .into(findViewById(R.id.image_fondo))
        }
    }

    private fun EditText.autoAdvanceTo(next: EditText?) {
        this.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                if (s?.length == 2) {
                    if (next != null) {
                        next.requestFocus()
                    } else {
                        // Ocultar teclado si es el último campo
                        val imm = context.getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
                        imm.hideSoftInputFromWindow(windowToken, 0)
                        clearFocus()
                    }
                }
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })
    }

    inner class MovableTouchListener : View.OnTouchListener {
        private var dX = 0f
        private var dY = 0f
        private var startX = 0f
        private var startY = 0f
        private val CLICK_THRESHOLD = 10  // Distancia máxima para considerar click

        override fun onTouch(view: View, event: MotionEvent): Boolean {
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    dX = view.x - event.rawX
                    dY = view.y - event.rawY
                    startX = event.rawX
                    startY = event.rawY
                }
                MotionEvent.ACTION_MOVE -> {
                    view.animate()
                        .x(event.rawX + dX)
                        .y(event.rawY + dY)
                        .setDuration(0)
                        .start()
                }
                MotionEvent.ACTION_UP -> {
                    val endX = event.rawX
                    val endY = event.rawY

                    val deltaX = Math.abs(endX - startX)
                    val deltaY = Math.abs(endY - startY)

                    if (deltaX < CLICK_THRESHOLD && deltaY < CLICK_THRESHOLD) {
                        // Se considera click
                        if (view is Button) {
                            showButtonOptions(view)
                        }
                    }
                }
            }
            return true
        }
    }
}

