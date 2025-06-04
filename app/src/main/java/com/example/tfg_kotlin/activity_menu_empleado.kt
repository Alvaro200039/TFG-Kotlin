package com.example.tfg_kotlin

import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import android.content.pm.PackageManager
import android.view.LayoutInflater
import android.view.Menu
import android.widget.NumberPicker
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.example.tfg_kotlin.BBDD_Global.Entities.Reserva
import com.example.tfg_kotlin.BBDD_Global.Entities.Usuario
import java.util.concurrent.TimeUnit
import com.google.android.material.materialswitch.MaterialSwitch
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.toObject
import kotlinx.coroutines.tasks.await

class activity_menu_empleado : AppCompatActivity() {

    private lateinit var firestore: FirebaseFirestore
    private var idUsuario: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_menu_creador)

        enableEdgeToEdge()
        setContentView(R.layout.activity_menu_empleado)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.menucreacion)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()

        val currentUser = auth.currentUser
        if (currentUser == null) {
            Toast.makeText(this, "No hay usuario logueado", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        idUsuario = currentUser.uid  // asignamos directamente el UID string

        val cifUsuario = intent.getStringExtra("cifUsuario") ?: ""

        if (cifUsuario.isEmpty()) {
            Toast.makeText(this, "CIF no recibido", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        firestore.collection("empresas")
            .document(cifUsuario)
            .collection("usuarios")
            .document(currentUser.uid)
            .get()
            .addOnSuccessListener { documentSnapshot ->
                if (documentSnapshot.exists()) {
                    val usuario = documentSnapshot.toObject<Usuario>()
                    if (usuario != null) {
                        Toast.makeText(this, "Hola ${usuario.nombre}", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(this, "Usuario no encontrado", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(this, "Usuario no existe en esta empresa", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error cargando usuario: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }


    override fun onResume(){
        super.onResume()
        limpiarReservasPasadas()
        mostrarSiguienteReserva()
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

        dialog.getButton(AlertDialog.BUTTON_POSITIVE)
            ?.setTextColor(ContextCompat.getColor(this, R.color.black))
        dialog.getButton(AlertDialog.BUTTON_NEGATIVE)
            ?.setTextColor(ContextCompat.getColor(this, R.color.red))
    }


    private fun mostrarDialogoReservas() {
        limpiarReservasPasadas()

        val uidUsuario = FirebaseAuth.getInstance().currentUser?.uid ?: ""

        if (uidUsuario.isEmpty()) {
            Toast.makeText(this, "Usuario no identificado", Toast.LENGTH_SHORT).show()
            return
        }

        lifecycleScope.launch {
            try {
                val snapshot = firestore.collection("reservas").get().await()
                val reservas = snapshot.documents.mapNotNull { doc ->
                    doc.toObject(Reserva::class.java)?.copy(id = doc.id)
                }

                // Filtrar por UID Firebase (String)
                val reservasUsuario = reservas.filter { it.idusuario == uidUsuario }

                if (reservasUsuario.isEmpty()) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(
                            this@activity_menu_empleado,
                            "No tienes reservas activas.",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                    return@launch
                }

                val reservasPorPiso = reservasUsuario.groupBy { it.piso }

                withContext(Dispatchers.Main) {
                    val dialogView = layoutInflater.inflate(R.layout.dialog_reservas, null)
                    val contenedor = dialogView.findViewById<LinearLayout>(R.id.contenedor_reservas)

                    lateinit var dialog: AlertDialog

                    for ((piso, lista) in reservasPorPiso) {
                        val pisoText = TextView(this@activity_menu_empleado).apply {
                            text = piso
                            textSize = 18f
                            setPadding(0, 16, 0, 8)
                            setTextColor(Color.BLACK)
                            setTypeface(null, Typeface.BOLD)
                        }
                        contenedor.addView(pisoText)

                        lista.forEach { reserva ->
                            val reservaText = TextView(this@activity_menu_empleado).apply {
                                text = "- ${reserva.nombreSala}  ${reserva.fechaHora}"
                                setPadding(16, 4, 0, 4)
                                setTextColor(Color.DKGRAY)
                                setOnClickListener {
                                    val confirmDialog = AlertDialog.Builder(this@activity_menu_empleado)
                                        .setTitle("¿Cancelar reserva?")
                                        .setMessage("¿Deseas cancelar la reserva de '${reserva.nombreSala}' el ${reserva.fechaHora}?")
                                        .setPositiveButton("Sí") { _, _ ->
                                            lifecycleScope.launch {
                                                try {
                                                    reserva.id?.let { idDoc ->
                                                        firestore.collection("reservas")
                                                            .document(idDoc)
                                                            .delete()
                                                            .await()
                                                    }
                                                    dialog.dismiss()
                                                    mostrarDialogoReservas() // Recargar
                                                } catch (e: Exception) {
                                                    withContext(Dispatchers.Main) {
                                                        Toast.makeText(
                                                            this@activity_menu_empleado,
                                                            "Error al eliminar la reserva",
                                                            Toast.LENGTH_SHORT
                                                        ).show()
                                                    }
                                                }
                                            }
                                        }
                                        .setNegativeButton("No", null)
                                        .create()

                                    confirmDialog.setOnShowListener {
                                        confirmDialog.getButton(AlertDialog.BUTTON_POSITIVE)
                                            ?.setTextColor(Color.BLACK)
                                        confirmDialog.getButton(AlertDialog.BUTTON_NEGATIVE)
                                            ?.setTextColor(Color.RED)
                                    }
                                    confirmDialog.window?.setBackgroundDrawableResource(R.drawable.dialog_background)
                                    confirmDialog.show()
                                }
                            }
                            contenedor.addView(reservaText)
                        }

                        val divider = View(this@activity_menu_empleado).apply {
                            setBackgroundColor(Color.LTGRAY)
                            layoutParams = LinearLayout.LayoutParams(
                                LinearLayout.LayoutParams.MATCH_PARENT,
                                2
                            ).apply { setMargins(0, 16, 0, 16) }
                        }
                        contenedor.addView(divider)
                    }

                    dialog = AlertDialog.Builder(this@activity_menu_empleado)
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
                    Toast.makeText(this@activity_menu_empleado, "Error cargando reservas", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun limpiarReservasPasadas() {
        val formato = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val snapshot = firestore.collection("reservas").get().await()
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
                        firestore.collection("reservas").document(idDoc).delete().await()
                    }
                }
            } catch (e: Exception) {
                // Opcional: Log.e("limpiarReservasPasadas", "Error al limpiar reservas", e)
            }
        }
    }

    private fun mostrarSiguienteReserva() {
        val textView = findViewById<TextView>(R.id.textProximaReserva)
        val formato = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
        val ahora = Date()

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val snapshot = firestore.collection("reservas")
                    .whereEqualTo("idusuario", idUsuario)  // idUsuario debe ser String
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
                                val inputData = Data.Builder()
                                    .putString("hora_reserva", reserva.fechaHora)
                                    .putString("nombre_sala", reserva.nombreSala)
                                    .build()

                                val workRequest = OneTimeWorkRequestBuilder<ReservaWorker>()
                                    .setInitialDelay(delay, TimeUnit.MILLISECONDS)
                                    .setInputData(inputData)
                                    .build()

                                val workName = "recordatorio_reserva_${reserva.nombreSala}_${reserva.fechaHora}"

                                WorkManager.getInstance(this@activity_menu_empleado)
                                    .enqueueUniqueWork(
                                        workName,
                                        ExistingWorkPolicy.REPLACE,
                                        workRequest
                                    )
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
