package com.example.tfg_kotlin

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.content.res.Resources
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.text.Editable
import android.text.InputFilter
import android.text.TextWatcher
import android.view.Gravity
import android.view.Menu
import android.view.MenuItem
import android.view.MotionEvent
import android.view.View
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
import java.util.Locale

data class Sala(
    var nombre: String,
    var tamaño: String = "Pequeño",
    var opcionesExtra: List<String> = emptyList(),
    var piso: String
)

data class SalaGuardada(
    val nombre: String,
    val x: Float,
    val y: Float,
    val tamaño: String,
    var ancho: Float = 0f,
    var alto: Float = 0f,
    val extras: List<String>,
    var piso: String
)

class Activity_creacion : AppCompatActivity() {

    private lateinit var container: ConstraintLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_creacion)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.creacion)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val toolbar = findViewById<Toolbar>(R.id.my_toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayShowTitleEnabled(false)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        // Enlace botones
        val btnHoras = findViewById<LinearLayout>(R.id.btn_horas)
        val btnPlano = findViewById<LinearLayout>(R.id.btn_plano)
        val btnSala = findViewById<LinearLayout>(R.id.btn_sala)
        val btnPisos = findViewById<LinearLayout>(R.id.btn_pisos)

        // Listener para btnHoras (antes action_add_hour)
        btnHoras.setOnClickListener {
            mostrarDialogoFranjas()
        }

        btnPisos.setOnClickListener {
            mostrarDialogoEliminarPisos()
        }

        // Listener para btnPlano (antes action_add_image)
        btnPlano.setOnClickListener {
            openGallery()
        }

        // Listener para btnSala (antes action_add)
        btnSala.setOnClickListener {
            addMovableButton()
        }


//Solo usar en modo desarrollo
      //  val sharedPref = getSharedPreferences("DistribucionSalas", MODE_PRIVATE)
       // sharedPref.edit() { clear() }  // Borra todos los datos guardados



        val sharedPreferences = getSharedPreferences("mi_preferencia", MODE_PRIVATE)
        sharedPreferences.edit().putString("numero_piso", "Piso nº").apply()

        val titleView = findViewById<TextView>(R.id.toolbar_title)
// Mostrar siempre texto fijo en la toolbar
        titleView.text = "Piso nº"
        titleView.setOnClickListener {
            showChangeTitleDialog()
        }


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
                guardarDistribucion()
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

    @SuppressLint("SuspiciousIndentation")
    private fun mostrarDialogoFranjas() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_franjas_horas, null)
        val editHoraInicio = dialogView.findViewById<EditText>(R.id.etHoraInicio)
        val editMinutoInicio = dialogView.findViewById<EditText>(R.id.etMinInicio)
        val editHoraFin = dialogView.findViewById<EditText>(R.id.etHoraFin)
        val editMinutoFin = dialogView.findViewById<EditText>(R.id.etMinFin)
        val botonAgregar = dialogView.findViewById<Button>(R.id.btnAddFranja)
        val layoutFranjas = dialogView.findViewById<LinearLayout>(R.id.layoutFranjas)


        editHoraInicio.autoAdvanceTo(editMinutoInicio)
        editMinutoInicio.autoAdvanceTo(editHoraFin)
        editHoraFin.autoAdvanceTo(editMinutoFin)
        editMinutoFin.autoAdvanceTo(null)

        val prefs = getSharedPreferences("mi_preferencia", MODE_PRIVATE)
        val listaFranjas = prefs.getStringSet("franjas_horarias", mutableSetOf())!!.toMutableSet()

        fun actualizarListaFranjasUI() {
            layoutFranjas.removeAllViews()
            val ordenadas = listaFranjas.sorted()
            for (franja in ordenadas) {
                val franjaView = LinearLayout(dialogView.context).apply {
                    orientation = LinearLayout.HORIZONTAL

                    val text = TextView(dialogView.context).apply {
                        this.text = franja
                        textSize = 16f
                        setPadding(8, 4, 8, 4)
                    }

                    val botonEliminar = Button(dialogView.context).apply {
                        this.text = "❌"
                        textSize = 14f
                        setBackgroundColor(Color.TRANSPARENT)
                        setPadding(16, 0, 16, 0)
                        setOnClickListener {
                            listaFranjas.remove(franja)
                            prefs.edit() { putStringSet("franjas_horarias", listaFranjas) }
                            actualizarListaFranjasUI()
                        }
                    }

                    addView(text)
                    addView(botonEliminar)
                }
                layoutFranjas.addView(franjaView)
            }
        }

        botonAgregar.setOnClickListener {
            val hInicio = editHoraInicio.text.toString()
            val mInicio = editMinutoInicio.text.toString()
            val hFin = editHoraFin.text.toString()
            val mFin = editMinutoFin.text.toString()

            if (hInicio.isBlank() || mInicio.isBlank() || hFin.isBlank() || mFin.isBlank()) {
                Toast.makeText(this, "Todos los campos deben estar completos", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val hi = hInicio.toIntOrNull()
            val mi = mInicio.toIntOrNull()
            val hf = hFin.toIntOrNull()
            val mf = mFin.toIntOrNull()

            if (hi == null || mi == null || hf == null || mf == null) {
                Toast.makeText(this, "Todos los valores deben ser números válidos", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (hi !in 0..23 || hf !in 0..23) {
                Toast.makeText(this, "Las horas deben estar entre 0 y 23", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (mi !in 0..59 || mf !in 0..59) {
                Toast.makeText(this, "Los minutos deben estar entre 0 y 59", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val minutosInicio = hi * 60 + mi
            val minutosFin = hf * 60 + mf

            if (minutosFin <= minutosInicio) {
                Toast.makeText(this, "La hora de fin debe ser posterior a la de inicio", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val horaInicioStr = "%02d:%02d".format(hi, mi)
            val horaFinStr = "%02d:%02d".format(hf, mf)
            val nuevaFranja = "$horaInicioStr - $horaFinStr"

            if (listaFranjas.contains(nuevaFranja)) {
                Toast.makeText(this, "Franja ya añadida", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            listaFranjas.add(nuevaFranja)
            prefs.edit() { putStringSet("franjas_horarias", listaFranjas) }
            actualizarListaFranjasUI()

            // Limpiar campos
            editHoraInicio.text.clear()
            editMinutoInicio.text.clear()
            editHoraFin.text.clear()
            editMinutoFin.text.clear()
        }

        actualizarListaFranjasUI()

        val dialog = AlertDialog.Builder(this)
            .setTitle("Añadir franjas horarias")
            .setView(dialogView)
            .setNegativeButton("Cerrar", null)
            .create()
            dialog.window?.setBackgroundDrawableResource(R.drawable.dialog_background)
            dialog.show()
            val btnCerrar = dialog.getButton(AlertDialog.BUTTON_NEGATIVE)
            btnCerrar.setTextColor(Color.BLACK)
    }


    fun entrarModoEmpleado(view: View) {
        val intent = Intent(this, Activity_empleados::class.java)
        startActivity(intent)
    }

    private fun openGallery() {
        getImage.launch("image/*")
    }

    private fun addMovableButton() {
        val sharedPrefsTitulo = getSharedPreferences("mi_preferencia", MODE_PRIVATE)
        val nombrePiso = sharedPrefsTitulo.getString("numero_piso", "Piso 1") ?: "Piso 1"
        val sala = Sala(nombre = "Sala", piso = nombrePiso)
        val button = Button(this).apply {
            text = "Sala"
            // Crear un fondo
            background = android.graphics.drawable.GradientDrawable().apply {
                setColor("#BEBEBE".toColorInt()) // Color de fondo
                cornerRadius = 50f // Radio de las esquinas en píxeles
            }
            setPadding(50, 20, 50, 20) // (izquierda, arriba, derecha, abajo)
            setOnTouchListener(MovableTouchListener())
            setOnClickListener {
                showButtonOptions(this)
            }
            tag = sala
        }
        container.addView(button)
        val layoutParams = button.layoutParams as ConstraintLayout.LayoutParams
        layoutParams.topMargin = 100
        layoutParams.leftMargin = 100
        button.layoutParams = layoutParams
    }

    // Función para cambiar el título de la Toolbar con un EditText
    private fun showChangeTitleDialog() {
        val sharedPreferences = getSharedPreferences("mi_preferencia", MODE_PRIVATE)
        val pisoGuardado = sharedPreferences.getString("numero_piso", "Piso nº") ?: "Piso nº"

        val editText = EditText(this).apply {
            setText(pisoGuardado)
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
                if (nuevoTitulo.isEmpty() || nuevoTitulo.equals("Piso nº", ignoreCase = true)|| nuevoTitulo.equals("Piso nº ", ignoreCase = true)) {
                    Toast.makeText(this, "Por favor, cambie el nombre del piso antes de guardar", Toast.LENGTH_SHORT).show()
                } else {
                    sharedPreferences.edit().putString("numero_piso", nuevoTitulo).apply()
                    findViewById<TextView>(R.id.toolbar_title).text = nuevoTitulo

                    val distPrefs = getSharedPreferences("DistribucionSalas", MODE_PRIVATE)
                    val pisosActuales = distPrefs.getStringSet("pisos", mutableSetOf())?.toMutableSet() ?: mutableSetOf()
                    pisosActuales.add(nuevoTitulo)
                    distPrefs.edit().putStringSet("pisos", pisosActuales).apply()
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
                    val sala = button.tag as? Sala
                    if (sala != null) {
                        mostrarDialogoCambiarTamaño(button, sala)
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


    private fun mostrarDialogoCambiarTamaño(salaButton: Button, sala: Sala) {
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
        val sharedPref = getSharedPreferences("DistribucionSalas", MODE_PRIVATE)
        val gson = Gson()
        val json = sharedPref.getString("salas", "[]")
        val lista: MutableList<SalaGuardada> = gson.fromJson(json, object : TypeToken<MutableList<SalaGuardada>>() {}.type)

        // Buscar la sala y actualizar su tamaño solo si ha cambiado
        val sala = lista.find { it.nombre == nombreSala }
        if (sala != null && (sala.ancho != nuevoAncho.toFloat() || sala.alto != nuevoAlto.toFloat())) {
            sala.ancho = nuevoAncho.toFloat()
            sala.alto = nuevoAlto.toFloat()

            // Guardar los cambios si hubo una actualización
            sharedPref.edit() { putString("salas", gson.toJson(lista)) }
        }
    }

    private fun showEditButtonDialog(button: Button) {
        val sala = button.tag as? Sala ?: Sala(nombre = button.text.toString(), piso = "Piso_default")

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

        val adapter = ArrayAdapter(

            this,
            android.R.layout.simple_spinner_dropdown_item,
            tamanios
        ).also {
            it.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }

        val spinnerTamaño = Spinner(this).apply {
            this.adapter = adapter
            setSelection(tamanios.indexOf(sala.tamaño).takeIf { it >= 0 } ?: 0)
            background = ContextCompat.getDrawable(context, R.drawable.spinner_dropdown_background)
            this.setPopupBackgroundDrawable(ContextCompat.getDrawable(context, R.drawable.spinner_dropdown_background))
        }

        val checkWifi = CheckBox(this).apply {
            text = "WiFi"
            isChecked = sala.opcionesExtra.contains("WiFi")
            buttonTintList = ColorStateList.valueOf(Color.GRAY)
        }
        val checkProyector = CheckBox(this).apply {
            text = "Proyector"
            isChecked = sala.opcionesExtra.contains("Proyector")
            buttonTintList = ColorStateList.valueOf(Color.GRAY)
        }
        val checkPizarra = CheckBox(this).apply {
            text = "Pizarra"
            isChecked = sala.opcionesExtra.contains("Pizarra")
            buttonTintList = ColorStateList.valueOf(Color.GRAY)
        }

        layout.apply {
            addView(editTextNombre)
            addView(charCountTextView)
            addView(spinnerTamaño)
            addView(checkWifi)
            addView(checkProyector)
            addView(checkPizarra)
        }

        val builder = AlertDialog.Builder(this)
        builder.setTitle("Editar sala")
        builder.setView(layout)
        builder.setPositiveButton("Guardar", null) // Controlamos el click nosotros
        builder.setNegativeButton("Cancelar", null)

        val dialog = builder.create()
        dialog.window?.setBackgroundDrawableResource(R.drawable.dialog_background)

        dialog.setOnShowListener {
            val positiveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE)

            dialog.getButton(AlertDialog.BUTTON_POSITIVE)?.setTextColor(Color.BLACK)
            dialog.getButton(AlertDialog.BUTTON_NEGATIVE)?.setTextColor(Color.RED)

            positiveButton.setOnClickListener {
                val nuevoNombre = editTextNombre.text.toString().trim()

                if (nuevoNombre.isEmpty()) {
                    editTextNombre.error = "El nombre no puede estar vacío"
                    return@setOnClickListener
                }

                // Obtener nombres ya usados en otros botones
                val nombreRepetido = container.children
                    .filterIsInstance<Button>()
                    .filter { it != button }
                    .mapNotNull { (it.tag as? Sala)?.nombre }
                    .any { it.equals(nuevoNombre, ignoreCase = true) }

                if (nombreRepetido) {
                    editTextNombre.error = "Ese nombre ya está en uso"
                    return@setOnClickListener
                }

                // Guardar cambios
                sala.nombre = nuevoNombre
                sala.tamaño = spinnerTamaño.selectedItem as String

                val opciones = mutableListOf<String>()
                if (checkWifi.isChecked) opciones.add("WiFi")
                if (checkProyector.isChecked) opciones.add("Proyector")
                if (checkPizarra.isChecked) opciones.add("Pizarra")
                sala.opcionesExtra = opciones

                actualizarBotonConSala(button, sala)
                button.tag = sala

                dialog.dismiss()
            }
        }
        dialog.show()
    }


   // private fun obtenerTodosLosBotones(): List<Button> {
     //   return container.children.filterIsInstance<Button>().toList()
    //}

    private fun actualizarBotonConSala(button: Button, sala: Sala) {
        val builder = StringBuilder()
        builder.append(sala.nombre)

        if (sala.opcionesExtra.isNotEmpty()) {
            builder.append("\n") // Salto de línea
            sala.opcionesExtra.forEach { extra ->
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


    private fun guardarDistribucion() {
        val salasGuardadas = mutableListOf<SalaGuardada>()

        for (i in 0 until container.childCount) {
            val view = container.getChildAt(i)
            if (view is Button) {
                val sala = view.tag as? Sala ?: continue
                salasGuardadas.add(
                    SalaGuardada(
                        nombre = sala.nombre,
                        x = view.x,
                        y = view.y,
                        ancho = view.width.toFloat(),
                        alto = view.height.toFloat(),
                        tamaño = sala.tamaño,
                        extras = sala.opcionesExtra,
                        piso = sala.piso
                    )
                )
            }
        }

        val sharedPreferences = getSharedPreferences("mi_preferencia", MODE_PRIVATE)
        val nombrePiso = sharedPreferences.getString("numero_piso", "Piso nº") ?: "Piso nº"

        if (nombrePiso == "Piso nº") {
            Snackbar.make(container, "Por favor, asigne un nombre al piso", Snackbar.LENGTH_LONG)
                .setAction("Editar") {
                    showChangeTitleDialog()
                }.show()
            return
        }

        val sharedPref = getSharedPreferences("DistribucionSalas", MODE_PRIVATE)
        val gson = Gson()
        val fondoUriString = fondoUri?.toString()

        sharedPref.edit().apply {
            putString("salas_$nombrePiso", gson.toJson(salasGuardadas))
            fondoUriString?.let { putString("fondo_uri_$nombrePiso", it) }
            putBoolean("distribucion_guardada", true)
            apply()
        }

        Snackbar.make(container, "Distribución guardada", Snackbar.LENGTH_SHORT).show()
    }

    fun naturalOrderKey(s: String): List<Any> {
        val regex = Regex("""(\d+|\D+)""")
        return regex.findAll(s.lowercase(Locale.ROOT)).map {
            val part = it.value
            part.toIntOrNull() ?: part
        }.toList()
    }

    fun compareNaturalKeys(a: List<Any>, b: List<Any>): Int {
        val minSize = minOf(a.size, b.size)
        for (i in 0 until minSize) {
            val comp = when {
                a[i] is Int && b[i] is Int -> (a[i] as Int).compareTo(b[i] as Int)
                a[i] is String && b[i] is String -> (a[i] as String).compareTo(b[i] as String)
                a[i] is Int && b[i] is String -> -1 // números antes que letras
                a[i] is String && b[i] is Int -> 1  // letras después que números
                else -> 0
            }
            if (comp != 0) return comp
        }
        // Si todos los elementos iguales hasta ahora, la lista más corta es menor
        return a.size.compareTo(b.size)
    }

    fun eliminarPiso(nombrePiso: String) {
        val sharedPrefDistribucion = getSharedPreferences("DistribucionSalas", MODE_PRIVATE)

        // Actualizar la distribución y el set de pisos dentro del mismo bloque edit para evitar errores de concurrencia
        sharedPrefDistribucion.edit().apply {
            // Elimina distribución y fondo
            remove("salas_$nombrePiso")
            remove("fondo_uri_$nombrePiso")

            // Actualiza el set de pisos guardados
            val pisosSet = sharedPrefDistribucion.getStringSet("pisos", mutableSetOf())?.toMutableSet() ?: mutableSetOf()
            pisosSet.remove(nombrePiso)
            putStringSet("pisos", pisosSet)

            apply()
        }

        // Ahora actualiza "mi_preferencia" si es el piso que está guardado ahí
        val sharedPrefNumeroPiso = getSharedPreferences("mi_preferencia", MODE_PRIVATE)
        val pisoGuardado = sharedPrefNumeroPiso.getString("numero_piso", null)

        if (pisoGuardado == nombrePiso) {
            val pisosRestantes = sharedPrefDistribucion.getStringSet("pisos", emptySet())
            val nuevoPiso = pisosRestantes?.firstOrNull()

            sharedPrefNumeroPiso.edit().apply {
                if (nuevoPiso != null) {
                    putString("numero_piso", nuevoPiso)
                } else {
                    remove("numero_piso")
                }
                apply()
            }
        }
    }

    private fun mostrarDialogoEliminarPisos() {
        val sharedPref = getSharedPreferences("DistribucionSalas", MODE_PRIVATE)
        val todasClaves = sharedPref.all.keys

        val pisos = todasClaves
            .filter { it.startsWith("salas_") }
            .map { it.removePrefix("salas_") }
            .toMutableList()

        if (pisos.isEmpty()) {
            Toast.makeText(this, "No hay pisos guardados.", Toast.LENGTH_SHORT).show()
            return
        }

        // Ordenar de forma natural, respetando números dentro de las cadenas
        pisos.sortWith { piso1, piso2 ->
            val key1 = naturalOrderKey(piso1)
            val key2 = naturalOrderKey(piso2)
            compareNaturalKeys(key1, key2)
        }

        val pisosArray = pisos.toTypedArray()

        AlertDialog.Builder(this)
            .setTitle("Eliminar piso")
            .setItems(pisosArray) { _, which ->
                val pisoSeleccionado = pisosArray[which]

                AlertDialog.Builder(this)
                    .setTitle("¿Eliminar '$pisoSeleccionado'?")
                    .setMessage("Esta acción eliminará la distribución y el fondo del piso.")
                    .setPositiveButton("Eliminar") { _, _ ->
                        eliminarPiso(pisoSeleccionado)
                        Toast.makeText(this, "Piso eliminado", Toast.LENGTH_SHORT).show()
                    }
                    .setNegativeButton("Cancelar", null)
                    .show()
            }
            .setNegativeButton("Cancelar", null)
            .show()
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