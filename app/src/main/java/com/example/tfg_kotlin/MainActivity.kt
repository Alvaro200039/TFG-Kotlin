package com.example.tfg_kotlin

import android.content.Intent
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
import android.view.ViewGroup
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
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.graphics.toColorInt
import com.bumptech.glide.Glide
import com.google.android.material.snackbar.Snackbar
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import androidx.core.content.edit
import androidx.core.view.children
import java.util.UUID

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

class MainActivity : AppCompatActivity() {

    private lateinit var container: ConstraintLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
//Solo usar en modo desarrollo
        val sharedPref = getSharedPreferences("DistribucionSalas", MODE_PRIVATE)
        sharedPref.edit() { clear() }  // Borra todos los datos guardados

        val toolbar = findViewById<Toolbar>(R.id.my_toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayShowTitleEnabled(false)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        val sharedPreferences = getSharedPreferences("mi_preferencia", MODE_PRIVATE)
        val pisoGuardado = sharedPreferences.getString("pisos", "Piso nº ")

        val titleView = findViewById<TextView>(R.id.toolbar_title)
        titleView.text = pisoGuardado
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
            R.id.action_add_image -> {
                openGallery()
                true
            }
            R.id.action_add -> {
                addMovableButton()
                true
            }

            R.id.action_save -> {
                guardarDistribucion()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
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
        val pisoGuardado = sharedPreferences.getString("pisos", "Piso nº ")

        // Crear un EditText con el valor actual
        val editText = EditText(this).apply {
            setText(pisoGuardado)
            setSelection(text.length) // Poner cursor al final para editar fácilmente
        }

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(50, 40, 50, 10)
            addView(editText)
        }

        val builder = AlertDialog.Builder(this)
            .setTitle("Edite el piso al que pertenece")
            .setView(layout)
            .setPositiveButton("Guardar") { dialog, _ ->
                val maxTitleLength = 11
                val nuevoTitulo = editText.text.toString().trim().take(maxTitleLength)
                if (nuevoTitulo.isNotBlank()) {
                    // Guardar el nuevo título en SharedPreferences
                    sharedPreferences.edit().putString("numero_piso", nuevoTitulo).apply()

                    // Actualizar el TextView del título
                    findViewById<TextView>(R.id.toolbar_title).text = nuevoTitulo

                    // Guardar el nuevo piso en conjunto de pisos en "DistribucionSalas"
                    val distPrefs = getSharedPreferences("DistribucionSalas", MODE_PRIVATE)
                    val pisosActuales = distPrefs.getStringSet("pisos", mutableSetOf())?.toMutableSet() ?: mutableSetOf()
                    pisosActuales.add(nuevoTitulo)
                    distPrefs.edit().putStringSet("pisos", pisosActuales).apply()
                }
            }
            .setNegativeButton("Cancelar", null)
            .create()

        builder.setOnShowListener {
            builder.window?.setBackgroundDrawableResource(R.drawable.dialog_background)
        }
        builder.show()
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
            max = 1000
            progress = salaButton.width
        }

        val anchoValue = TextView(this).apply {
            text = "Ancho: ${anchoSeekBar.progress}px"
        }

        val altoSeekBar = SeekBar(this).apply {
            max = 1000
            progress = salaButton.height
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
                actualizarTamañoSalaGuardada(sala.nombre, nuevoAncho, nuevoAlto)
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
        }

        dialog.show()
    }

    private fun actualizarTamañoSalaGuardada(nombreSala: String, nuevoAncho: Int, nuevoAlto: Int) {
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

        val tamaños = listOf("Grande", "Pequeño")
        val spinnerTamaño = Spinner(this).apply {
            adapter = ArrayAdapter(this@MainActivity, android.R.layout.simple_spinner_dropdown_item, tamaños)
            setSelection(tamaños.indexOf(sala.tamaño).takeIf { it >= 0 } ?: 0)
        }

        val checkWifi = CheckBox(this).apply {
            text = "WiFi"
            isChecked = sala.opcionesExtra.contains("WiFi")
        }
        val checkProyector = CheckBox(this).apply {
            text = "Proyector"
            isChecked = sala.opcionesExtra.contains("Proyector")
        }
        val checkPizarra = CheckBox(this).apply {
            text = "Pizarra"
            isChecked = sala.opcionesExtra.contains("Pizarra")
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
            val negativeButton = dialog.getButton(AlertDialog.BUTTON_NEGATIVE)

            positiveButton.setBackgroundColor("#008000".toColorInt())
            positiveButton.setTextColor("#FFFFFF".toColorInt())
            negativeButton.setBackgroundColor("#B22222".toColorInt())
            negativeButton.setTextColor("#FFFFFF".toColorInt())

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


    private fun obtenerTodosLosBotones(): List<Button> {
        return container.children.filterIsInstance<Button>().toList()
    }

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
                        piso= sala.piso
                    )
                )
            }
        }

        val sharedPref = getSharedPreferences("DistribucionSalas", MODE_PRIVATE)
        val gson = Gson()

        val fondoUriString = fondoUri?.toString()

        sharedPref.edit().apply {
            val sharedPrefsTitulo = getSharedPreferences("mi_preferencia", MODE_PRIVATE)
            val nombrePiso = sharedPrefsTitulo.getString("numero_piso", "Piso 1") ?: "Piso 1"

            putString("salas_$nombrePiso", gson.toJson(salasGuardadas))
            // Guardar el URI del fondo específico para ese piso
            fondoUriString?.let { putString("fondo_uri_$nombrePiso", it) }
            putBoolean("distribucion_guardada", true)
            apply()
        }

        Snackbar.make(container, "Distribución guardada", Snackbar.LENGTH_SHORT).show()
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