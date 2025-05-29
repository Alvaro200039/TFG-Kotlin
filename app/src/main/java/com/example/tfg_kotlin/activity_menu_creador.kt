package com.example.tfg_kotlin

import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.widget.Button
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
import androidx.lifecycle.lifecycleScope
import com.example.tfg_kotlin.database.AppDatabase
import com.example.tfg_kotlin.repository.AppRepository
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class activity_menu_creador : AppCompatActivity() {

    private lateinit var repository: AppRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val db = AppDatabase.getDatabase(applicationContext)
        repository = AppRepository(
            db.jefeDao(),
            db.salaDao(),
            db.reservaDao()
        )
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

            if (!nombrePiso.isNullOrEmpty()) {
                val intent = Intent(this, Activity_empleados::class.java)
                intent.putExtra("nombre_piso", nombrePiso)
                startActivity(intent)
            } else {
                Toast.makeText(this, "No se ha seleccionado ningún piso. Crea uno primero.", Toast.LENGTH_SHORT).show()
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

    override fun onResume(){
        super.onResume()
        mostrarSiguienteReserva()
        limpiarReservasPasadas()
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
        mostrarSiguienteReserva()

        lifecycleScope.launch {
            val reservas = repository.getAllReservas().toMutableList()

            if (reservas.isEmpty()) {
                Toast.makeText(this@activity_menu_creador, "No tienes reservas activas.", Toast.LENGTH_SHORT).show()
                return@launch
            }

            val reservasPorPiso = reservas.groupBy { it.piso }
            val dialogView = layoutInflater.inflate(R.layout.dialog_reservas, null)
            val contenedor = dialogView.findViewById<LinearLayout>(R.id.contenedor_reservas)

            lateinit var dialog: AlertDialog

            for ((piso, lista) in reservasPorPiso) {
                val pisoText = TextView(this@activity_menu_creador).apply {
                    text = piso
                    textSize = 18f
                    setPadding(0, 16, 0, 8)
                    setTextColor(Color.BLACK)
                    setTypeface(null, Typeface.BOLD)
                }
                contenedor.addView(pisoText)

                lista.forEach { reserva ->
                    val reservaText = TextView(this@activity_menu_creador).apply {
                        text = "- ${reserva.nombreSala}  ${reserva.fechaHora}"
                        setPadding(16, 4, 0, 4)
                        setTextColor(Color.DKGRAY)
                        setOnClickListener {
                            val confirmDialog = AlertDialog.Builder(this@activity_menu_creador)
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

                val divider = View(this@activity_menu_creador).apply {
                    setBackgroundColor(Color.LTGRAY)
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        2
                    ).apply { setMargins(0, 16, 0, 16) }
                }
                contenedor.addView(divider)
            }

            dialog = AlertDialog.Builder(this@activity_menu_creador)
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

    private fun mostrarSiguienteReserva() {
        val textView = findViewById<TextView>(R.id.textProximaReserva)
        val formato = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
        val ahora = Date()

        lifecycleScope.launch {
            val reservas = repository.getAllReservas()

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
                return@launch
            }

            val siguienteReserva = reservasFuturas.minByOrNull {
                formato.parse(it.fechaHora)?.time ?: Long.MAX_VALUE
            }

            siguienteReserva?.let {
                val texto = "Siguiente reserva: ${it.piso} \n${it.nombreSala} el ${it.fechaHora}"
                textView.text = texto
            }
        }
    }


}
