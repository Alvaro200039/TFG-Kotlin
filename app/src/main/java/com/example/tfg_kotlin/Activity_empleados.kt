package com.example.tfg_kotlin

import android.app.DatePickerDialog
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.MenuItem
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.snackbar.Snackbar
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.util.Calendar
import androidx.core.content.edit
import androidx.core.net.toUri
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import com.example.tfg_kotlin.Utils.naturalOrderKey
import com.example.tfg_kotlin.Utils.compareNaturalKeys
import com.example.tfg_kotlin.activity_menu_creador
import com.example.tfg_kotlin.database.AppDatabase
import com.example.tfg_kotlin.entities.Reserva
import com.example.tfg_kotlin.entities.Salas
import com.example.tfg_kotlin.repository.AppRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


class Activity_empleados : AppCompatActivity() {
    private lateinit var repository: AppRepository
    private lateinit var container: ConstraintLayout
    private var fechaSeleccionada: String = ""
    private var horaSeleccionada: String = ""
    private lateinit var textViewFecha: TextView
    private lateinit var spinnerPisos: Spinner
    private var pisoSeleccionado: String = ""
    private lateinit var textViewHora: TextView
    private var snackbarActivo: Snackbar? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val db = AppDatabase.getDatabase(applicationContext)
        repository = AppRepository(
            db.usuarioDao(),
            db.salaDao(),
            db.reservaDao(),
            franjaHorariaDao = TODO(),
        )
        enableEdgeToEdge()
        setContentView(R.layout.activity_empleados)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.empleados)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val btnReservas = findViewById<LinearLayout>(R.id.btn_reservas)
        btnReservas.setOnClickListener {
            mostrarDialogoReservas()
        }

        val btnFranja = findViewById<LinearLayout>(R.id.btn_franja)
        btnFranja.setOnClickListener {
            if (fechaSeleccionada.isEmpty()) {
                snackbarActivo?.dismiss()  // Cierra el anterior si sigue visible
                snackbarActivo = Snackbar.make(container, "Primero selecciona una fecha", Snackbar.LENGTH_LONG)
                    .setAction("Seleccionar fecha") {
                        mostrarDialogoFecha()
                    }

                snackbarActivo?.show()
            } else {
                mostrarDialogoHoras()
            }
        }

        container = findViewById(R.id.contentLayout)

        val toolbar: Toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = ""

        val sharedPreferences = getSharedPreferences("DistribucionSalas", MODE_PRIVATE)
        val pisosGuardadosSet = sharedPreferences.getStringSet("pisos", setOf()) ?: setOf()
        val pisosGuardados = pisosGuardadosSet.toList().sorted()

        spinnerPisos = Spinner(this).apply {
            adapter = ArrayAdapter(this@Activity_empleados, android.R.layout.simple_spinner_dropdown_item, pisosGuardados)
            this.setPopupBackgroundDrawable(ContextCompat.getDrawable(context, R.drawable.spinner_dropdown_background))
            onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                    pisoSeleccionado = pisosGuardados[position]
                    cargarSalas(pisoSeleccionado)
                    cargarImagenFondo(pisoSeleccionado)
                    verificarDisponibilidad("$fechaSeleccionada $horaSeleccionada")
                }

                override fun onNothingSelected(parent: AdapterView<*>) {}
            }
            layoutParams = Toolbar.LayoutParams(
                Toolbar.LayoutParams.WRAP_CONTENT,
                Toolbar.LayoutParams.WRAP_CONTENT
            ).apply {
                gravity = Gravity.START
                marginStart = 0
            }
        }
        toolbar.addView(spinnerPisos)



        val botonSeleccionarFechaHora = ImageButton(this).apply {
            setImageResource(R.drawable.time)
            setBackgroundColor(Color.TRANSPARENT)
            setOnClickListener { mostrarDialogoFecha() }
            layoutParams = Toolbar.LayoutParams(
                Toolbar.LayoutParams.WRAP_CONTENT,
                Toolbar.LayoutParams.WRAP_CONTENT
            ).apply {
                gravity = Gravity.END
                marginEnd = 20
                marginStart= 20
            }
        }
        toolbar.addView(botonSeleccionarFechaHora)

        val contenedorFechaHora = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = Toolbar.LayoutParams(
                Toolbar.LayoutParams.WRAP_CONTENT,
                Toolbar.LayoutParams.WRAP_CONTENT
            ).apply {
                gravity = Gravity.END or Gravity.CENTER_VERTICAL
                marginEnd = 8
            }
        }

        textViewFecha = TextView(this).apply {
            text = "Fecha:"
            setTextColor(Color.BLACK)
            textSize = 16f
        }

        textViewHora = TextView(this).apply {
            text = "Hora:"
            setTextColor(Color.BLACK)
            textSize = 16f
        }

        contenedorFechaHora.addView(textViewFecha)
        contenedorFechaHora.addView(textViewHora)
        toolbar.addView(contenedorFechaHora)


        // Cargar piso automáticamente si se ha guardado o se ha pasado por intent
        val pisoDesdeIntent = intent.getStringExtra("nombre_piso")
        val pisoInicial = pisoDesdeIntent ?: sharedPreferences.getString("numero_piso", null)

        if (pisoInicial != null && pisosGuardados.contains(pisoInicial)) {
            pisoSeleccionado = pisoInicial
            cargarSalas(pisoSeleccionado)
            cargarImagenFondo(pisoSeleccionado)
            verificarDisponibilidad("$fechaSeleccionada $horaSeleccionada")
            spinnerPisos.setSelection(pisosGuardados.indexOf(pisoSeleccionado))
        }
    }

    override fun onResume() {
        super.onResume()
        actualizarSpinnerPisos()
    }

    private fun actualizarSpinnerPisos() {
        val sharedPref = getSharedPreferences("DistribucionSalas", MODE_PRIVATE)
        val pisosGuardadosSet = sharedPref.getStringSet("pisos", setOf()) ?: setOf()
        val pisosGuardados = pisosGuardadosSet.toList()
            .sortedWith { a, b -> compareNaturalKeys(naturalOrderKey(a), naturalOrderKey(b)) }

        // Actualizar el adaptador del spinner
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, pisosGuardados)
        spinnerPisos.adapter = adapter

        // Si tienes un piso seleccionado guardado, intenta seleccionarlo
        val pisoInicial = intent.getStringExtra("nombre_piso") ?: sharedPref.getString("numero_piso", null)
        if (pisoInicial != null && pisosGuardados.contains(pisoInicial)) {
            spinnerPisos.setSelection(pisosGuardados.indexOf(pisoInicial))
        } else if (pisosGuardados.isNotEmpty()) {
            pisoSeleccionado = pisosGuardados[0]
            cargarSalas(pisoSeleccionado)
            cargarImagenFondo(pisoSeleccionado)
            verificarDisponibilidad("$fechaSeleccionada $horaSeleccionada")
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {

            android.R.id.home -> {
                onBackPressedDispatcher.onBackPressed()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun mostrarDialogoFecha() {
        snackbarActivo?.dismiss()
        val calendario = Calendar.getInstance()

        val datePickerDialog = DatePickerDialog(
            this,
            android.R.style.Theme_Material_Dialog_MinWidth,
            { _, year, month, dayOfMonth ->
                fechaSeleccionada = String.format("%02d/%02d/%d", dayOfMonth, month + 1, year)
                textViewFecha.text = "Fecha: $fechaSeleccionada"
                mostrarDialogoHoras()
            },
            calendario.get(Calendar.YEAR),
            calendario.get(Calendar.MONTH),
            calendario.get(Calendar.DAY_OF_MONTH)
        )

        datePickerDialog.datePicker.minDate = calendario.timeInMillis

        datePickerDialog.setOnShowListener {
            datePickerDialog.window?.setBackgroundDrawableResource(R.drawable.datepicker_background)
        }

        datePickerDialog.show()
    }


    private fun mostrarDialogoReservas() {
        limpiarReservasPasadas()

        lifecycleScope.launch {
            val reservas = repository.getAllReservas().toMutableList()

            if (reservas.isEmpty()) {
                Toast.makeText(this@Activity_empleados, "No tienes reservas activas.", Toast.LENGTH_SHORT).show()
                return@launch
            }

            val reservasPorPiso = reservas.groupBy { it.piso }
            val dialogView = layoutInflater.inflate(R.layout.dialog_reservas, null)
            val contenedor = dialogView.findViewById<LinearLayout>(R.id.contenedor_reservas)

            lateinit var dialog: AlertDialog

            for ((piso, lista) in reservasPorPiso) {
                val pisoText = TextView(this@Activity_empleados).apply {
                    text = piso
                    textSize = 18f
                    setPadding(0, 16, 0, 8)
                    setTextColor(Color.BLACK)
                    setTypeface(null, Typeface.BOLD)
                }
                contenedor.addView(pisoText)

                lista.forEach { reserva ->
                    val reservaText = TextView(this@Activity_empleados).apply {
                        text = "- ${reserva.nombreSala}  ${reserva.fechaHora}"
                        setPadding(16, 4, 0, 4)
                        setTextColor(Color.DKGRAY)
                        setOnClickListener {
                            val confirmDialog = AlertDialog.Builder(this@Activity_empleados)
                                .setTitle("¿Cancelar reserva?")
                                .setMessage("¿Deseas cancelar la reserva de '${reserva.nombreSala}' el ${reserva.fechaHora}?")
                                .setPositiveButton("Sí") { _, _ ->
                                    lifecycleScope.launch {
                                        repository.eliminarReserva(reserva)

                                        dialog.dismiss()
                                        mostrarDialogoReservas() // Recargar reservas
                                    }
                                }
                                .setNegativeButton("No", null)
                                .create()

                            confirmDialog.setOnShowListener {
                                confirmDialog.getButton(AlertDialog.BUTTON_POSITIVE)?.setTextColor(Color.BLACK)
                                confirmDialog.getButton(AlertDialog.BUTTON_NEGATIVE)?.setTextColor(Color.RED)
                            }

                            confirmDialog.window?.setBackgroundDrawableResource(R.drawable.dialog_background)
                            confirmDialog.show()
                        }
                    }
                    contenedor.addView(reservaText)
                }

                val divider = View(this@Activity_empleados).apply {
                    setBackgroundColor(Color.LTGRAY)
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        2
                    ).apply { setMargins(0, 16, 0, 16) }
                }
                contenedor.addView(divider)
            }

            dialog = AlertDialog.Builder(this@Activity_empleados)
                .setTitle("Tus reservas activas")
                .setView(dialogView)
                .setPositiveButton("Cerrar", null)
                .create()

            dialog.show()
            dialog.window?.setBackgroundDrawableResource(R.drawable.dialog_background)
            dialog.getButton(AlertDialog.BUTTON_POSITIVE)?.setTextColor(Color.BLACK)
        }
    }

    private fun limpiarReservasPasadas() {
        val formato = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
        val ahora = formato.format(Date()) // Lo convertimos a String porque Room usa fechaHora como String

        lifecycleScope.launch {
            repository.limpiarReservasAntiguas(ahora)
        }
    }


    private fun mostrarDialogoHoras() {
        if (fechaSeleccionada.isEmpty()) {
            Toast.makeText(this, "Primero selecciona una fecha.", Toast.LENGTH_SHORT).show()
            return
        }

        lifecycleScope.launch {
            // Obtén la lista desde Room
            val franjas = repository.getFranjasHorarias()
            val franjasOriginales = franjas.map { it.hora }


            if (franjasOriginales.isEmpty()) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@Activity_empleados, "No hay franjas horarias disponibles. Crea algunas primero.", Toast.LENGTH_SHORT).show()
                }
                return@launch
            }

            val calendario = Calendar.getInstance()
            val hoy = String.format("%02d/%02d/%d",
                calendario.get(Calendar.DAY_OF_MONTH),
                calendario.get(Calendar.MONTH) + 1,
                calendario.get(Calendar.YEAR)
            )

            val horaActual = String.format("%02d:%02d",
                calendario.get(Calendar.HOUR_OF_DAY),
                calendario.get(Calendar.MINUTE)
            )

            val horasDisponibles = if (fechaSeleccionada == hoy) {
                franjasOriginales.filter { franja ->
                    franja >= horaActual
                }
            } else {
                franjasOriginales
            }

            if (horasDisponibles.isEmpty()) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@Activity_empleados, "No hay franjas horarias disponibles para el día seleccionado.", Toast.LENGTH_SHORT).show()
                }
                return@launch
            }

            withContext(Dispatchers.Main) {
                val horasArray = horasDisponibles.toTypedArray()

                val dialog = AlertDialog.Builder(this@Activity_empleados)
                    .setTitle("Selecciona una franja horaria para el $fechaSeleccionada")
                    .setItems(horasArray) { _, which ->
                        horaSeleccionada = horasArray[which]
                        textViewHora.text = "Hora: $horaSeleccionada"
                        verificarDisponibilidad("$fechaSeleccionada $horaSeleccionada")
                    }
                    .setNegativeButton("Cerrar", null)
                    .create()

                dialog.show()
                dialog.window?.setBackgroundDrawableResource(R.drawable.dialog_background)

                val btnCerrar = dialog.getButton(AlertDialog.BUTTON_NEGATIVE)
                btnCerrar?.setTextColor(Color.BLACK)
            }
        }
    }


    private fun cargarSalas(nombrePiso: String) {
        container.removeAllViews()  // Limpiar contenedor antes de cargar

        lifecycleScope.launch {
            try {
                // Obtener salas del repositorio (Room)
                val salas = repository.getSalasPorPiso(nombrePiso)

                withContext(Dispatchers.Main) {
                    for (sala in salas) {
                        // Crear botón para cada sala
                        val salaButton = Button(this@Activity_empleados).apply {
                            text = formatearTextoSala(sala)
                            tag = sala

                            setOnClickListener {
                                if (fechaSeleccionada.isEmpty() || horaSeleccionada.isEmpty()) {
                                    snackbarActivo?.dismiss()
                                    snackbarActivo = Snackbar.make(container, "Primero selecciona una fecha", Snackbar.LENGTH_LONG)
                                        .setAction("Seleccionar fecha") {
                                            mostrarDialogoFecha()
                                        }
                                    snackbarActivo?.show()
                                } else {
                                    mostrarDialogoDetallesSala(sala)
                                }
                            }

                            background = GradientDrawable().apply {
                                setColor(Color.GREEN)
                                cornerRadius = 50f
                            }

                            layoutParams = ConstraintLayout.LayoutParams(
                                sala.ancho.toInt(),
                                sala.alto.toInt()
                            ).apply {
                                leftToLeft = ConstraintLayout.LayoutParams.PARENT_ID
                                topToTop = ConstraintLayout.LayoutParams.PARENT_ID
                                setMargins(sala.x.toInt(), sala.y.toInt(), 0, 0)
                            }
                        }

                        container.addView(salaButton)
                    }

                    // Verificar disponibilidad si fecha y hora están seleccionadas
                    if (fechaSeleccionada.isNotEmpty() && horaSeleccionada.isNotEmpty()) {
                        verificarDisponibilidad("$fechaSeleccionada $horaSeleccionada")
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@Activity_empleados, "Error al cargar salas: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }



    private fun mostrarDialogoDetallesSala(sala: Salas) {
        val fechaHora = "$fechaSeleccionada $horaSeleccionada"
        val nombreUsuario = getSharedPreferences("mi_preferencia", MODE_PRIVATE)
            .getString("nombre_usuario", "Empleado") ?: "Empleado"

        lifecycleScope.launch {
            // Obtener reservas para la fecha y hora con Room
            val reservas = repository.getReservasPorFechaHora(fechaHora).toMutableList()

            val reservaExistente = reservas.find {
                it.nombreSala == sala.nombre &&
                        it.piso.trim() == sala.piso.trim() &&
                        it.fechaHora == fechaHora &&
                        it.nombreUsuario == nombreUsuario
            }

            val builder = AlertDialog.Builder(this@Activity_empleados)  // Usa this@TuActivity si es necesario
            builder.setTitle("Detalles de ${sala.nombre}")

            val detalles = """
            ${sala.piso}
            Tamaño: ${sala.tamaño}
            Extras: ${if (sala.extras.isNotEmpty()) sala.extras.joinToString(", ") else "Ninguno"}
            Fecha: $fechaSeleccionada
            Hora: $horaSeleccionada
        """.trimIndent()
            builder.setMessage(detalles)

            if (reservaExistente != null) {
                builder.setPositiveButton("Cancelar reserva") { _, _ ->
                    lifecycleScope.launch {
                        repository.eliminarReserva(reservaExistente)
                        verificarDisponibilidad(fechaHora)
                        Toast.makeText(this@Activity_empleados, "Reserva cancelada para ${sala.nombre}", Toast.LENGTH_SHORT).show()
                    }
                }
            } else {
                builder.setPositiveButton("Reservar") { _, _ ->
                    reservarSala(sala.nombre)
                }
            }

            builder.setNegativeButton("Cerrar") { dialog, _ ->
                dialog.dismiss()
            }

            val dialog = builder.create()
            dialog.window?.setBackgroundDrawableResource(R.drawable.dialog_background)
            dialog.setOnShowListener {
                dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                    ?.setTextColor(ContextCompat.getColor(this@Activity_empleados, R.color.black))
                dialog.getButton(AlertDialog.BUTTON_NEGATIVE)
                    ?.setTextColor(ContextCompat.getColor(this@Activity_empleados, R.color.black))
            }
            dialog.show()
        }
    }



    private fun formatearTextoSala(sala: Salas): String {
        val builder = StringBuilder().append(sala.nombre)
        if (sala.extras.isNotEmpty()) {
            builder.append("\n")
            sala.extras.forEach {
                builder.append(
                    when (it) {
                        "WiFi" -> "📶 "
                        "Proyector" -> "📽️ "
                        "Pizarra" -> "🖍️ "
                        else -> ""
                    }
                )
            }
        }
        return builder.toString()
    }

    private fun cargarImagenFondo(nombrePiso: String) {
        val sharedPref = getSharedPreferences("DistribucionSalas", MODE_PRIVATE)
        val fondoUriString = sharedPref.getString("fondo_uri_$nombrePiso", null)

        if (fondoUriString.isNullOrEmpty()) {
            // No hay fondo guardado, opcional: establecer un fondo por defecto
            // container.setBackgroundResource(R.drawable.fondo_predeterminado)
            return
        }

        try {
            val uri = fondoUriString.toUri()
            Glide.with(this)
                .load(uri)
                .centerCrop()
                .into(object : CustomTarget<Drawable>() {
                    override fun onResourceReady(resource: Drawable, transition: Transition<in Drawable>?) {
                        container.background = resource
                    }
                    override fun onLoadCleared(placeholder: Drawable?) {
                        // Opcional: limpiar fondo si es necesario
                    }
                })
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }


    private fun verificarDisponibilidad(fechaHora: String) {
        lifecycleScope.launch {
            val reservas = repository.getReservasPorFechaHora(fechaHora)  // llamada suspend

            for (i in 0 until container.childCount) {
                val view = container.getChildAt(i)
                if (view is Button) {
                    val sala = view.tag as? Salas ?: continue
                    val ocupada = reservas.any {
                        it.nombreSala.equals(sala.nombre, ignoreCase = true) &&
                                it.piso.trim().equals(sala.piso.trim(), ignoreCase = true) &&
                                it.fechaHora == fechaHora
                    }
                    val color = if (ocupada) Color.RED else Color.GREEN
                    view.background = GradientDrawable().apply {
                        setColor(color)
                        cornerRadius = 50f
                    }
                }
            }
        }
    }



    private fun reservarSala(nombreSala: String) {
        val fechaHora = "$fechaSeleccionada $horaSeleccionada"

        val prefs = getSharedPreferences("mi_preferencia", MODE_PRIVATE)
        val idUsuario = prefs.getInt("usuario_id", -1)

        if (idUsuario == -1) {
            Toast.makeText(this, "Usuario no identificado", Toast.LENGTH_SHORT).show()
            return
        }

        lifecycleScope.launch {
            // Obtener usuario actual desde Room
            val usuarioActual = repository.usuarioDao.getUsuarioById(idUsuario)
            val nombreUsuario = usuarioActual?.nombre ?: "Empleado"

            // Cargar reservas para esa fecha y hora
            val reservas = repository.getReservasPorFechaHora(fechaHora)

            // Buscar reserva existente para la misma sala, piso y fechaHora
            val reservaExistente = reservas.find {
                it.nombreSala == nombreSala && it.piso == pisoSeleccionado && it.fechaHora == fechaHora
            }

            // Buscar si el usuario tiene reserva a esa hora en cualquier sala
            val reservaUsuarioMismaHora = reservas.find {
                it.fechaHora == fechaHora && it.nombreUsuario == nombreUsuario
            }

            if (reservaExistente != null) {
                if (reservaExistente.nombreUsuario == nombreUsuario) {
                    val dialog = AlertDialog.Builder(this@Activity_empleados)
                        .setTitle("Cancelar reserva")
                        .setMessage("¿Deseas cancelar tu reserva para '$nombreSala' en '$fechaHora'?")
                        .setPositiveButton("Sí") { _, _ ->
                            lifecycleScope.launch {
                                repository.eliminarReserva(reservaExistente)
                                verificarDisponibilidad(fechaHora)
                                Toast.makeText(this@Activity_empleados, "Reserva cancelada para $nombreSala", Toast.LENGTH_SHORT).show()
                            }
                        }
                        .setNegativeButton("No", null)
                        .create()
                    dialog.show()
                    dialog.window?.setBackgroundDrawableResource(R.drawable.dialog_background)
                } else {
                    Toast.makeText(this@Activity_empleados, "Ya reservada por ${reservaExistente.nombreUsuario} en este piso", Toast.LENGTH_SHORT).show()
                }
                return@launch
            }

            if (reservaUsuarioMismaHora != null) {
                Toast.makeText(this@Activity_empleados, "Ya tienes una reserva a esa hora en '${reservaUsuarioMismaHora.nombreSala}' (${reservaUsuarioMismaHora.piso})", Toast.LENGTH_SHORT).show()
                return@launch
            }

            val dialog = AlertDialog.Builder(this@Activity_empleados)
                .setTitle("Confirmar reserva")
                .setMessage("¿Deseas reservar '$nombreSala' en el '$pisoSeleccionado' para '$fechaHora'?")
                .setPositiveButton("Sí") { _, _ ->
                    lifecycleScope.launch {
                        val reserva = Reserva(nombreSala, fechaHora, nombreUsuario, pisoSeleccionado)
                        repository.insertarReserva(reserva)
                        verificarDisponibilidad(fechaHora)
                        Toast.makeText(this@Activity_empleados, "Reserva realizada para $nombreSala en el $pisoSeleccionado", Toast.LENGTH_SHORT).show()
                    }
                }
                .setNegativeButton("No", null)
                .create()

            dialog.window?.setBackgroundDrawableResource(R.drawable.dialog_background)
            dialog.setOnShowListener {
                val positive = dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                positive.setTextColor(ContextCompat.getColor(this@Activity_empleados, R.color.black))
                val negative = dialog.getButton(AlertDialog.BUTTON_NEGATIVE)
                negative.setTextColor(ContextCompat.getColor(this@Activity_empleados, R.color.black))
            }
            dialog.show()
        }
    }

}