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
import androidx.core.content.edit
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class activity_menu_empleado : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_menu_empleado)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.menuempleado)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        limpiarReservasPasadas()
        mostrarSiguienteReserva()


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
                        val confirmDialog = AlertDialog.Builder(this@activity_menu_empleado)
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
