package com.example.tfg_kotlin

import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.Typeface
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.LinearLayout
import android.widget.NumberPicker
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.example.tfg_kotlin.BBDD_Global.Entities.Reserva
import com.example.tfg_kotlin.BBDD_Global.Entities.Sesion
import com.google.android.material.materialswitch.MaterialSwitch
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

/**
 * Clase base para MenuCreadorActivity y MenuEmpleadoActivity.
 * Contiene toda la lógica compartida: reservas, notificaciones,
 * limpieza de reservas pasadas y siguiente reserva.
 */
abstract class BaseMenuActivity : AppCompatActivity() {

    protected lateinit var firestore: FirebaseFirestore

    // Limpia reservas pasadas y muestra la siguiente al volver a la actividad
    override fun onResume() {
        super.onResume()
        limpiarReservasPasadas()
        mostrarSiguienteReserva()
    }

    // Infla el menú de la toolbar
    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_principal, menu)
        return true
    }

    // Opciones de la toolbar
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                onBackPressedDispatcher.onBackPressed()
                true
            }
            R.id.action_options -> {
                mostrarDialogoNotificaciones()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    // Solicitud de permisos para notificaciones
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 1001) {
            if (!(grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                Toast.makeText(this, "No se podrán mostrar notificaciones de reservas.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Diálogo de configuración de notificaciones
    protected fun mostrarDialogoNotificaciones() {
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

        dialog.window?.setBackgroundDrawableResource(R.drawable.dialog_background)
        dialog.show()
        dialog.getButton(AlertDialog.BUTTON_POSITIVE)
            ?.setTextColor(ContextCompat.getColor(this, R.color.black))
        dialog.getButton(AlertDialog.BUTTON_NEGATIVE)
            ?.setTextColor(ContextCompat.getColor(this, R.color.red))
    }

    // Diálogo para ver y cancelar reservas activas del usuario
    protected fun mostrarDialogoReservas() {
        limpiarReservasPasadas()
        val sesion = Sesion.datos
        val db = FirebaseFirestore.getInstance()
        val nombreEmpresa = sesion?.empresa?.nombre
        val uidUsuario = FirebaseAuth.getInstance().currentUser?.uid ?: ""

        if (uidUsuario.isBlank() || nombreEmpresa.isNullOrBlank()) {
            Toast.makeText(this, "Usuario o empresa no identificado", Toast.LENGTH_SHORT).show()
            return
        }

        val empresaId = nombreEmpresa

        lifecycleScope.launch {
            try {
                val snapshot = db.collection("empresas")
                    .document(empresaId)
                    .collection("reservas")
                    .get()
                    .await()

                val reservas = snapshot.documents.mapNotNull { doc ->
                    doc.toObject(Reserva::class.java)?.copy(id = doc.id)
                }

                val reservasUsuario = reservas.filter { it.idusuario == uidUsuario }

                if (reservasUsuario.isEmpty()) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(this@BaseMenuActivity, "No tienes reservas activas.", Toast.LENGTH_SHORT).show()
                    }
                    return@launch
                }

                val reservasPorPiso = reservasUsuario.groupBy { it.piso }

                withContext(Dispatchers.Main) {
                    val dialogView = layoutInflater.inflate(R.layout.dialog_reservas, null)
                    val contenedor = dialogView.findViewById<LinearLayout>(R.id.contenedor_reservas)
                    lateinit var dialog: AlertDialog

                    for ((piso, lista) in reservasPorPiso) {
                        val pisoText = TextView(this@BaseMenuActivity).apply {
                            text = piso
                            textSize = 18f
                            setPadding(0, 16, 0, 8)
                            setTextColor(Color.BLACK)
                            setTypeface(null, Typeface.BOLD)
                        }
                        contenedor.addView(pisoText)

                        lista.forEach { reserva ->
                            val reservaText = TextView(this@BaseMenuActivity).apply {
                                text = "- ${reserva.nombreSala}  ${reserva.fechaHora}"
                                setPadding(16, 4, 0, 4)
                                setTextColor(Color.DKGRAY)
                                setOnClickListener {
                                    val confirmDialog = AlertDialog.Builder(this@BaseMenuActivity)
                                        .setTitle("¿Cancelar reserva?")
                                        .setMessage("¿Deseas cancelar la reserva de '${reserva.nombreSala}' el ${reserva.fechaHora}?")
                                        .setPositiveButton("Sí") { _, _ ->
                                            lifecycleScope.launch {
                                                try {
                                                    reserva.id?.let { idDoc ->
                                                        db.collection("empresas")
                                                            .document(empresaId)
                                                            .collection("reservas")
                                                            .document(idDoc)
                                                            .delete()
                                                            .await()
                                                    }
                                                    dialog.dismiss()
                                                    mostrarDialogoReservas()
                                                    mostrarSiguienteReserva()
                                                } catch (e: Exception) {
                                                    withContext(Dispatchers.Main) {
                                                        Toast.makeText(this@BaseMenuActivity, "Error al eliminar la reserva", Toast.LENGTH_SHORT).show()
                                                    }
                                                }
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

                        val divider = View(this@BaseMenuActivity).apply {
                            setBackgroundColor(Color.LTGRAY)
                            layoutParams = LinearLayout.LayoutParams(
                                LinearLayout.LayoutParams.MATCH_PARENT, 2
                            ).apply { setMargins(0, 16, 0, 16) }
                        }
                        contenedor.addView(divider)
                    }

                    dialog = AlertDialog.Builder(this@BaseMenuActivity)
                        .setTitle("Tus reservas activas")
                        .setView(dialogView)
                        .setPositiveButton("Cerrar", null)
                        .create()

                    dialog.show()
                    dialog.window?.setBackgroundDrawableResource(R.drawable.dialog_background)
                    dialog.getButton(AlertDialog.BUTTON_POSITIVE)?.setTextColor(Color.BLACK)
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@BaseMenuActivity, "Error cargando reservas", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    // Elimina reservas cuya fecha ya ha pasado
    protected fun limpiarReservasPasadas() {
        val formato = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
        val sesion = Sesion.datos
        val nombreEmpresa = sesion?.empresa?.nombre
        if (nombreEmpresa.isNullOrBlank()) return

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val snapshot = firestore.collection("empresas")
                    .document(nombreEmpresa)
                    .collection("reservas")
                    .get()
                    .await()

                val reservas = snapshot.documents.mapNotNull { doc ->
                    doc.toObject(Reserva::class.java)?.copy(id = doc.id)
                }

                val reservasAntiguas = reservas.filter {
                    try {
                        val fechaReserva = formato.parse(it.fechaHora)
                        fechaReserva != null && fechaReserva.before(Date())
                    } catch (e: Exception) {
                        false
                    }
                }

                reservasAntiguas.forEach { reserva ->
                    reserva.id?.let { idDoc ->
                        firestore.collection("empresas")
                            .document(nombreEmpresa)
                            .collection("reservas")
                            .document(idDoc)
                            .delete()
                            .await()
                    }
                }
            } catch (_: Exception) { }
        }
    }

    // Muestra la siguiente reserva más cercana y programa notificación
    protected fun mostrarSiguienteReserva() {
        val textView = findViewById<TextView>(R.id.textProximaReserva)
        val formato = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
        val ahora = Date()

        val sesion = Sesion.datos
        val nombreEmpresa = sesion?.empresa?.nombre
        val uidUsuario = FirebaseAuth.getInstance().currentUser?.uid ?: ""

        if (uidUsuario.isBlank() || nombreEmpresa.isNullOrBlank()) {
            textView.text = "No hay usuario o empresa válidos"
            return
        }

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val snapshot = firestore.collection("empresas")
                    .document(nombreEmpresa)
                    .collection("reservas")
                    .whereEqualTo("idusuario", uidUsuario)
                    .get()
                    .await()

                val reservas = snapshot.documents.mapNotNull { doc ->
                    doc.toObject(Reserva::class.java)?.copy(id = doc.id)
                }

                val reservasFuturas = reservas.filter {
                    try {
                        val fechaReserva = formato.parse(it.fechaHora)
                        fechaReserva != null && fechaReserva.after(ahora)
                    } catch (e: Exception) {
                        false
                    }
                }

                if (reservasFuturas.isEmpty()) {
                    withContext(Dispatchers.Main) {
                        textView.text = "No hay reservas próximas"
                    }
                    return@launch
                }

                val siguienteReserva = reservasFuturas.minByOrNull {
                    formato.parse(it.fechaHora)?.time ?: Long.MAX_VALUE
                }

                siguienteReserva?.let { reserva ->
                    val texto = "Siguiente reserva: ${reserva.piso} \n${reserva.nombreSala} el ${reserva.fechaHora}"
                    withContext(Dispatchers.Main) {
                        textView.text = texto
                    }

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
                                val esJefe = Sesion.datos?.usuario?.esJefe ?: false
                                val inputData = Data.Builder()
                                    .putString("hora_reserva", reserva.fechaHora)
                                    .putString("nombre_sala", reserva.nombreSala)
                                    .putBoolean("es_jefe", esJefe)
                                    .build()

                                val workRequest = OneTimeWorkRequestBuilder<ReservaWorker>()
                                    .setInitialDelay(delay, TimeUnit.MILLISECONDS)
                                    .setInputData(inputData)
                                    .build()

                                val workName = "recordatorio_reserva_${reserva.nombreSala}_${reserva.fechaHora}"
                                WorkManager.getInstance(this@BaseMenuActivity)
                                    .enqueueUniqueWork(workName, ExistingWorkPolicy.REPLACE, workRequest)
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    textView.text = "Error al cargar reservas"
                }
            }
        }
    }
}
