package com.example.tfg_kotlin

import android.content.Context
import android.content.res.ColorStateList
import android.content.res.Resources
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.text.Editable
import android.text.InputFilter
import android.text.TextWatcher
import android.view.Gravity
import android.view.LayoutInflater
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
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import androidx.core.content.edit
import androidx.core.view.children
import androidx.lifecycle.lifecycleScope
import com.example.tfg_kotlin.Utils.naturalOrderKey
import com.example.tfg_kotlin.Utils.compareNaturalKeys
import com.example.tfg_kotlin.dao.PisoDao
import com.example.tfg_kotlin.dao.SalaDao
import com.example.tfg_kotlin.database.AppDatabase
import com.example.tfg_kotlin.entities.FranjaHoraria
import com.example.tfg_kotlin.entities.Piso
import com.example.tfg_kotlin.entities.Salas
import com.example.tfg_kotlin.repository.AppRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.last
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


class Activity_creacion : AppCompatActivity() {

    private lateinit var container: ConstraintLayout
    private lateinit var repository: AppRepository
    private var pisoActual: Piso? = null

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
                    val pisoNombre = findViewById<TextView>(R.id.toolbar_title).text.toString()
                    val prefs = getSharedPreferences("usuario_prefs", MODE_PRIVATE)
                    val empresaId = prefs.getInt("empresa_id", -1)

                    guardarDistribucion(
                        empresaId = empresaId,
                        pisoNombre = pisoNombre,
                        fondoUri = fondoUri,
                        container = container,
                        pisoDao = repository.pisoDao,
                        salaDao = repository.salaDao
                    )
                }
                true
            }
            else -> super.onOptionsItemSelected(item)
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
                        val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                        imm.hideSoftInputFromWindow(windowToken, 0)
                        clearFocus()
                    }
                }
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })
    }

private fun mostrarDialogoFranjas() {
    val dialogView = layoutInflater.inflate(R.layout.dialog_franjas_horas, null)

    val layoutFranjas = dialogView.findViewById<LinearLayout>(R.id.layoutFranjas)
    val botonAgregar = dialogView.findViewById<Button>(R.id.btnAddFranja)

    val editHoraInicio = dialogView.findViewById<EditText>(R.id.etHoraInicio)
    val editMinutoInicio = dialogView.findViewById<EditText>(R.id.etMinInicio)
    val editHoraFin = dialogView.findViewById<EditText>(R.id.etHoraFin)
    val editMinutoFin = dialogView.findViewById<EditText>(R.id.etMinFin)

    // Funciones para avanzar foco etc...

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

                var textview = TextView(dialogView.context).apply {
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
        repository.franjaHorariaDao.getTodasFranjas().collectLatest { franjas ->
            actualizarListaFranjasUI(franjas)
        }
    }

    botonAgregar.setOnClickListener {
        val hInicio = editHoraInicio.text.toString()
        val mInicio = editMinutoInicio.text.toString()
        val hFin = editHoraFin.text.toString()
        val mFin = editMinutoFin.text.toString()

        // Validaciones como antes, luego:

        val horaInicioStr = "%02d:%02d".format(hInicio.toInt(), mInicio.toInt())
        val horaFinStr = "%02d:%02d".format(hFin.toInt(), mFin.toInt())
        val nuevaFranja = "$horaInicioStr - $horaFinStr"

        // Insertar nueva franja en DB
        lifecycleScope.launch {
            repository.franjaHorariaDao.insertarFranja(FranjaHoraria(nuevaFranja))
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

    private fun addMovableButton() {
        val piso = pisoActual
        if (piso == null) {
            Toast.makeText(this, "No hay piso seleccionado", Toast.LENGTH_SHORT).show()
            return
        }

        val sala = Salas(
            nombre = "Sala",
            tamaño = "pequeña", // o "grande", según quieras
            pisoId = piso.id,
            x = 100f,
            y = 100f,
            ancho = 300f,
            alto = 200f,
            extras = emptyList()
        )


        val button = Button(this).apply {
            text = sala.nombre
            background = android.graphics.drawable.GradientDrawable().apply {
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

        // Guardar sala en Room
        lifecycleScope.launch {
            repository.salaDao.insertar(sala)
        }
    }


    private fun showChangeTitleDialog() {
        if (pisoActual == null) {
            Toast.makeText(this, "No hay piso cargado", Toast.LENGTH_SHORT).show()
            return
        }

        val editText = EditText(this).apply {
            setText(pisoActual!!.nombre)
            setSelection(text.length)
        }

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(50, 40, 50, 10)
            addView(editText)
        }

        val dialog = AlertDialog.Builder(this)
            .setTitle("Edite el piso al que pertenece")
            .setView(layout)
            .setPositiveButton("Guardar") { _, _ ->
                val maxTitleLength = 11
                val nuevoTitulo = editText.text.toString().trim().take(maxTitleLength)

                if (nuevoTitulo.isEmpty() || nuevoTitulo.equals("Piso nº", ignoreCase = true) || nuevoTitulo.equals("Piso nº ", ignoreCase = true)) {
                    Toast.makeText(this, "Por favor, cambie el nombre del piso antes de guardar", Toast.LENGTH_SHORT).show()
                } else {
                    val pisoEditado = pisoActual!!.copy(nombre = nuevoTitulo)

                    lifecycleScope.launch {
                        repository.pisoDao.insertarPiso(pisoEditado)
                        pisoActual = pisoEditado
                        withContext(Dispatchers.Main) {
                            findViewById<TextView>(R.id.toolbar_title).text = nuevoTitulo
                            Toast.makeText(this@Activity_creacion, "Nombre del piso actualizado", Toast.LENGTH_SHORT).show()
                        }
                    }

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

        val tamanios = listOf("Grande", "Pequeño")

        var adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_dropdown_item,
            tamanios
        ).also {
            it.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }

        val spinnerTamanio = Spinner(this).apply {
            adapter = adapter
            setSelection(tamanios.indexOf(sala.tamaño).takeIf { it >= 0 } ?: 0)
            background = ContextCompat.getDrawable(context, R.drawable.spinner_dropdown_background)
            setPopupBackgroundDrawable(ContextCompat.getDrawable(context, R.drawable.spinner_dropdown_background))
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
                    tamaño = spinnerTamanio.selectedItem as String,
                    extras = mutableListOf<String>().apply {
                        if (checkWifi.isChecked) add("WiFi")
                        if (checkProyector.isChecked) add("Proyector")
                        if (checkPizarra.isChecked) add("Pizarra")
                    }
                )

                // Guardar en Room en coroutine
                lifecycleScope.launch {
                    repository.salaDao.actualizar(salaEditada)

                    // Actualizar UI en hilo principal
                    runOnUiThread {
                        actualizarBotonConSala(button, salaEditada)
                        button.tag = salaEditada
                        Toast.makeText(this@Activity_creacion, "Sala actualizada", Toast.LENGTH_SHORT).show()
                        dialog.dismiss()
                    }
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

    private var fondoUri: Uri? = null


    suspend fun guardarDistribucion(
        empresaId: Int,
        pisoNombre: String,
        fondoUri: Uri?,
        container: ViewGroup,
        pisoDao: PisoDao,
        salaDao: SalaDao
    ) {
        val salasGuardadas = mutableListOf<Salas>()

        // Recoger la información de cada vista-sala del contenedor
        for (i in 0 until container.childCount) {
            val view = container.getChildAt(i)
            if (view is Button) {
                val sala = view.tag as? Salas ?: continue
                salasGuardadas.add(
                    Salas(
                        id = sala.id,  // Puede ser 0 si es nueva
                        nombre = sala.nombre,
                        tamaño = sala.tamaño,
                        pisoId = 0,  // Se asignará tras guardar piso
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

        val uriFondoString = fondoUri?.toString() ?: ""

        // Verificar si ya existe un piso con ese nombre y empresa
        val pisoExistente = pisoDao.obtenerPisoPorNombre(pisoNombre)

        val pisoId = if (pisoExistente == null) {
            val nuevoPiso = Piso(
                nombre = pisoNombre,
                uriFondo = uriFondoString,
                empresaId = empresaId
            )
            pisoDao.insertarPiso(nuevoPiso).toInt()
        } else {
            val pisoActualizado = pisoExistente.copy(
                uriFondo = uriFondoString,
                empresaId = empresaId
            )
            pisoDao.actualizarPiso(pisoActualizado)
            pisoExistente.id
        }

        // Eliminar salas anteriores asociadas al piso
        salaDao.eliminarPorPiso(pisoId)

        // Insertar nuevas salas con el pisoId actualizado
        salasGuardadas.forEach { sala ->
            sala.pisoId = pisoId
            salaDao.insertar(sala)
        }

        withContext(Dispatchers.Main) {
            Toast.makeText(container.context, "Distribución guardada correctamente", Toast.LENGTH_SHORT).show()
        }
    }

    private fun eliminarPisoPorNombre(nombrePiso: String) {
        lifecycleScope.launch {
            // 1. Obtener el piso por nombre
            val piso = repository.pisoDao.obtenerPisoPorNombre(nombrePiso)

            if (piso != null) {
                // 2. Eliminar piso (salas se eliminarán automáticamente por ON DELETE CASCADE)
                repository.pisoDao.eliminarPiso(piso)

                // 3. Actualizar SharedPreferences "mi_preferencia" para piso seleccionado
                val prefNumeroPiso = getSharedPreferences("mi_preferencia", MODE_PRIVATE)
                val pisoActual = prefNumeroPiso.getString("numero_piso", null)

                if (pisoActual == nombrePiso) {
                    // Obtener pisos restantes
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
                    // Actualiza UI si usas LiveData/StateFlow
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

            // Orden natural
            nombresPisos.sortWith { piso1, piso2 ->
                val key1 = naturalOrderKey(piso1)
                val key2 = naturalOrderKey(piso2)
                compareNaturalKeys(key1, key2)
            }

            withContext(Dispatchers.Main) {
                val pisosArray = nombresPisos.toTypedArray()

                AlertDialog.Builder(this@Activity_creacion)
                    .setTitle("Eliminar piso")
                    .setItems(pisosArray) { _, which ->
                        val pisoSeleccionado = pisosArray[which]

                        val dialog = AlertDialog.Builder(this@Activity_creacion)
                            .setTitle("¿Eliminar '$pisoSeleccionado'?")
                            .setMessage("Esta acción eliminará el piso y todas sus salas.")
                            .setPositiveButton("Eliminar") { _, _ ->
                                eliminarPisoPorNombre(pisoSeleccionado)
                            }
                            .setNegativeButton("Cancelar", null)
                            .create()

                        dialog.setOnShowListener {
                            dialog.window?.setBackgroundDrawableResource(R.drawable.dialog_background)
                            dialog.getButton(AlertDialog.BUTTON_NEGATIVE)?.setTextColor(Color.RED)
                        }
                        dialog.show()
                    }
                    .setNegativeButton("Cancelar", null)
                    .create()
                    .apply {
                        setOnShowListener {
                            window?.setBackgroundDrawableResource(R.drawable.dialog_background)
                            getButton(AlertDialog.BUTTON_NEGATIVE)?.setTextColor(Color.RED)
                        }
                    }
                    .show()
            }
        }
    }




    // Aquí es donde gestionas la selección de imagen de fondo
    private val getImage = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            fondoUri = uri // Guarda la URI seleccionada
            Glide.with(this)
                .load(uri)
                .fitCenter()
                .into(findViewById(R.id.image_fondo))
        }
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

