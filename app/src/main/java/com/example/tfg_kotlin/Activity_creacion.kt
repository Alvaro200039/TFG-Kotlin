package com.example.tfg_kotlin

import android.content.res.ColorStateList
import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Bundle
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
import androidx.core.graphics.drawable.toBitmapOrNull
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.graphics.toColorInt
import com.bumptech.glide.Glide
import com.google.android.material.snackbar.Snackbar
import androidx.core.view.children
import androidx.lifecycle.lifecycleScope
import com.example.tfg_kotlin.Utils.naturalOrderKey
import com.example.tfg_kotlin.Utils.compareNaturalKeys
import com.example.tfg_kotlin.BBDD_Global.Entities.Piso
import com.example.tfg_kotlin.BBDD_Global.Entities.Salas
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import android.util.Base64
import com.google.firebase.Firebase
import com.google.firebase.firestore.firestore


class Activity_creacion : AppCompatActivity() {

        private lateinit var container: ConstraintLayout
        private var pisoActual: Piso? = null
        private var empresaCif: String = ""
        private lateinit var firestore: FirebaseFirestore
        private lateinit var auth: FirebaseAuth
        private var imagen: ByteArray? = null

        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)
            setContentView(R.layout.activity_creacion)

            enableEdgeToEdge()
            ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.creacion)) { v, insets ->
                val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
                v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
                insets
            }

            // Inicializar Firebase
            firestore = FirebaseFirestore.getInstance()
            auth = FirebaseAuth.getInstance()

            // Obtener CIF desde el Intent
            empresaCif = intent.getStringExtra("cifUsuario") ?: ""
            if (empresaCif.isEmpty()) {
                Toast.makeText(this, "Error: CIF no recibido", Toast.LENGTH_SHORT).show()
                finish()
                return
            }

            // Toolbar
            val toolbar = findViewById<Toolbar>(R.id.my_toolbar)
            setSupportActionBar(toolbar)
            supportActionBar?.setDisplayShowTitleEnabled(false)
            supportActionBar?.setDisplayHomeAsUpEnabled(true)

            val titleView = findViewById<TextView>(R.id.toolbar_title)
            titleView.text = "Piso nº" // provisional
            titleView.setOnClickListener {
                showChangeTitleDialog()
            }

            // Obtener último piso desde Firestore
            firestore.collection("empresas")
                .document(empresaCif)
                .collection("pisos")
                .orderBy("nombre", Query.Direction.ASCENDING) // o por fecha de creación si tienes ese campo
                .get()
                .addOnSuccessListener { querySnapshot ->
                    val pisos = querySnapshot.toObjects(Piso::class.java)
                    if (pisos.isNotEmpty()) {
                        pisoActual = pisos.last()
                        titleView.text = pisoActual?.nombre ?: "Sin nombre"
                    } else {
                        Toast.makeText(this, "No hay pisos creados", Toast.LENGTH_SHORT).show()
                    }
                }
                .addOnFailureListener {
                    Toast.makeText(this, "Error cargando pisos", Toast.LENGTH_SHORT).show()
                }

            // Botones
            val btnHoras = findViewById<LinearLayout>(R.id.btn_horas)
            val btnPlano = findViewById<LinearLayout>(R.id.btn_plano)
            val btnSala = findViewById<LinearLayout>(R.id.btn_sala)
            val btnPisos = findViewById<LinearLayout>(R.id.btn_pisos)
            val layoutFranjas = findViewById<LinearLayout>(R.id.layoutFranjas)

            btnHoras.setOnClickListener {cargarFranjas(layoutFranjas)}
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
                val toolbarTitle = findViewById<TextView>(R.id.toolbar_title).text.toString()

                lifecycleScope.launch {
                    try {
                        // Aquí extraes la imagen de fondo desde el fondo del layout, si es necesario
                        val fondoLayout = findViewById<View>(R.id.container)
                        val fondoBitmap = fondoLayout.background?.toBitmapOrNull()
                        val fondoBytes = fondoBitmap?.let { bitmap ->
                            val stream = ByteArrayOutputStream()
                            bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
                            stream.toByteArray()
                        }
                        guardarDistribucion(
                            pisoNombre = toolbarTitle,
                            imagen = fondoBytes,
                            container = findViewById(R.id.container)
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


    private fun cargarFranjas(layoutFranjas: LinearLayout) {
            lifecycleScope.launch {
                try {
                    val snapshot = firestore.collection("empresas")
                        .document(empresaCif)
                        .collection("franjasHorarias")
                        .get()
                        .await()

                    val franjas = snapshot.documents.mapNotNull { it.id }
                    actualizarListaFranjas(franjas, layoutFranjas)
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(this@Activity_creacion, "Error cargando franjas: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }

        private fun actualizarListaFranjas(franjas: List<String>, layoutFranjas: LinearLayout) {
            layoutFranjas.removeAllViews()
            for (hora in franjas.sorted()) {
                val franjaView = LinearLayout(layoutFranjas.context).apply {
                    orientation = LinearLayout.HORIZONTAL

                    val textView = TextView(layoutFranjas.context).apply {
                        text = hora
                        textSize = 16f
                        setPadding(8, 4, 8, 4)
                    }

                    val botonEliminar = Button(layoutFranjas.context).apply {
                        text = "❌"
                        textSize = 14f
                        setBackgroundColor(Color.TRANSPARENT)
                        setPadding(16, 0, 16, 0)
                        setOnClickListener {
                            lifecycleScope.launch {
                                try {
                                    firestore.collection("empresas")
                                        .document(empresaCif)
                                        .collection("franjasHorarias")
                                        .document(hora)
                                        .delete()
                                        .await()

                                    cargarFranjas(layoutFranjas) // Aquí ya funciona
                                } catch (e: Exception) {
                                    withContext(Dispatchers.Main) {
                                        Toast.makeText(this@Activity_creacion, "Error eliminando franja: ${e.message}", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            }
                        }
                    }

                    addView(textView)
                    addView(botonEliminar)
                }
                layoutFranjas.addView(franjaView)
            }
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
                setText(piso?.nombre ?: "Piso nº ")
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
                        pisoActual = if (piso == null) {
                            Piso(
                                id = null,                      // Todavía no tiene ID Firestore
                                nombre = nuevoTitulo,
                                empresaCif = empresaCif,  // Usa la variable que tengas con el CIF actual
                                imagenUrl = null                // O alguna URL por defecto si tienes
                            )
                        } else {
                            piso.copy(nombre = nuevoTitulo)
                        }


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
            val empresaCif = pisoActual?.empresaCif ?: return
            val pisoId = pisoActual?.id ?: return

            lifecycleScope.launch {
                try {
                    val salasRef = firestore.collection("empresas")
                        .document(empresaCif)
                        .collection("pisos")
                        .document(pisoId)
                        .collection("salas")

                    // Buscar sala por nombre
                    val salaQuery = salasRef
                        .whereEqualTo("nombre", nombreSala)
                        .get()
                        .await()

                    if (!salaQuery.isEmpty) {
                        val doc = salaQuery.documents.first()
                        val sala = doc.toObject(Salas::class.java)

                        if (sala != null && (sala.ancho != nuevoAncho.toFloat() || sala.alto != nuevoAlto.toFloat())) {
                            val salaActualizada = sala.copy(
                                ancho = nuevoAncho.toFloat(),
                                alto = nuevoAlto.toFloat()
                            )

                            // Actualizar el documento en Firestore
                            salasRef.document(doc.id).set(salaActualizada).await()
                        }
                    }
                } catch (e: Exception) {
                    Log.e("Firestore", "Error al actualizar tamaño de sala: ${e.message}", e)
                }
            }
        }



        private fun showEditButtonDialog(button: Button) {
            val sala = button.tag as? Salas
            if (sala == null) {
                Toast.makeText(this, "No se encontró la sala asociada al botón", Toast.LENGTH_SHORT).show()
                return
            }

            val empresaCif = pisoActual?.empresaCif ?: return
            val pisoId = pisoActual?.id ?: return

            val layout = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(50, 40, 50, 10)
            }

            val editTextNombre = EditText(this).apply {
                hint = "Nuevo nombre"
                setText(sala.nombre)
                filters = arrayOf(InputFilter.LengthFilter(20))
            }

            val charCountTextView = TextView(this).apply {
                text = "${sala.nombre.length}/20"
                setTextColor("#000000".toColorInt())
            }

            editTextNombre.addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
                override fun afterTextChanged(s: Editable?) {
                    charCountTextView.text = "${s?.length ?: 0}/20"
                }
            })

            val tamanios = listOf("Pequeño", "Grande")
            val spinnerTamanio = Spinner(this).apply {
                adapter = ArrayAdapter(this@Activity_creacion, android.R.layout.simple_spinner_dropdown_item, tamanios)
                setSelection(tamanios.indexOf(sala.tamaño).takeIf { it >= 0 } ?: 0)
                background = ContextCompat.getDrawable(context, R.drawable.spinner_dropdown_background)
                setPopupBackgroundDrawable(ContextCompat.getDrawable(context, R.drawable.spinner_dropdown_background))
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { topMargin = 16; bottomMargin = 16 }
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
                .setPositiveButton("Guardar", null)
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

                    // Crear la sala editada
                    val salaEditada = sala.copy(
                        nombre = nuevoNombre,
                        tamaño = spinnerTamanio.selectedItem?.toString().toString(),
                        extras = mutableListOf<String>().apply {
                            if (checkWifi.isChecked) add("WiFi")
                            if (checkProyector.isChecked) add("Proyector")
                            if (checkPizarra.isChecked) add("Pizarra")
                        }
                    )

                    // Guardar en Firestore
                    lifecycleScope.launch {
                        try {
                            val docId = sala.id ?: return@launch
                            val salaRef = firestore.collection("empresas")
                                .document(empresaCif)
                                .collection("pisos")
                                .document(pisoId)
                                .collection("salas")
                                .document(docId)

                            salaRef.set(salaEditada).await()

                            actualizarBotonConSala(button, salaEditada)
                            button.tag = salaEditada
                            Toast.makeText(this@Activity_creacion, "Sala actualizada", Toast.LENGTH_SHORT).show()
                            dialog.dismiss()
                        } catch (e: Exception) {
                            Toast.makeText(this@Activity_creacion, "Error al actualizar sala", Toast.LENGTH_SHORT).show()
                            Log.e("Firestore", "Error: ${e.message}", e)
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


        suspend fun guardarDistribucion(
            pisoNombre: String,
            imagen: ByteArray?,
            container: ViewGroup
        ) {
            val db = FirebaseFirestore.getInstance()
            val auth = FirebaseAuth.getInstance()
            val empresaCif = auth.currentUser?.uid ?: "" // O usa el cif real del usuario autenticado

            val salasGuardadas = mutableListOf<Map<String, Any>>()

            for (i in 0 until container.childCount) {
                val view = container.getChildAt(i)
                if (view is Button) {
                    val sala = view.tag as? Salas ?: continue
                    val salaMap = mapOf(
                        "id" to sala.id,
                        "nombre" to sala.nombre,
                        "tamaño" to sala.tamaño,
                        "x" to view.x,
                        "y" to view.y,
                        "ancho" to view.width.toFloat(),
                        "alto" to view.height.toFloat(),
                        "extras" to sala.extras
                    )
                    salasGuardadas.add(salaMap as Map<String, Any>)
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
                    Snackbar.make(container, "Por favor, asigna un nombre al piso", Snackbar.LENGTH_LONG).show()
                }
                return
            }

            val encodedImage = imagen?.let { Base64.encodeToString(it, Base64.DEFAULT) }

            val pisoRef = db.collection("empresas")
                .document(empresaCif)
                .collection("pisos")
                .document(pisoNombre)

            val pisoData = mapOf(
                "nombre" to pisoNombre,
                "empresaCif" to empresaCif,
                "imagenBase64" to encodedImage,
                "salas" to salasGuardadas
            )

            try {
                pisoRef.set(pisoData).await()
                withContext(Dispatchers.Main) {
                    Toast.makeText(container.context, "Distribución guardada en Firestore", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(container.context, "Error al guardar: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }




    private fun eliminarPisoPorNombre(nombrePiso: String, empresaCif: String) {
        val db = Firebase.firestore
        val empresaDoc = db.collection("empresas").document(empresaCif)
        val pisosCollection = empresaDoc.collection("pisos")

        lifecycleScope.launch {
            try {
                // Buscar piso por nombre
                val querySnapshot = pisosCollection.whereEqualTo("nombre", nombrePiso).get().await()
                val pisoDoc = querySnapshot.documents.firstOrNull()

                if (pisoDoc != null) {
                    val pisoId = pisoDoc.id

                    // Eliminar salas asociadas a ese piso
                    pisosCollection.document(pisoId).collection("salas").get().await().documents.forEach {
                        it.reference.delete()
                    }

                    // Eliminar el piso
                    pisosCollection.document(pisoId).delete().await()

                    // Actualizar preferencia local
                    val prefNumeroPiso = getSharedPreferences("mi_preferencia", MODE_PRIVATE)
                    val pisoActual = prefNumeroPiso.getString("numero_piso", null)

                    if (pisoActual == nombrePiso) {
                        val pisosRestantes = pisosCollection.get().await().documents
                        val nombresPisos = pisosRestantes.mapNotNull { it.getString("nombre") }

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
                        mostrarDialogoEliminarPisos()
                    }

                } else {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(this@Activity_creacion, "Piso no encontrado", Toast.LENGTH_SHORT).show()
                    }
                }

            } catch (e: Exception) {
                Log.e("Firestore", "Error al eliminar piso: ${e.message}", e)
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@Activity_creacion, "Error eliminando el piso", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }


    private fun mostrarDialogoEliminarPisos() {
        val db = Firebase.firestore
        val empresaDoc = db.collection("empresas").document(empresaCif)
        val pisosCollection = empresaDoc.collection("pisos")

        lifecycleScope.launch {
            try {
                val snapshot = pisosCollection.get().await()
                val nombresPisos = snapshot.documents.mapNotNull { it.getString("nombre") }.toMutableList()

                if (nombresPisos.isEmpty()) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(this@Activity_creacion, "No hay pisos guardados.", Toast.LENGTH_SHORT).show()
                    }
                    return@launch
                }

                // Orden natural como antes
                nombresPisos.sortWith { p1, p2 ->
                    val k1 = naturalOrderKey(p1)
                    val k2 = naturalOrderKey(p2)
                    compareNaturalKeys(k1, k2)
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
                                    eliminarPisoPorNombre(pisoSeleccionado, empresaCif)
                                    dialogConfirm.dismiss()
                                    dialogInterface.dismiss()
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

            } catch (e: Exception) {
                Log.e("Firestore", "Error al obtener pisos: ${e.message}", e)
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@Activity_creacion, "Error obteniendo pisos", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

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

