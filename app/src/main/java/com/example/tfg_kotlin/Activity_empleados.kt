package com.example.tfg_kotlin

import android.graphics.Color
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.Spinner
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

data class Reserva(val nombreSala: String, val hora: String)

class Activity_empleados : AppCompatActivity() {

    private lateinit var container: ConstraintLayout
    private lateinit var spinnerHora: Spinner

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_empleados)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.container)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Configurar la Toolbar con el Spinner
        val toolbar: Toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)

        spinnerHora = findViewById(R.id.spinnerHora)

        // Crear lista de horas
        val horasDisponibles = arrayOf(
            "09:00 - 10:00",
            "10:00 - 11:00",
            "11:00 - 12:00",
            "12:00 - 13:00",
            "13:00 - 14:00",
            "16:00 - 17:00",
            "17:00 - 18:00"
        )

        // Adaptador para el Spinner
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, horasDisponibles)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerHora.adapter = adapter

        // Configurar el Listener para el Spinner
        spinnerHora.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parentView: AdapterView<*>, view: View?, position: Int, id: Long) {
                val horaSeleccionada = horasDisponibles[position]
                verificarDisponibilidad(horaSeleccionada)
            }

            override fun onNothingSelected(parentView: AdapterView<*>) {}
        }

        // Inicializar el contenedor de botones (salas)
        container = findViewById(R.id.container)
        cargarSalas()

        // Cargar la imagen de fondo
        cargarImagenFondo()
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
                    reservarSala(sala.nombre)
                }

                // Fondo con esquinas redondeadas y color verde (disponible)
                background = GradientDrawable().apply {
                    setColor(Color.GREEN) // Verde para disponible
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

                setPadding(32, 16, 32, 16)

                textSize = if (sala.tamaño == "Grande") 22f else 14f
            }
            container.addView(salaButton)
        }
    }

    // Formatear texto con nombre y extras
    private fun formatearTextoSala(sala: Sala): String {
        val builder = StringBuilder()
        builder.append(sala.nombre)
        if (sala.opcionesExtra.isNotEmpty()) {
            builder.append("\n")
            sala.opcionesExtra.forEach { extra ->
                when (extra) {
                    "WiFi" -> builder.append("📶 ")
                    "Proyector" -> builder.append("📽️ ")
                    "Pizarra" -> builder.append("🖍️ ")
                }
            }
        }
        return builder.toString()
    }

    // Función para cargar la imagen de fondo
    private fun cargarImagenFondo() {
        val sharedPref = getSharedPreferences("DistribucionSalas", MODE_PRIVATE)
        val fondoUriString = sharedPref.getString("fondo_uri", null)

        fondoUriString?.let {
            val uri = Uri.parse(it)
            try {
                val bitmap = MediaStore.Images.Media.getBitmap(contentResolver, uri)
                val contentLayout = findViewById<ConstraintLayout>(R.id.contentLayout)
                contentLayout.background = BitmapDrawable(resources, bitmap)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    // Función para verificar la disponibilidad
    private fun verificarDisponibilidad(horaSeleccionada: String) {
        val sharedPref = getSharedPreferences("DistribucionSalas", MODE_PRIVATE)
        val gson = Gson()
        val jsonReservas = sharedPref.getString("reservas", "[]")
        val reservas: List<Reserva> = gson.fromJson(jsonReservas, object : TypeToken<List<Reserva>>() {}.type)

        for (i in 0 until container.childCount) {
            val view = container.getChildAt(i)
            if (view is Button) {
                val sala = (view.tag as? Sala) ?: continue

                val ocupada = reservas.any { it.nombreSala == sala.nombre && it.hora == horaSeleccionada }

                val color = if (ocupada) Color.RED else Color.GREEN

                val fondo = GradientDrawable().apply {
                    setColor(color)
                    cornerRadius = 50f
                }
                view.background = fondo
            }
        }
    }

    // Función para reservar una sala
    private fun reservarSala(nombreSala: String) {
        val horaSeleccionada = spinnerHora.selectedItem as String
        val sharedPref = getSharedPreferences("DistribucionSalas", MODE_PRIVATE)
        val gson = Gson()
        val jsonReservas = sharedPref.getString("reservas", "[]")
        val reservas: MutableList<Reserva> = gson.fromJson(jsonReservas, object : TypeToken<MutableList<Reserva>>() {}.type)

        // Verificar si ya está reservada
        if (reservas.any { it.nombreSala == nombreSala && it.hora == horaSeleccionada }) {
            Toast.makeText(this, "La sala ya está reservada en ese horario", Toast.LENGTH_SHORT).show()
            return
        }

        // Mostrar AlertDialog de confirmación
        val mensaje = "¿Deseas reservar la sala '$nombreSala' para la hora '$horaSeleccionada'?"
        val builder = androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Confirmar reserva")
            .setMessage(mensaje)
            .setPositiveButton("Sí") { _, _ ->
                // Confirmación: guardar reserva
                val nuevaReserva = Reserva(nombreSala, horaSeleccionada)
                reservas.add(nuevaReserva)

                val editor = sharedPref.edit()
                editor.putString("reservas", gson.toJson(reservas))
                editor.apply()

                verificarDisponibilidad(horaSeleccionada)
                Toast.makeText(this, "Reserva realizada para $nombreSala a las $horaSeleccionada", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("No", null)

        // Crear el AlertDialog
        val dialog = builder.create()

        // Cambiar fondo del AlertDialog
        dialog.window?.setBackgroundDrawableResource(R.drawable.dialog_background)

        // Cambiar los botones cuando el diálogo se muestre
        dialog.setOnShowListener {
            val positiveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE)
            val negativeButton = dialog.getButton(AlertDialog.BUTTON_NEGATIVE)

            // Cambiar color de fondo de los botones
            positiveButton.setBackgroundColor("#008000".toColorInt())  // Botón verde
            negativeButton.setBackgroundColor("#B22222".toColorInt())  // Botón rojo

            // Cambiar color de texto de los botones
            positiveButton.setTextColor("#FFFFFF".toColorInt())  // Texto en blanco para el botón "Sí"
            negativeButton.setTextColor("#FFFFFF".toColorInt())  // Texto en blanco para el botón "No"
        }

        // Mostrar el diálogo
        dialog.show()
    }
}