package com.example.tfg_kotlin

import android.app.DatePickerDialog
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.BitmapDrawable
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
import java.util.Calendar
import androidx.core.net.toUri
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import com.example.tfg_kotlin.activity_menu_creador
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import com.example.tfg_kotlin.database.AppDatabase
import com.example.tfg_kotlin.entities.Piso
import com.example.tfg_kotlin.entities.Reserva
import com.example.tfg_kotlin.entities.Salas
import com.example.tfg_kotlin.repository.AppRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


class Activity_empleados : AppCompatActivity() {
    private lateinit var repository: AppRepository
    private lateinit var container: ConstraintLayout
    private var fechaSeleccionada: String = ""
    private var horaSeleccionada: String = ""
    private lateinit var textViewFecha: TextView
    private lateinit var spinnerPisos: Spinner
    private var pisoSeleccionado: Int = -1
    private lateinit var textViewHora: TextView
    private var snackbarActivo: Snackbar? = null
    private lateinit var listaPisos: List<Piso>



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val db = AppDatabase.getDatabase(applicationContext)
        repository = AppRepository(
            db.usuarioDao(),
            db.salaDao(),
            db.reservaDao(),
            db.franjahorariaDao(),
            db.pisoDao(),
            db.empresaDao()
        )
        enableEdgeToEdge()
        setContentView(R.layout.activity_empleados)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.empleados)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        container = findViewById(R.id.contentLayout)

        val toolbar: Toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = ""

        // Crear spinnerPisos sin adapter por ahora
        spinnerPisos = Spinner(this).apply {
            setPopupBackgroundDrawable(ContextCompat.getDrawable(context, R.drawable.spinner_dropdown_background))
            layoutParams = Toolbar.LayoutParams(
                Toolbar.LayoutParams.WRAP_CONTENT,
                Toolbar.LayoutParams.WRAP_CONTENT
            ).apply {
                gravity = Gravity.START
                marginStart = 0
            }
        }
        toolbar.addView(spinnerPisos)

        // Cargar pisos desde Room y asignar adapter
        lifecycleScope.launch {
            repository.pisoDao.obtenerTodosLosPisos().collect { pisos ->
                listaPisos = pisos
                val nombresPisos = pisos.map { it.nombre }
                val adapter = ArrayAdapter(this@Activity_empleados, android.R.layout.simple_spinner_item, nombresPisos)
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                spinnerPisos.adapter = adapter
            }
        }

        spinnerPisos.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                val piso = listaPisos[position]
                pisoSeleccionado = piso.id
                val nombrePiso = piso.nombre
                val empresaId: Int? = piso.empresaId

                // Carga las salas (sin cargar imagen)
                cargarSalas(
                    nombrePiso,
                    empresaId
                )

                // Lanza coroutine para cargar imagen de fondo
                lifecycleScope.launch {
                    cargarImagenFondo(nombrePiso, empresaId)
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>) {
                pisoSeleccionado = -1
                container.removeAllViews()
                // Opcional: quitar fondo o poner fondo predeterminado
               // container.setBackgroundResource(R.drawable.fondo_predeterminado)
            }
        }



        val btnReservas = findViewById<LinearLayout>(R.id.btn_reservas)
        btnReservas.setOnClickListener {
            mostrarDialogoReservas()
        }

        val btnFranja = findViewById<LinearLayout>(R.id.btn_franja)
        btnFranja.setOnClickListener {
            if (fechaSeleccionada.isEmpty()) {
                snackbarActivo?.dismiss()
                snackbarActivo = Snackbar.make(container, "Primero selecciona una fecha", Snackbar.LENGTH_LONG)
                    .setAction("Seleccionar fecha") { mostrarDialogoFecha() }
                snackbarActivo?.show()
            } else {
                mostrarDialogoHoras()
            }
        }

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
                marginStart = 20
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
    }



    override fun onResume() {
        super.onResume()
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

    private fun cargarSalas(nombrePiso: String, empresaId: Int?) {
        container.removeAllViews()

        lifecycleScope.launch {
            try {
                val salas = repository.getSalasPorNombrePiso(nombrePiso)
                withContext(Dispatchers.Main) {
                    for (sala in salas) {
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


    private suspend fun cargarImagenFondo(nombrePiso: String, empresaId: Int?) {
        val contentLayout = findViewById<ConstraintLayout>(R.id.contentLayout)
        if (empresaId == null) {
            withContext(Dispatchers.Main) {
                contentLayout.setBackgroundResource(R.drawable.wallpaper)
            }
            return
        }

        val piso = repository.pisoDao.obtenerPisoPorNombreYEmpresa(nombrePiso, empresaId)

        withContext(Dispatchers.Main) {
            if (piso == null || piso.imagen == null) {
                contentLayout.setBackgroundResource(R.drawable.wallpaper)
                return@withContext
            }

            try {
                val imagenBytes = piso.imagen
                val bitmap = BitmapFactory.decodeByteArray(imagenBytes, 0, imagenBytes.size)

                Glide.with(this@Activity_empleados)
                    .asBitmap()
                    .load(bitmap)
                    .centerCrop()
                    .into(object : CustomTarget<Bitmap>() {
                        override fun onResourceReady(resource: Bitmap, transition: Transition<in Bitmap>?) {
                            contentLayout.background = BitmapDrawable(resources, resource)
                        }

                        override fun onLoadCleared(placeholder: Drawable?) {
                            // Opcional: limpia el fondo si quieres
                        }
                    })
            } catch (e: Exception) {
                e.printStackTrace()
                contentLayout.setBackgroundResource(R.drawable.wallpaper)
            }
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

        val idUsuario = 123 // Igual que en reservarSala, o cambia para obtenerlo dinámico

        if (idUsuario == -1) {
            Toast.makeText(this, "Usuario no identificado", Toast.LENGTH_SHORT).show()
            return
        }

        lifecycleScope.launch {
            val usuarioActual = repository.usuarioDao.getUsuarioById(idUsuario)
            val nombreUsuario = usuarioActual?.nombre ?: "Juan"

            val reservas = repository.getAllReservas().toMutableList()

            // Filtrar solo las reservas del usuario actual
            val reservasUsuario = reservas.filter { it.idusuario == idUsuario }

            if (reservasUsuario.isEmpty()) {
                Toast.makeText(this@Activity_empleados, "No tienes reservas activas.", Toast.LENGTH_SHORT).show()
                return@launch
            }

            val reservasPorPiso = reservasUsuario.groupBy { it.piso }
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
            try {
                // Obtén la lista desde Room
                val franjas = withContext(Dispatchers.IO) {
                    repository.getFranjasHorarias()
                }
                val franjasOriginales = franjas.map { it.hora }

                if (franjasOriginales.isEmpty()) {
                    Toast.makeText(this@Activity_empleados, "No hay franjas horarias disponibles. Crea algunas primero.", Toast.LENGTH_SHORT).show()
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
                    Toast.makeText(this@Activity_empleados, "No hay franjas horarias disponibles para el día seleccionado.", Toast.LENGTH_SHORT).show()
                    return@launch
                }

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

            } catch (e: Exception) {
                Toast.makeText(this@Activity_empleados, "Error al cargar franjas horarias.", Toast.LENGTH_SHORT).show()
                e.printStackTrace()
            }
        }
    }


    private fun mostrarDialogoDetallesSala(sala: Salas) {
        val fechaHora = "$fechaSeleccionada $horaSeleccionada"
        val idUsuario = 123 // O recuperarlo de SharedPreferences

        if (idUsuario == -1) {
            Toast.makeText(this, "Usuario no identificado", Toast.LENGTH_SHORT).show()
            return
        }

        lifecycleScope.launch {
            val usuario = repository.usuarioDao.getUsuarioById(idUsuario)
            val nombreUsuario = usuario?.nombre ?: "Juan"

            val piso = repository.pisoDao.obtenerPisoPorId(sala.pisoId)
            val reservas = repository.getReservasPorFechaHora(fechaHora).toMutableList()

            val reservaExistente = reservas.find {
                it.nombreSala == sala.nombre &&
                        it.piso.trim() == piso?.nombre?.trim() &&
                        it.fechaHora == fechaHora
            }

            val builder = AlertDialog.Builder(this@Activity_empleados)
            builder.setTitle("Detalles de ${sala.nombre}")

            // Inflar layout personalizado
            val dialogView = layoutInflater.inflate(R.layout.dialog_detalles_sala, null)

            // Referencias a TextViews
            val tvPiso = dialogView.findViewById<TextView>(R.id.tvValorPiso)
            val tvTamano = dialogView.findViewById<TextView>(R.id.tvValorTamano)
            val tvExtras = dialogView.findViewById<TextView>(R.id.tvValorExtras)
            val tvFecha = dialogView.findViewById<TextView>(R.id.tvValorFecha)
            val tvHora = dialogView.findViewById<TextView>(R.id.tvValorHora)

            // Asignar valores
            tvPiso.text = piso?.nombre ?: "Desconocido"
            tvTamano.text = sala.tamaño
            tvExtras.text = if (sala.extras.isNotEmpty()) sala.extras.joinToString(", ") else "Ninguno"
            tvFecha.text = fechaSeleccionada
            tvHora.text = horaSeleccionada

            builder.setView(dialogView)

            if (reservaExistente != null && reservaExistente.nombreUsuario == nombreUsuario) {
                builder.setPositiveButton("Cancelar reserva") { _, _ ->
                    lifecycleScope.launch {
                        repository.eliminarReserva(reservaExistente)
                        verificarDisponibilidad(fechaHora)
                        Toast.makeText(this@Activity_empleados, "Reserva cancelada para ${sala.nombre}", Toast.LENGTH_SHORT).show()
                    }
                }
            } else if (reservaExistente == null) {
                builder.setPositiveButton("Reservar") { _, _ ->
                    reservarSala(sala.nombre)
                }
            } else {
                builder.setPositiveButton("Reservada") { _, _ -> }
            }

            builder.setNegativeButton("Cerrar") { dialog, _ -> dialog.dismiss() }

            val dialog = builder.create()
            dialog.window?.setBackgroundDrawableResource(R.drawable.dialog_background)
            dialog.setOnShowListener {
                val positiveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                val negativeButton = dialog.getButton(AlertDialog.BUTTON_NEGATIVE)

                positiveButton?.setTextColor(ContextCompat.getColor(this@Activity_empleados, R.color.black))
                negativeButton?.setTextColor(ContextCompat.getColor(this@Activity_empleados, R.color.red))

                if (reservaExistente != null && reservaExistente.nombreUsuario != nombreUsuario) {
                    positiveButton?.isEnabled = false
                    positiveButton?.setTextColor(ContextCompat.getColor(this@Activity_empleados, R.color.grey))
                    Toast.makeText(this@Activity_empleados, "Reservada por ${reservaExistente.nombreUsuario} a esta hora", Toast.LENGTH_SHORT).show()
                }
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

    private fun verificarDisponibilidad(fechaHora: String) {
        lifecycleScope.launch {
            val reservas = repository.getReservasPorFechaHora(fechaHora)  // suspend

            // Recolectar el flow para obtener lista
            val pisos = repository.pisoDao.obtenerTodosLosPisos().first()
            val mapaPisos = pisos.associate { it.id to it.nombre }

            for (i in 0 until container.childCount) {
                val view = container.getChildAt(i)
                if (view is Button) {
                    val sala = view.tag as? Salas ?: continue
                    val nombrePisoSala = mapaPisos[sala.pisoId] ?: ""

                    val ocupada = reservas.any {
                        it.nombreSala.equals(sala.nombre, ignoreCase = true) &&
                                it.piso.trim().equals(nombrePisoSala.trim(), ignoreCase = true) &&
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
        val idUsuario = 123   // id de usuario fijo para pruebas

        if (idUsuario == -1) {
            Toast.makeText(this, "Usuario no identificado", Toast.LENGTH_SHORT).show()
            return
        }

        lifecycleScope.launch {
            val usuarioActual = repository.usuarioDao.getUsuarioById(idUsuario)
            val nombreUsuario = usuarioActual?.nombre ?: "Juan"

            // Obtener todos los pisos
            val pisos = repository.pisoDao.obtenerTodosLosPisos().first()
            val nombrePiso = pisos.find { it.id == pisoSeleccionado }?.nombre ?: ""

            // Obtener la sala con nombreSala y pisoId (pisoSeleccionado)
            val salaSeleccionada = repository.salaDao.obtenerSalaPorNombreYPiso(nombreSala, pisoSeleccionado)
            if (salaSeleccionada == null) {
                Toast.makeText(this@Activity_empleados, "No se encontró la sala para reservar", Toast.LENGTH_SHORT).show()
                return@launch
            }

            val reservas = repository.getReservasPorFechaHora(fechaHora)

            // Buscar reserva en la misma sala, mismo piso y fechaHora
            val reservaExistente = reservas.find {
                it.nombreSala == nombreSala && it.piso == nombrePiso && it.fechaHora == fechaHora
            }

            // Buscar si el usuario ya tiene reserva a esa hora, sin importar la sala o piso
            val reservaUsuarioMismaHora = reservas.find {
                it.fechaHora == fechaHora && it.idusuario == idUsuario
            }

            if (reservaExistente != null) {
                if (reservaExistente.idusuario == idUsuario) {
                    // Preguntar si quiere cancelar su reserva
                    val dialog = AlertDialog.Builder(this@Activity_empleados)
                        .setTitle("Cancelar reserva")
                        .setMessage("'$nombreUsuario', ¿Deseas cancelar tu reserva para '$nombreSala' en '$fechaHora'?")
                        .setPositiveButton("Sí") { _, _ ->
                            lifecycleScope.launch {
                                repository.eliminarReserva(reservaExistente)
                                verificarDisponibilidad(fechaHora)
                                Toast.makeText(this@Activity_empleados, "Reserva cancelada para $nombreSala", Toast.LENGTH_SHORT).show()
                            }
                        }
                        .setNegativeButton("No", null)
                        .create()
                    dialog.setOnShowListener {
                        val positiveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                        val negativeButton = dialog.getButton(AlertDialog.BUTTON_NEGATIVE)

                        positiveButton?.setTextColor(ContextCompat.getColor(this@Activity_empleados, R.color.black))
                        negativeButton?.setTextColor(ContextCompat.getColor(this@Activity_empleados, R.color.red))
                    }
                    dialog.show()
                    dialog.window?.setBackgroundDrawableResource(R.drawable.dialog_background)
                } else {
                    Toast.makeText(
                        this@Activity_empleados,
                        "Sala ya reservada por ${reservaExistente.nombreUsuario} en este piso",
                        Toast.LENGTH_SHORT
                    ).show()
                }
                return@launch
            }

            if (reservaUsuarioMismaHora != null) {
                Toast.makeText(
                    this@Activity_empleados,
                    "Ya tienes una reserva a esa hora en '${reservaUsuarioMismaHora.nombreSala}' (${reservaUsuarioMismaHora.piso})",
                    Toast.LENGTH_SHORT
                ).show()
                return@launch
            }

            val dialog = AlertDialog.Builder(this@Activity_empleados)
                .setTitle("Confirmar reserva")
                .setMessage("¿Deseas reservar '$nombreSala' en el '$nombrePiso' para '$fechaHora'?")
                .setPositiveButton("Sí") { _, _ ->
                    lifecycleScope.launch {
                        val reserva = Reserva(
                            nombreSala = nombreSala,
                            fechaHora = fechaHora,
                            nombreUsuario = nombreUsuario,
                            idusuario = idUsuario,
                            piso = nombrePiso,
                            id = 0,                  // Room generará el id automáticamente
                            idSala = salaSeleccionada.id  // ID real de la sala
                        )
                        repository.insertarReserva(reserva)
                        verificarDisponibilidad(fechaHora)
                        Toast.makeText(this@Activity_empleados, "Reserva realizada para $nombreSala en el $nombrePiso", Toast.LENGTH_SHORT).show()
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