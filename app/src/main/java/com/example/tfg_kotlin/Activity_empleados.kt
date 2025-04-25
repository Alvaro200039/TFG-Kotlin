package com.example.tfg_kotlin

import android.app.DatePickerDialog
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.provider.MediaStore
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.ImageButton
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
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

data class Reserva(val nombreSala: String, val fechaHora: String, val nombreUsuario: String)

class Activity_empleados : AppCompatActivity() {

    private lateinit var container: ConstraintLayout
    private var fechaSeleccionada: String = ""
    private var horaSeleccionada: String = ""
    private lateinit var textViewFecha: TextView

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

        // Recuperar el nombre de la empresa desde SharedPreferences
        val sharedPreferences = getSharedPreferences("mi_preferencia", MODE_PRIVATE)
        val nombreEmpresa = sharedPreferences.getString("nombre_empresa", "Nombre Empresa Predeterminado")
        supportActionBar?.title = nombreEmpresa // Actualizar el título de la Toolbar con el nombre guardado
        // Crear un TextView para mostrar la fecha seleccionada
            textViewFecha = TextView(this).apply {
            id = View.generateViewId()
            setTextColor(Color.BLACK)
            textSize = 16f
            layoutParams = Toolbar.LayoutParams(
                Toolbar.LayoutParams.WRAP_CONTENT,
                Toolbar.LayoutParams.WRAP_CONTENT
            ).apply {
                gravity = Gravity.NO_GRAVITY // Alinearlo a la derecha
                marginStart = 400 // Ajustar la separación entre la fecha y el botón
            }
        }
        // Agregar el TextView a la Toolbar
        toolbar.addView(textViewFecha)

        // Botón para abrir selector de fecha y hora
        val botonSeleccionarFechaHora = ImageButton(this).apply {
            id = View.generateViewId()
            setImageResource(R.drawable.time) // Reemplaza con tu ícono
            setBackgroundColor(Color.TRANSPARENT)
            layoutParams = Toolbar.LayoutParams(
                Toolbar.LayoutParams.WRAP_CONTENT,
                Toolbar.LayoutParams.WRAP_CONTENT
            ).apply {
                gravity = Gravity.END // Alinearlo a la derecha
                marginEnd = 25
            }
            setOnClickListener { mostrarDialogoFecha() }
        }
        toolbar.addView(botonSeleccionarFechaHora)

        cargarSalas()
        cargarImagenFondo()
    }

    private fun mostrarDialogoFecha() {
        val calendario = Calendar.getInstance()
        val datePicker = DatePickerDialog(
            this,
            { _, year, month, dayOfMonth ->
                fechaSeleccionada = String.format("%02d/%02d/%d", dayOfMonth, month + 1, year)
                textViewFecha.text = "Fecha: $fechaSeleccionada"
                mostrarDialogoHoras()
            },
            calendario.get(Calendar.YEAR),
            calendario.get(Calendar.MONTH),
            calendario.get(Calendar.DAY_OF_MONTH)
        )
        datePicker.show()

// Aplicar fondo redondeado después de mostrarlo
        datePicker.window?.setBackgroundDrawable(
            ContextCompat.getDrawable(this, R.drawable.dialog_background)
        )
    }

    private fun mostrarDialogoHoras() {
        val horasDisponibles = arrayOf(
            "09:00 - 10:00", "10:00 - 11:00", "11:00 - 12:00",
            "12:00 - 13:00", "13:00 - 14:00", "16:00 - 17:00", "17:00 - 18:00"
        )

        val dialog = AlertDialog.Builder(this)
            .setTitle("Selecciona una franja horaria para el $fechaSeleccionada")
            .setItems(horasDisponibles) { _, which ->
                horaSeleccionada = horasDisponibles[which]
                verificarDisponibilidad("$fechaSeleccionada $horaSeleccionada")
            }
            .create()
            dialog.show()
            dialog.window?.setBackgroundDrawable(
            ContextCompat.getDrawable(this, R.drawable.dialog_background))
    }

    private fun cargarSalas() {
        val sharedPref = getSharedPreferences("DistribucionSalas", MODE_PRIVATE)
        val gson = Gson()
        val jsonSalas = sharedPref.getString("salas", "[]")
        val salasGuardadas: List<SalaGuardada> = gson.fromJson(jsonSalas, object : TypeToken<List<SalaGuardada>>() {}.type)

        for (salaGuardada in salasGuardadas) {
            val sala = Sala(
                nombre = salaGuardada.nombre,
                tamaño = salaGuardada.tamaño,
                opcionesExtra = salaGuardada.extras
            )

            val salaButton = Button(this).apply {
                text = formatearTextoSala(sala)
                tag = sala
                setOnClickListener {
                    if (fechaSeleccionada.isEmpty() || horaSeleccionada.isEmpty()) {
                        Snackbar.make(container, "Selecciona fecha y hora primero", Snackbar.LENGTH_SHORT).show()
                    } else {
                        reservarSala(sala.nombre)
                    }
                }

                background = GradientDrawable().apply {
                    setColor(Color.GREEN)
                    cornerRadius = 50f
                }

                layoutParams = ConstraintLayout.LayoutParams(
                    salaGuardada.ancho.toInt(),
                    salaGuardada.alto.toInt()
                ).apply {
                    leftToLeft = ConstraintLayout.LayoutParams.PARENT_ID
                    topToTop = ConstraintLayout.LayoutParams.PARENT_ID
                    setMargins(salaGuardada.x.toInt(), salaGuardada.y.toInt(), 0, 0)
                }
            }
            container.addView(salaButton)
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

    private fun cargarImagenFondo() {
        val sharedPref = getSharedPreferences("DistribucionSalas", MODE_PRIVATE)

        // Solo cargar si se ha guardado explícitamente
        val distribucionGuardada = sharedPref.getBoolean("distribucion_guardada", false)
        if (!distribucionGuardada) return

        val fondoUriString = sharedPref.getString("fondo_uri", null)

        fondoUriString?.let {
            try {
                val bitmap = MediaStore.Images.Media.getBitmap(contentResolver, it.toUri())
                findViewById<ConstraintLayout>(R.id.contentLayout).background =
                    bitmap.toDrawable(resources)
            } catch (e: Exception) {
                e.printStackTrace()
            }
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
                val ocupada = reservas.any { it.nombreSala == sala.nombre && it.fechaHora == fechaHora }
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

        val reservaExistente = reservas.find { it.nombreSala == nombreSala && it.fechaHora == fechaHora }

        if (reservaExistente != null) {
            if (reservaExistente.nombreUsuario == nombreUsuario) {
                // Mostrar diálogo para cancelar la reserva propia
                val dialog = AlertDialog.Builder(this)
                    .setTitle("Cancelar reserva")
                    .setMessage("¿Deseas cancelar tu reserva para la sala '$nombreSala' en '$fechaHora'?")
                    .setPositiveButton("Sí") { _, _ ->
                        reservas.remove(reservaExistente)
                        sharedPref.edit() { putString("reservas", gson.toJson(reservas)) }
                        verificarDisponibilidad(fechaHora)
                        Snackbar.make(container, "Reserva cancelada para $nombreSala", Snackbar.LENGTH_SHORT).show()
                    }
                    .setNegativeButton("No", null)
                    .create()

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
                dialog.show()
            } else {
                // Mostrar quién tiene la reserva
                Snackbar.make(container, "Ya reservada por ${reservaExistente.nombreUsuario}", Snackbar.LENGTH_SHORT).show()
            }
            return
        }

        // Si no está reservada, mostrar diálogo de confirmación
        val dialog = AlertDialog.Builder(this)
            .setTitle("Confirmar reserva")
            .setMessage("¿Deseas reservar la sala '$nombreSala' para '$fechaHora'?")
            .setPositiveButton("Sí") { _, _ ->
                reservas.add(Reserva(nombreSala, fechaHora, nombreUsuario))
                sharedPref.edit() { putString("reservas", gson.toJson(reservas)) }
                verificarDisponibilidad(fechaHora)
                Snackbar.make(container, "Reserva realizada para $nombreSala", Snackbar.LENGTH_SHORT).show()
            }
            .setNegativeButton("No", null)
            .create()

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
        dialog.show()
    }
}