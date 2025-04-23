package com.example.tfg_kotlin

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.text.Editable
import android.text.InputFilter
import android.text.TextWatcher
import android.view.Menu
import android.view.MenuItem
import android.view.MotionEvent
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.graphics.toColorInt
import com.google.gson.Gson

data class Sala(
    var nombre: String,
    var tamaño: String = "Pequeño",
    var opcionesExtra: List<String> = emptyList()
)

data class SalaGuardada(
    val nombre: String,
    val x: Float,
    val y: Float,
    val tamaño: String,
    val extras: List<String>
)

class MainActivity : AppCompatActivity() {

    private lateinit var container: ConstraintLayout
    private val PICK_IMAGE_REQUEST = 1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val toolbar = findViewById<Toolbar>(R.id.my_toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

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
        // Este código está obsoleto, pero sigue funcionando en versiones antiguas de Android.
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        startActivityForResult(intent, PICK_IMAGE_REQUEST)
    }

    private fun addMovableButton() {

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
        }

        container.addView(button)
        val layoutParams = button.layoutParams as ConstraintLayout.LayoutParams
        layoutParams.topMargin = 100
        layoutParams.leftMargin = 100
        button.layoutParams = layoutParams
    }

    private fun showButtonOptions(button: Button) {
        val editText = EditText(this)
        val options = arrayOf("Editar", "Eliminar")

        val builder = AlertDialog.Builder(this)
        builder.setTitle("Opciones de la sala")

        builder.setItems(options) { dialog, which ->
            when (which) {
                0 -> showEditButtonDialog(button) // Editar texto
                1 -> container.removeView(button) // Eliminar botón
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

    private fun showEditButtonDialog(button: Button) {
        val sala = button.tag as? Sala ?: Sala(nombre = button.text.toString())

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(50, 40, 50, 10)
        }

        val editTextNombre = EditText(this).apply {
            hint = "Nuevo nombre"
            setText(sala.nombre)
            // Limitar el número de caracteres (por ejemplo, 10 caracteres)
            val maxLength = 20
            val filter = InputFilter.LengthFilter(maxLength)
            filters = arrayOf(filter)
        }

        // Añadir un TextView para mostrar el contador de caracteres
        val charCountTextView = TextView(this).apply {
            text = "0/${20}"  // Inicialmente, 0 caracteres de 10
            setTextColor("#000000".toColorInt())
        }

        // Actualizar el contador cada vez que el texto cambia
        editTextNombre.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(charSequence: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(charSequence: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(editable: Editable?) {
                val currentLength = editable?.length ?: 0
                charCountTextView.text = "$currentLength/20"  // Actualiza el contador
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

        val builder = androidx.appcompat.app.AlertDialog.Builder(this)
        builder.setTitle("Editar sala")
        builder.setView(layout)
        builder.setPositiveButton("Guardar") { dialog, _ ->
            sala.nombre = editTextNombre.text.toString()
            sala.tamaño = spinnerTamaño.selectedItem as String

            val opciones = mutableListOf<String>()
            if (checkWifi.isChecked) opciones.add("WiFi")
            if (checkProyector.isChecked) opciones.add("Proyector")
            if (checkPizarra.isChecked) opciones.add("Pizarra")
            sala.opcionesExtra = opciones

            // Actualizar botón visualmente
            actualizarBotonConSala(button, sala)

            button.tag = sala
        }
        builder.setNegativeButton("Cancelar", null)

        // Cambiar fondo del AlertDialog
        val dialog = builder.create()

        // Cambiar fondo
        dialog.window?.setBackgroundDrawableResource(R.drawable.dialog_background) // Aquí se aplica el fondo

        dialog.setOnShowListener {
            // Opcionalmente puedes personalizar los botones
            val positiveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE)
            val negativeButton = dialog.getButton(AlertDialog.BUTTON_NEGATIVE)

            // Cambiar color de fondo de los botones
            positiveButton.setBackgroundColor("#008000".toColorInt())  // Botón verde
            negativeButton.setBackgroundColor("#B22222".toColorInt())  // Botón rojo

            // Cambiar color de texto de los botones
            positiveButton.setTextColor("#FFFFFF".toColorInt())  // Texto en blanco para el botón "Guardar"
            negativeButton.setTextColor("#FFFFFF".toColorInt())  // Texto en blanco para el botón "Cancelar"
        }

        dialog.show()
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

        val params = button.layoutParams as ConstraintLayout.LayoutParams
        if (sala.tamaño == "Grande") {
            button.textSize = 22f
            params.width = ConstraintLayout.LayoutParams.WRAP_CONTENT
            params.height = ConstraintLayout.LayoutParams.WRAP_CONTENT
        } else {
            button.textSize = 14f
            params.width = ConstraintLayout.LayoutParams.WRAP_CONTENT
            params.height = ConstraintLayout.LayoutParams.WRAP_CONTENT
        }
        button.layoutParams = params
    }
    private var fondoUri: Uri? = null
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == Activity.RESULT_OK) {
            val selectedImage: Uri? = data?.data
            selectedImage?.let {
                fondoUri = it
                val bitmap = MediaStore.Images.Media.getBitmap(contentResolver, it)
                container.background = BitmapDrawable(resources, bitmap)
            }
        }
    }

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
                        tamaño = sala.tamaño,
                        extras = sala.opcionesExtra
                    )
                )
            }
        }

        val editor = getSharedPreferences("DistribucionSalas", MODE_PRIVATE).edit()

        // Guarda la URI del fondo si la tienes
        fondoUri?.let {
            editor.putString("fondo_uri", it.toString())
        }

        val gson = Gson()
        val jsonSalas = gson.toJson(salasGuardadas)
        editor.putString("salas", jsonSalas)

        editor.apply()

        Toast.makeText(this, "Distribución guardada", Toast.LENGTH_SHORT).show()
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