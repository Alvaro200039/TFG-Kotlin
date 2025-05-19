package com.example.tfg_kotlin

import android.app.DatePickerDialog
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.provider.MediaStore
import android.view.Gravity
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.ImageButton
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.graphics.toColorInt
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.snackbar.Snackbar
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.util.Calendar
import androidx.core.content.edit
import androidx.core.net.toUri
import androidx.core.graphics.drawable.toDrawable

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
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.container)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        container = findViewById(R.id.contentLayout)

        val toolbar: Toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.title = ""

        val sharedPreferences = getSharedPreferences("DistribucionSalas", MODE_PRIVATE)
        val pisosGuardadosSet = sharedPreferences.getStringSet("pisos", setOf()) ?: setOf()
        val pisosGuardados = pisosGuardadosSet.toList().sorted()

        spinnerPisos = Spinner(this).apply {
            adapter = ArrayAdapter(this@Activity_empleados, android.R.layout.simple_spinner_dropdown_item, pisosGuardados.toList())
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
            // Alineamos el texto a la derecha de la Toolbar
            layoutParams = Toolbar.LayoutParams(
                Toolbar.LayoutParams.WRAP_CONTENT,
                Toolbar.LayoutParams.WRAP_CONTENT
            ).apply {
                gravity = Gravity.END // Esto alinea el TextView a la derecha
                marginEnd = 16 // Opcional: añadir un margen para separar el texto del borde derecho
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
                gravity = Gravity.END // Esto alinea el botón a la derecha también
                marginEnd = 25
            }
        }
        toolbar.addView(botonSeleccionarFechaHora)
    }

    private fun mostrarDialogoFecha() {
        val calendario = Calendar.getInstance()
        val datePicker = DatePickerDialog(
            this,
            { _, year, month, dayOfMonth ->
                fechaSeleccionada = String.format("%02d/%02d/%d", dayOfMonth, month + 1, year)
                textViewFecha.text = "Fecha: $fechaSeleccionada"
                mostrarDialogoHoras() // Llamamos a mostrar las horas después de seleccionar la fecha
            },
            calendario.get(Calendar.YEAR),
            calendario.get(Calendar.MONTH),
            calendario.get(Calendar.DAY_OF_MONTH)
        )
        datePicker.show()
    }

    private fun mostrarDialogoHoras() {
        val prefs = getSharedPreferences("mi_preferencia", MODE_PRIVATE)
        val franjasSet = prefs.getStringSet("franjas_horarias", emptySet()) ?: emptySet()
        val horasDisponibles = franjasSet.toTypedArray()

        if (horasDisponibles.isEmpty()) {
            Toast.makeText(this, "No hay franjas horarias disponibles. Crea algunas primero.", Toast.LENGTH_SHORT).show()
            return
        }

        val dialog = AlertDialog.Builder(this)
            .setTitle("Selecciona una franja horaria para el $fechaSeleccionada")
            .setItems(horasDisponibles) { _, which ->
                horaSeleccionada = horasDisponibles[which]
                verificarDisponibilidad("$fechaSeleccionada $horaSeleccionada")
            }
            .create()
        dialog.show()
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
                        reservarSala(sala.nombre)  // Llamar a la función para reservar la sala
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

        // Verificar si la distribución ha sido guardada
        if (!sharedPref.getBoolean("distribucion_guardada", false)) return

        // Recuperar el URI del fondo específico para el piso actual
        val fondoUriString = sharedPref.getString("fondo_uri_$nombrePiso", null)

        // Si se encuentra el URI, cargar la imagen de fondo
        fondoUriString?.let {
            try {
                val bitmap = MediaStore.Images.Media.getBitmap(contentResolver, it.toUri())
                container.background = bitmap.toDrawable(resources)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        } ?: run {
            // Si no hay fondo guardado, puedes establecer un fondo predeterminado o hacer algo más
            // container.setBackgroundResource(R.drawable.fondo_predeterminado)
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

        dialog.show()
        dialog.window?.setBackgroundDrawableResource(R.drawable.dialog_background)
        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE)?.apply {
                setBackgroundColor("#008000".toColorInt())
                setTextColor(Color.WHITE)
            }
            dialog.getButton(AlertDialog.BUTTON_NEGATIVE)?.apply {
                setBackgroundColor("#B22222".toColorInt())
                setTextColor(Color.WHITE)
            }
        }
    }
}