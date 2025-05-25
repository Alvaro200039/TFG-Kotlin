package com.example.tfg_kotlin

import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import androidx.core.content.edit
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_menu_creador)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.menucreacion)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        limpiarReservasPasadas()
        mostrarSiguienteReserva()


        // Botón: Crear o editar salas
        val btnEditarSalas = findViewById<Button>(R.id.btnEditarSalas)
        btnEditarSalas.setOnClickListener {
            val intent = Intent(this, Activity_creacion::class.java)
            startActivity(intent)
        }

        // Botón: Hacer nueva reserva
        val btnNuevaReserva = findViewById<Button>(R.id.btnNuevaReserva)
        btnNuevaReserva.setOnClickListener {
            val prefNumeroPiso = getSharedPreferences("mi_preferencia", MODE_PRIVATE)
            val nombrePiso = prefNumeroPiso.getString("numero_piso", null)

            val prefDistribucion = getSharedPreferences("DistribucionSalas", MODE_PRIVATE)
            val pisosGuardadosSet = prefDistribucion.getStringSet("pisos", emptySet()) ?: emptySet()

            if (nombrePiso != null && pisosGuardadosSet.contains(nombrePiso)) {
                // Aquí opcionalmente podrías verificar si hay salas guardadas para ese piso
                val salasJson = prefDistribucion.getString("salas_$nombrePiso", null)
                if (!salasJson.isNullOrEmpty()) {
                    val intent = Intent(this, Activity_empleados::class.java)
                    intent.putExtra("nombre_piso", nombrePiso)
                    startActivity(intent)
                } else {
                    Toast.makeText(this, "No hay salas guardadas para el piso $nombrePiso. Crea una distribución primero.", Toast.LENGTH_LONG).show()
                }
            } else {
                Toast.makeText(this, "No hay piso guardado o válido. Crea uno primero.", Toast.LENGTH_SHORT).show()
            }
        }

        // Botón: Ver mis reservas
        val btnVerReservas = findViewById<Button>(R.id.btnVerReservas)
        btnVerReservas.setOnClickListener {
            mostrarDialogoReservas()
        }

        // Botón: Cerrar sesión (opcional)
        val btnLogout = findViewById<Button>(R.id.btnLogout)
        btnLogout?.setOnClickListener {
            // Aquí podrías limpiar datos, cerrar sesión y volver al login
            // Por ejemplo:
            // startActivity(Intent(this, LoginActivity::class.java))
            // finish()
        }

        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayShowTitleEnabled(false)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
    }

    override fun onResume() {
        super.onResume()

        val prefNumeroPiso = getSharedPreferences("mi_preferencia", MODE_PRIVATE)
        val nombrePiso = prefNumeroPiso.getString("numero_piso", null)

        val prefDistribucion = getSharedPreferences("DistribucionSalas", MODE_PRIVATE)
        val pisosGuardadosSet = prefDistribucion.getStringSet("pisos", emptySet()) ?: emptySet()

        if (nombrePiso == null || !pisosGuardadosSet.contains(nombrePiso)) {
            Toast.makeText(this, "No hay piso válido guardado. Crea uno primero.", Toast.LENGTH_SHORT).show()
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


    private fun mostrarDialogoReservas() {
        limpiarReservasPasadas()
        val sharedPref = getSharedPreferences("DistribucionSalas", MODE_PRIVATE)
        val gson = Gson()

        val reservas: List<Reserva> = gson.fromJson(
            sharedPref.getString("reservas", "[]"),
            object : TypeToken<List<Reserva>>() {}.type
        )

        if (reservas.isEmpty()) {
            Toast.makeText(this, "No tienes reservas activas.", Toast.LENGTH_SHORT).show()
            return
        }

        // Agrupar por piso
        val reservasPorPiso = reservas.groupBy { it.piso }

        // Inflar el layout personalizado
        val dialogView = layoutInflater.inflate(R.layout.dialog_reservas, null)
        val contenedor = dialogView.findViewById<LinearLayout>(R.id.contenedor_reservas)

        for ((piso, lista) in reservasPorPiso) {
            // Título del piso
            val pisoText = TextView(this).apply {
                text = piso
                textSize = 18f
                setPadding(0, 16, 0, 8)
                setTextColor(Color.BLACK)
                setTypeface(null, Typeface.BOLD)
            }
            contenedor.addView(pisoText)

            // Añadir las reservas de ese piso
            lista.forEach { reserva ->
                val reservaText = TextView(this).apply {
                    text = "- ${reserva.nombreSala}  ${reserva.fechaHora}"
                    setPadding(16, 4, 0, 4)
                    setTextColor(Color.DKGRAY)
                }
                contenedor.addView(reservaText)
            }

            // Línea divisoria entre pisos
            val divider = View(this).apply {
                setBackgroundColor(Color.LTGRAY)
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    2
                ).apply { setMargins(0, 16, 0, 16) }
            }
            contenedor.addView(divider)
        }

        val dialog = AlertDialog.Builder(this)
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

    private fun mostrarSiguienteReserva() {
        val sharedPref = getSharedPreferences("DistribucionSalas", MODE_PRIVATE)
        val gson = Gson()

        val reservas: List<Reserva> = gson.fromJson(
            sharedPref.getString("reservas", "[]"),
            object : TypeToken<List<Reserva>>() {}.type
        )

        val textView = findViewById<TextView>(R.id.textProximaReserva)

        if (reservas.isEmpty()) {
            textView.text = "No hay reservas"
            return
        }

        val formato = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
        val ahora = Date()

        // Filtrar reservas que aún no han pasado (fechaHora > ahora)
        val reservasFuturas = reservas.filter {
            try {
                val fechaReserva = formato.parse(it.fechaHora)
                fechaReserva != null && fechaReserva.after(ahora)
            } catch (e: Exception) {
                false
            }
        }

        if (reservasFuturas.isEmpty()) {
            textView.text = "No hay reservas próximas"
            return
        }

        // Ordenar por fecha ascendente
        val siguienteReserva = reservasFuturas.minByOrNull {
            formato.parse(it.fechaHora)?.time ?: Long.MAX_VALUE
        }

        // Mostrar datos en el TextView
        siguienteReserva?.let {
            val texto = "Siguiente reserva \n${it.nombreSala} el ${it.fechaHora}"
            textView.text = texto
        }
    }




}
