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
import androidx.lifecycle.lifecycleScope
import com.example.tfg_kotlin.BBDD_Global.Database.GlobalDB
import com.example.tfg_kotlin.BBDD_Maestra.Database.MasterDB
import com.example.tfg_kotlin.repository.GlobalRepository
import com.example.tfg_kotlin.repository.MasterRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import android.content.pm.PackageManager
import android.Manifest
import android.os.Build
import android.view.LayoutInflater
import android.view.Menu
import android.widget.NumberPicker
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit
import com.google.android.material.materialswitch.MaterialSwitch



class activity_menu_creador : AppCompatActivity() {

    private lateinit var repositoryApp: GlobalRepository
    private lateinit var repositoryMaster: MasterRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val masterDb = MasterDB.getDatabase(applicationContext)
        repositoryMaster = MasterRepository(
            masterDb.empresaDao()
        )

        val db = GlobalDB.getDatabase(applicationContext)
        repositoryApp = GlobalRepository(
            db.usuarioDao(),
            db.salaDao(),
            db.reservaDao(),
            db.franjahorariaDao(),
            db.pisoDao()
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
            lifecycleScope.launch {
                val pisos = repositoryApp.pisoDao.obtenerTodosLosPisos().first()

                if (pisos.isNotEmpty()) {
                    val pisoSeleccionado = pisos.last() // O el que quieras, aquí el último piso
                    val nombrePiso = pisoSeleccionado.nombre

                    val intent = Intent(this@activity_menu_creador, Activity_empleados::class.java)
                    intent.putExtra("nombre_piso", nombrePiso)
                    startActivity(intent)
                } else {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(
                            this@activity_menu_creador,
                            "No se ha creado ningún piso. Crea uno primero.",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
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



        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                    1001
                )
            }
        }
    }

    override fun onResume(){
        super.onResume()
        mostrarSiguienteReserva()
        limpiarReservasPasadas()
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_principal, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                onBackPressedDispatcher.onBackPressed()
                true
            }
            R.id.action_options -> {
                mostrarDialogoNotificaciones() // tu función para mostrar el diálogo
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }


    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == 1001) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permiso concedido
            } else {
                Toast.makeText(this, "No se podrán mostrar notificaciones de reservas.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun mostrarDialogoNotificaciones() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_notificaciones, null)

        val switch = dialogView.findViewById<MaterialSwitch>(R.id.switchNotificaciones)
        val picker = dialogView.findViewById<NumberPicker>(R.id.pickerMinutos)

        val prefs = getSharedPreferences("ajustes_usuario", MODE_PRIVATE)
        val notificacionesActivadas = prefs.getBoolean("notificaciones_activadas", true)
        val minutosAntes = prefs.getInt("minutos_antes", 10)

        switch.isChecked = notificacionesActivadas
        picker.minValue = 1
        picker.maxValue = 60
        picker.value = minutosAntes

        val dialog = AlertDialog.Builder(this)
            .setTitle("Notificaciones")
            .setView(dialogView)
            .setPositiveButton("Guardar") { _, _ ->
                val editor = prefs.edit()
                editor.putBoolean("notificaciones_activadas", switch.isChecked)
                editor.putInt("minutos_antes", picker.value)
                editor.apply()
                Toast.makeText(this, "Preferencias guardadas", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancelar", null)
            .create()

        // Fondo transparente para que solo se vea tu diseño personalizado
        dialog.window?.setBackgroundDrawableResource(R.drawable.dialog_background)

        dialog.show()

        dialog.getButton(AlertDialog.BUTTON_POSITIVE)?.setTextColor(ContextCompat.getColor(this, R.color.black))
        dialog.getButton(AlertDialog.BUTTON_NEGATIVE)?.setTextColor(ContextCompat.getColor(this, R.color.red))
    }


    private fun mostrarDialogoReservas() {
        limpiarReservasPasadas()
        mostrarSiguienteReserva()

        val idUsuario = 123 // Igual que en reservarSala, o cambia para obtenerlo dinámico

        if (idUsuario == -1) {
            Toast.makeText(this, "Usuario no identificado", Toast.LENGTH_SHORT).show()
            return
        }

        lifecycleScope.launch {
            val usuarioActual = repositoryApp.usuarioDao.getUsuarioById(idUsuario)
            val nombreUsuario = usuarioActual?.nombre ?: "Juan"

            val reservas = repositoryApp.getAllReservas().toMutableList()

            // Filtrar solo las reservas del usuario actual
            val reservasUsuario = reservas.filter { it.idusuario == idUsuario }

            if (reservasUsuario.isEmpty()) {
                Toast.makeText(this@activity_menu_creador, "No tienes reservas activas.", Toast.LENGTH_SHORT).show()
                return@launch
            }

            val reservasPorPiso = reservasUsuario.groupBy { it.piso }
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
                                        repositoryApp.eliminarReserva(reserva)

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
            repositoryApp.limpiarReservasAntiguas(ahora)
        }
    }

    private fun mostrarSiguienteReserva() {
        val textView = findViewById<TextView>(R.id.textProximaReserva)
        val formato = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
        val ahora = Date()
        val idUsuario = 123

        lifecycleScope.launch {
            val reservas = repositoryApp.getReservasPorUsuario(idUsuario)

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

            siguienteReserva?.let { reserva ->
                val texto =
                    "Siguiente reserva: ${reserva.piso} \n${reserva.nombreSala} el ${reserva.fechaHora}"
                textView.text = texto

                val fechaReserva = formato.parse(reserva.fechaHora)
                fechaReserva?.let { fecha ->
                    val tiempoRestante = fecha.time - System.currentTimeMillis()

                    val prefs = getSharedPreferences("ajustes_usuario", MODE_PRIVATE)
                    val notificacionesActivadas = prefs.getBoolean("notificaciones_activadas", true)
                    val minutosAntes = prefs.getInt("minutos_antes", 10)

                    if (notificacionesActivadas) {
                        val tiempoAntes = TimeUnit.MINUTES.toMillis(minutosAntes.toLong())
                        val delay = tiempoRestante - tiempoAntes

                        if (delay > 0) {
                            val inputData = Data.Builder()
                                .putString("hora_reserva", reserva.fechaHora)
                                .putString("nombre_sala", reserva.nombreSala)
                                .build()

                            val workRequest = OneTimeWorkRequestBuilder<ReservaWorker>()
                                .setInitialDelay(delay, TimeUnit.MILLISECONDS)
                                .setInputData(inputData)
                                .build()

                            val workName = "recordatorio_reserva_${reserva.nombreSala}_${reserva.fechaHora}"

                            WorkManager.getInstance(this@activity_menu_creador).enqueueUniqueWork(
                                workName,
                                ExistingWorkPolicy.REPLACE,
                                workRequest
                            )
                        }
                    }
                }
            }
        }
    }
}
