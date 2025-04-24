package com.example.tfg_kotlin

import android.app.DatePickerDialog
import android.graphics.Color
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.ImageButton
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
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.util.Calendar

data class Reserva(val nombreSala: String, val fechaHora: String)

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
        val datePicker = DatePickerDialog(this,
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
    }

    private fun mostrarDialogoHoras() {
        val horasDisponibles = arrayOf(
            "09:00 - 10:00", "10:00 - 11:00", "11:00 - 12:00",
            "12:00 - 13:00", "13:00 - 14:00", "16:00 - 17:00", "17:00 - 18:00"
        )

        AlertDialog.Builder(this)
            .setTitle("Selecciona una franja horaria para $fechaSeleccionada")
            .setItems(horasDisponibles) { _, which ->
                horaSeleccionada = horasDisponibles[which]
                verificarDisponibilidad("$fechaSeleccionada $horaSeleccionada")
            }
            .create()
            .show()
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
                        Toast.makeText(context, "Selecciona fecha y hora primero", Toast.LENGTH_SHORT).show()
                    } else {
                        reservarSala(sala.nombre)
                    }
                }

                background = GradientDrawable().apply {
                    setColor(Color.GREEN)
                    cornerRadius = 50f
                }

                layoutParams = ConstraintLayout.LayoutParams(
                    ConstraintLayout.LayoutParams.WRAP_CONTENT,
                    ConstraintLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    leftToLeft = ConstraintLayout.LayoutParams.PARENT_ID
                    topToTop = ConstraintLayout.LayoutParams.PARENT_ID
                    setMargins(salaGuardada.x.toInt(), salaGuardada.y.toInt(), 0, 0)
                }

                if (sala.tamaño == "Grande") {
                    textSize = 22f
                    setPadding(48, 32, 48, 32)
                } else {
                    textSize = 14f
                    setPadding(32, 16, 32, 16)
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
        val fondoUriString = sharedPref.getString("fondo_uri", null)

        fondoUriString?.let {
            try {
                val bitmap = MediaStore.Images.Media.getBitmap(contentResolver, Uri.parse(it))
                findViewById<ConstraintLayout>(R.id.contentLayout).background = BitmapDrawable(resources, bitmap)
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

        if (reservas.any { it.nombreSala == nombreSala && it.fechaHora == fechaHora }) {
            Toast.makeText(this, "La sala ya está reservada en ese horario", Toast.LENGTH_SHORT).show()
            return
        }

        val builder = AlertDialog.Builder(this)
            .setTitle("Confirmar reserva")
            .setMessage("¿Deseas reservar la sala '$nombreSala' para '$fechaHora'?")
            .setPositiveButton("Sí") { _, _ ->
                reservas.add(Reserva(nombreSala, fechaHora))
                sharedPref.edit().putString("reservas", gson.toJson(reservas)).apply()
                verificarDisponibilidad(fechaHora)
                Toast.makeText(this, "Reserva realizada para $nombreSala", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("No", null)

        val dialog = builder.create()
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