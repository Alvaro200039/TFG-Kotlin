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
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import com.example.tfg_kotlin.MainActivity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class Reserva(val nombreSala: String, val fechaHora: String, val nombreUsuario: String, val piso: String)

class Activity_empleados : AppCompatActivity() {

    private lateinit var container: ConstraintLayout
    private var fechaSeleccionada: String = ""
    private var horaSeleccionada: String = ""
    private lateinit var textViewFecha: TextView
    private lateinit var spinnerPisos: Spinner
    private var pisoSeleccionado: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
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
                Snackbar.make(container, "Primero selecciona una fecha", Snackbar.LENGTH_SHORT).show()
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
                marginStart = 20
            }
        }
        toolbar.addView(spinnerPisos)

        // Texto para mostrar la fecha seleccionada
        textViewFecha = TextView(this).apply {
            setTextColor(Color.BLACK)
            textSize = 16f
            layoutParams = Toolbar.LayoutParams(
                Toolbar.LayoutParams.WRAP_CONTENT,
                Toolbar.LayoutParams.WRAP_CONTENT
            ).apply {
                gravity = Gravity.END
                marginEnd = 16
            }
        }
        toolbar.addView(textViewFecha)

        // Botón para seleccionar fecha y hora
        val botonSeleccionarFechaHora = ImageButton(this).apply {
            setImageResource(R.drawable.time)
            setBackgroundColor(Color.TRANSPARENT)
            setOnClickListener { mostrarDialogoFecha() }
            layoutParams = Toolbar.LayoutParams(
                Toolbar.LayoutParams.WRAP_CONTENT,
                Toolbar.LayoutParams.WRAP_CONTENT
            ).apply {
                gravity = Gravity.END
                marginEnd = 25
            }
        }
        toolbar.addView(botonSeleccionarFechaHora)

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
        val pisosGuardados = pisosGuardadosSet.toList().sorted()

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
        val sharedPref = getSharedPreferences("DistribucionSalas", MODE_PRIVATE)
        val gson = Gson()

        val reservas: MutableList<Reserva> = gson.fromJson(
            sharedPref.getString("reservas", "[]"),
            object : TypeToken<MutableList<Reserva>>() {}.type
        )

        if (reservas.isEmpty()) {
            Toast.makeText(this, "No tienes reservas activas.", Toast.LENGTH_SHORT).show()
            return
        }

        val reservasPorPiso = reservas.groupBy { it.piso }
        val dialogView = layoutInflater.inflate(R.layout.dialog_reservas, null)
        val contenedor = dialogView.findViewById<LinearLayout>(R.id.contenedor_reservas)

        // 🔸 Declaramos dialog como variable externa para poder cerrarlo desde dentro
        lateinit var dialog: AlertDialog

        for ((piso, lista) in reservasPorPiso) {
            val pisoText = TextView(this).apply {
                text = piso
                textSize = 18f
                setPadding(0, 16, 0, 8)
                setTextColor(Color.BLACK)
                setTypeface(null, Typeface.BOLD)
            }
            contenedor.addView(pisoText)

            lista.forEach { reserva ->
                val reservaText = TextView(this).apply {
                    text = "- ${reserva.nombreSala}  ${reserva.fechaHora}"
                    setPadding(16, 4, 0, 4)
                    setTextColor(Color.DKGRAY)
                    setOnClickListener {
                        val confirmDialog = AlertDialog.Builder(this@Activity_empleados)
                            .setTitle("¿Cancelar reserva?")
                            .setMessage("¿Deseas cancelar la reserva de '${reserva.nombreSala}' el ${reserva.fechaHora}?")
                            .setPositiveButton("Sí") { _, _ ->
                                reservas.remove(reserva)
                                sharedPref.edit() { putString("reservas", gson.toJson(reservas)) }

                                // 🔸 Cerrar todos los diálogos antes de refrescar
                                dialog.dismiss()

                                // 🔄 Volver a mostrar el diálogo actualizado
                                mostrarDialogoReservas()
                            }
                            .setNegativeButton("No", null)
                            .create()

                        // 🔹 Aquí aplicas el fondo personalizado
                        confirmDialog.window?.setBackgroundDrawableResource(R.drawable.dialog_background)

                        confirmDialog.show()
                    }
                }
                contenedor.addView(reservaText)
            }

            val divider = View(this).apply {
                setBackgroundColor(Color.LTGRAY)
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    2
                ).apply { setMargins(0, 16, 0, 16) }
            }
            contenedor.addView(divider)
        }

        dialog = AlertDialog.Builder(this)
            .setTitle("Tus reservas activas")
            .setView(dialogView)
            .setPositiveButton("Cerrar", null)
            .create()

        dialog.show()
        dialog.window?.setBackgroundDrawableResource(R.drawable.dialog_background)
        dialog.getButton(AlertDialog.BUTTON_POSITIVE)?.setTextColor(Color.BLACK)
    }

    private fun limpiarReservasPasadas() {
        val sharedPref = getSharedPreferences("DistribucionSalas", MODE_PRIVATE)
        val gson = Gson()

        val reservas: List<Reserva> = gson.fromJson(
            sharedPref.getString("reservas", "[]"),
            object : TypeToken<List<Reserva>>() {}.type
        )

        val formato = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
        val ahora = Date()

        val reservasFuturas = reservas.filter {
            try {
                val fechaReserva = formato.parse(it.fechaHora)
                fechaReserva.after(ahora)
            } catch (e: Exception) {
                true // Si falla el parseo, la dejamos para evitar perder reservas por error
            }
        }

        sharedPref.edit() {
            putString("reservas", gson.toJson(reservasFuturas))
        }
    }



    private fun mostrarDialogoHoras() {
        val prefs = getSharedPreferences("mi_preferencia", MODE_PRIVATE)
        val franjasSet = prefs.getStringSet("franjas_horarias", emptySet()) ?: emptySet()
        val franjasOriginales = franjasSet.toList()

        if (franjasOriginales.isEmpty()) {
            Toast.makeText(this, "No hay franjas horarias disponibles. Crea algunas primero.", Toast.LENGTH_SHORT).show()
            return
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

        // Filtrar franjas si la fecha seleccionada es hoy
        val horasDisponibles = if (fechaSeleccionada == hoy) {
            franjasOriginales.filter { franja ->
                // Comparamos cadenas HH:mm lexicográficamente porque el formato es fijo
                franja >= horaActual
            }
        } else {
            franjasOriginales
        }

        if (horasDisponibles.isEmpty()) {
            Toast.makeText(this, "No hay franjas horarias disponibles para el día seleccionado.", Toast.LENGTH_SHORT).show()
            return
        }

        val horasArray = horasDisponibles.toTypedArray()

        val dialog = AlertDialog.Builder(this)
            .setTitle("Selecciona una franja horaria para el $fechaSeleccionada")
            .setItems(horasArray) { _, which ->
                horaSeleccionada = horasArray[which]
                verificarDisponibilidad("$fechaSeleccionada $horaSeleccionada")
            }
            .create()

        dialog.show()
        dialog.window?.setBackgroundDrawableResource(R.drawable.dialog_background)
        val btnCerrar = dialog.getButton(AlertDialog.BUTTON_NEGATIVE)
        btnCerrar?.setTextColor(Color.BLACK)
    }

    private fun cargarSalas(nombrePiso: String) {
        container.removeAllViews()  // Limpiar el contenedor antes de cargar las salas

        val sharedPref = getSharedPreferences("DistribucionSalas", MODE_PRIVATE)
        val gson = Gson()

        // Recuperar las salas guardadas como JSON
        val jsonSalas = sharedPref.getString("salas_$nombrePiso", "[]")

        // Convertir el JSON en una lista de objetos SalaGuardada
        val salasGuardadas: List<SalaGuardada> = gson.fromJson(jsonSalas, object : TypeToken<List<SalaGuardada>>() {}.type)

        for (salaGuardada in salasGuardadas) {
            val sala = Sala(
                nombre = salaGuardada.nombre,
                tamaño = salaGuardada.tamaño,
                opcionesExtra = salaGuardada.extras,
                piso = pisoSeleccionado
            )

            // Crear un botón para cada sala cargada
            val salaButton = Button(this).apply {
                // Configurar el texto del botón con la información de la sala
                text = formatearTextoSala(sala)
                tag = sala  // Asignar la sala como tag para usarla más tarde

                // Configurar la acción al hacer clic en el botón
                setOnClickListener {
                    if (fechaSeleccionada.isEmpty() || horaSeleccionada.isEmpty()) {
                        Snackbar.make(container, "Selecciona fecha y hora primero", Snackbar.LENGTH_SHORT).show()
                    } else {
                        mostrarDialogoDetallesSala(sala)
                    }
                }

                // Establecer el fondo del botón con un color y bordes redondeados
                background = GradientDrawable().apply {
                    setColor(Color.GREEN)  // Establecemos un color inicial por defecto
                    cornerRadius = 50f
                }

                // Configurar el layoutParams del botón para ubicarlo en el contenedor
                layoutParams = ConstraintLayout.LayoutParams(
                    salaGuardada.ancho.toInt(),
                    salaGuardada.alto.toInt()
                ).apply {
                    leftToLeft = ConstraintLayout.LayoutParams.PARENT_ID
                    topToTop = ConstraintLayout.LayoutParams.PARENT_ID
                    setMargins(salaGuardada.x.toInt(), salaGuardada.y.toInt(), 0, 0)
                }
            }

            // Añadir el botón al contenedor
            container.addView(salaButton)
        }

        // Después de cargar todas las salas, verificar la disponibilidad con la fecha y hora seleccionadas
        if (fechaSeleccionada.isNotEmpty() && horaSeleccionada.isNotEmpty()) {
            verificarDisponibilidad("$fechaSeleccionada $horaSeleccionada")
        }
    }

    private fun mostrarDialogoDetallesSala(sala: Sala) {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Detalles de ${sala.nombre}")

        val detalles = """
        ${sala.piso}
        Tamaño: ${sala.tamaño}
        Extras: ${if (sala.opcionesExtra.isNotEmpty()) sala.opcionesExtra.joinToString(", ") else "Ninguno"}
        Fecha: $fechaSeleccionada
        Hora: $horaSeleccionada
    """.trimIndent()
        builder.setMessage(detalles)

        val fechaHora = "$fechaSeleccionada $horaSeleccionada"
        val sharedPref = getSharedPreferences("DistribucionSalas", MODE_PRIVATE)
        val gson = Gson()
        val reservas: MutableList<Reserva> = gson.fromJson(
            sharedPref.getString("reservas", "[]"),
            object : TypeToken<MutableList<Reserva>>() {}.type
        )

        val nombreUsuario = getSharedPreferences("mi_preferencia", MODE_PRIVATE)
            .getString("nombre_usuario", "Empleado") ?: "Empleado"

        val reservaExistente = reservas.find {
            it.nombreSala == sala.nombre &&
                    it.piso.trim() == sala.piso.trim() &&
                    it.fechaHora == fechaHora &&
                    it.nombreUsuario == nombreUsuario
        }

        if (reservaExistente != null) {
            // Si ya está reservada por el usuario → opción de cancelar
            builder.setPositiveButton("Cancelar reserva") { _, _ ->
                reservas.remove(reservaExistente)
                sharedPref.edit { putString("reservas", gson.toJson(reservas)) }
                verificarDisponibilidad(fechaHora)
                Snackbar.make(container, "Reserva cancelada para ${sala.nombre}", Snackbar.LENGTH_SHORT).show()
            }
        } else {
            // Si no está reservada por el usuario → opción de reservar
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
                ?.setTextColor(ContextCompat.getColor(this, R.color.black))
            dialog.getButton(AlertDialog.BUTTON_NEGATIVE)
                ?.setTextColor(ContextCompat.getColor(this, R.color.black))
        }

        dialog.show()
    }


    private fun formatearTextoSala(sala: Sala): String {
        val builder = StringBuilder().append(sala.nombre)
        if (sala.opcionesExtra.isNotEmpty()) {
            builder.append("\n")
            sala.opcionesExtra.forEach {
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
        val sharedPref = getSharedPreferences("DistribucionSalas", MODE_PRIVATE)
        val gson = Gson()
        val reservas: List<Reserva> = gson.fromJson(
            sharedPref.getString("reservas", "[]"),
            object : TypeToken<List<Reserva>>() {}.type
        )

        for (i in 0 until container.childCount) {
            val view = container.getChildAt(i)
            if (view is Button) {
                val sala = view.tag as? Sala ?: continue

                // Verificamos si hay una reserva exacta para esta sala en esa fecha y hora
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

    private fun reservarSala(nombreSala: String) {
        val fechaHora = "$fechaSeleccionada $horaSeleccionada"
        val sharedPref = getSharedPreferences("DistribucionSalas", MODE_PRIVATE)
        val gson = Gson()
        val reservas: MutableList<Reserva> = gson.fromJson(
            sharedPref.getString("reservas", "[]"),
            object : TypeToken<MutableList<Reserva>>() {}.type
        )

        val nombreUsuario = getSharedPreferences("mi_preferencia", MODE_PRIVATE)
            .getString("nombre_usuario", "Empleado") ?: "Empleado"

        // Buscar una reserva existente para esa sala, fecha, piso
        val reservaExistente = reservas.find {
            it.nombreSala == nombreSala && it.piso == pisoSeleccionado && it.fechaHora == fechaHora
        }

        // Verificar si el usuario ya tiene una reserva en otra sala a la misma hora
        val reservaUsuarioMismaHora = reservas.find {
            it.fechaHora == fechaHora && it.nombreUsuario == nombreUsuario
        }

        // Si ya existe una reserva en esa sala, fecha y piso
        if (reservaExistente != null) {
            if (reservaExistente.nombreUsuario == nombreUsuario) {
                // Si la reserva es del mismo usuario, se ofrece cancelarla
                val dialog = AlertDialog.Builder(this)
                    .setTitle("Cancelar reserva")
                    .setMessage("¿Deseas cancelar tu reserva para '$nombreSala' en '$fechaHora'?")
                    .setPositiveButton("Sí") { _, _ ->
                        reservas.remove(reservaExistente)
                        sharedPref.edit { putString("reservas", gson.toJson(reservas)) }
                        verificarDisponibilidad(fechaHora)
                        Snackbar.make(container, "Reserva cancelada para $nombreSala", Snackbar.LENGTH_SHORT).show()
                    }
                    .setNegativeButton("No", null)
                    .create()
                dialog.show()
                dialog.window?.setBackgroundDrawableResource(R.drawable.dialog_background)
            } else {
                Snackbar.make(container, "Ya reservada por ${reservaExistente.nombreUsuario} en este piso", Snackbar.LENGTH_SHORT).show()
            }
            return
        }

        // Si el usuario ya tiene otra reserva a esa hora (en otra sala)
        if (reservaUsuarioMismaHora != null) {
            Snackbar.make(container, "Ya tienes una reserva a esa hora en '${reservaUsuarioMismaHora.nombreSala}'(${reservaUsuarioMismaHora.piso})", Snackbar.LENGTH_SHORT).show()
            return
        }

        // Si no hay conflictos, hacer la reserva
        val dialog = AlertDialog.Builder(this)
            .setTitle("Confirmar reserva")
            .setMessage("¿Deseas reservar '$nombreSala' en el '$pisoSeleccionado' para '$fechaHora'?")
            .setPositiveButton("Sí") { _, _ ->
                reservas.add(Reserva(nombreSala, fechaHora, nombreUsuario, pisoSeleccionado))
                sharedPref.edit { putString("reservas", gson.toJson(reservas)) }
                verificarDisponibilidad(fechaHora)
                Snackbar.make(container, "Reserva realizada para $nombreSala en el $pisoSeleccionado", Snackbar.LENGTH_SHORT).show()
            }
            .setNegativeButton("No", null)
            .create()

        dialog.window?.setBackgroundDrawableResource(R.drawable.dialog_background)
        dialog.setOnShowListener {
            val positive = dialog.getButton(AlertDialog.BUTTON_POSITIVE)
            positive.setTextColor(ContextCompat.getColor(this, R.color.black))

            val negative = dialog.getButton(AlertDialog.BUTTON_NEGATIVE)
            negative.setTextColor(ContextCompat.getColor(this, R.color.black))
        }
        dialog.show()
    }
}