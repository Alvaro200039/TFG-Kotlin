package com.example.tfg_kotlin

import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.widget.NumberPicker
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.example.tfg_kotlin.data.model.Reserva
import com.example.tfg_kotlin.data.model.Sesion
import com.example.tfg_kotlin.data.repository.FirestoreRepository
import com.example.tfg_kotlin.ui.viewmodel.MenuViewModel
import com.google.android.material.materialswitch.MaterialSwitch
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.concurrent.TimeUnit

abstract class BaseMenuActivity : AppCompatActivity() {

    protected val firestore: FirebaseFirestore by lazy { FirebaseFirestore.getInstance() }
    protected val firestoreRepo: FirestoreRepository by lazy { FirestoreRepository(firestore) }

    abstract val menuViewModel: MenuViewModel

    override fun onResume() {
        super.onResume()
        menuViewModel.loadNextReserva()
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
                mostrarDialogoNotificaciones()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    protected fun setupBaseObservers(textNextReserva: TextView?) {
        menuViewModel.nextReserva.observe(this) { reserva ->
            if (reserva != null) {
                textNextReserva?.text = getString(R.string.msg_siguiente_reserva, reserva.piso, reserva.nombreSala, reserva.fechaHora)
                scheduleNotification(reserva)
            } else {
                textNextReserva?.text = getString(R.string.msg_no_hay_reservas_proximas)
            }
        }
    }

    private fun scheduleNotification(reserva: Reserva) {
        val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
        try {
            val fecha = sdf.parse(reserva.fechaHora) ?: return
            val diff = fecha.time - System.currentTimeMillis()

            val prefs = getSharedPreferences("ajustes_usuario", MODE_PRIVATE)
            if (prefs.getBoolean("notificaciones_activadas", true)) {
                val mins = prefs.getInt("minutos_antes", 10)
                val delay = diff - TimeUnit.MINUTES.toMillis(mins.toLong())

                if (delay > 0) {
                    val data = Data.Builder()
                        .putString("hora_reserva", reserva.fechaHora)
                        .putString("nombre_sala", reserva.nombreSala)
                        .putBoolean("es_jefe", Sesion.datos?.usuario?.esJefe ?: false)
                        .build()

                    val work = OneTimeWorkRequestBuilder<ReservaWorker>()
                        .setInitialDelay(delay, TimeUnit.MILLISECONDS)
                        .setInputData(data)
                        .build()

                    WorkManager.getInstance(this).enqueueUniqueWork(
                        "notif_${reserva.id}", 
                        ExistingWorkPolicy.REPLACE, 
                        work
                    )
                }
            }
        } catch (_: Exception) {}
    }

    protected fun mostrarDialogoNotificaciones() {
        val view = LayoutInflater.from(this).inflate(R.layout.dialog_notificaciones, null)
        val sw = view.findViewById<MaterialSwitch>(R.id.switchNotificaciones)
        val pk = view.findViewById<NumberPicker>(R.id.pickerMinutos)

        val prefs = getSharedPreferences("ajustes_usuario", MODE_PRIVATE)
        sw.isChecked = prefs.getBoolean("notificaciones_activadas", true)
        pk.minValue = 1
        pk.maxValue = 60
        pk.value = prefs.getInt("minutos_antes", 10)

        AlertDialog.Builder(this)
            .setTitle(R.string.title_notificaciones)
            .setView(view)
            .setPositiveButton(R.string.btn_guardar) { _, _ ->
                prefs.edit().apply {
                    putBoolean("notificaciones_activadas", sw.isChecked)
                    putInt("minutos_antes", pk.value)
                    apply()
                }
                menuViewModel.loadNextReserva() // Refresh to update scheduled notification
            }
            .setNegativeButton(R.string.btn_cancelar, null)
            .show()
    }

    protected fun mostrarDialogoReservas() {
        menuViewModel.userReservas.observe(this) { reservas ->
            if (reservas.isEmpty()) {
                Toast.makeText(this, R.string.msg_no_reservas_activas, Toast.LENGTH_SHORT).show()
                return@observe
            }

            val view = layoutInflater.inflate(R.layout.dialog_reservas, null)
            val container = view.findViewById<android.widget.LinearLayout>(R.id.contenedor_reservas)
            
            reservas.groupBy { it.piso }.forEach { (piso, list) ->
                container.addView(TextView(this).apply {
                    text = piso
                    textSize = 18f
                    setTypeface(null, android.graphics.Typeface.BOLD)
                    setPadding(0, 16, 0, 8)
                    setTextColor(android.graphics.Color.BLACK)
                })

                list.forEach { res ->
                    container.addView(TextView(this).apply {
                        text = getString(R.string.reserva_item_format, res.nombreSala, res.fechaHora)
                        setPadding(16, 4, 0, 4)
                        setOnClickListener {
                            AlertDialog.Builder(this@BaseMenuActivity)
                                .setTitle(R.string.title_cancelar_reserva)
                                .setMessage(getString(R.string.msg_confirmar_cancelar_reserva, res.nombreSala, res.fechaHora))
                                .setPositiveButton(R.string.btn_si) { _, _ -> menuViewModel.cancelReserva(res.id ?: "") }
                                .setNegativeButton(R.string.btn_no, null)
                                .show()
                        }
                    })
                }
            }

            AlertDialog.Builder(this)
                .setTitle(R.string.title_tus_reservas)
                .setView(view)
                .setPositiveButton(R.string.btn_cerrar, null)
                .show()
        }
    }
}
