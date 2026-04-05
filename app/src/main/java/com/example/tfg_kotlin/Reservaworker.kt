package com.example.tfg_kotlin

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.annotation.RequiresPermission
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.Worker
import androidx.work.WorkerParameters
//import com.tuapp.trabajo.R // asegúrate de tener el icono en drawable

// Extensión para poder realizar accinoes en segundo plano
class ReservaWorker(context: Context, params: WorkerParameters) : Worker(context, params) {
    // Reqioere de permisos para mostrar notificaciones
    @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
    override fun doWork(): Result {
        // Obtiene la hora de la reserva
        val horaReserva = inputData.getString("hora_reserva") ?: return Result.failure()
        // Obtiene le nombre de la sala
        val sala = inputData.getString("nombre_sala") ?: "Sala"

        // Muestra notificaciones
        mostrarNotificacion("Recordatorio de reserva", "Tu reserva en $sala es a las $horaReserva.")
        return Result.success()
    }

    // Constructor de notificaiones
    @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
    private fun mostrarNotificacion(titulo: String, mensaje: String) {
        val channelId = "canal_reservas"

        // Crear canal (solo Android 8+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Recordatorios de Reserva",
                //Necesario para que suene o aparezace en pantalla
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notificaciones para recordar reservas de sala"
            }
            // Guarda el canal
            val manager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }

        // Crear intent que abre la Activity correcta según el rol del usuario
        val esJefe = inputData.getBoolean("es_jefe", false)
        val targetClass = if (esJefe) MenuCreadorActivity::class.java else MenuEmpleadoActivity::class.java
        val intent = Intent(applicationContext, targetClass).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("lanzada_desde_notificacion", true) // si necesitas pasar algo
        }

        // Requerido para las notificaciones
        val pendingIntent = PendingIntent.getActivity(
            applicationContext,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE // FLAG_IMMUTABLE es obligatorio en Android 12+
        )

        // Creación de las nortificaciones con el firmato deseado
        val notification = NotificationCompat.Builder(applicationContext, channelId)
            .setSmallIcon(R.drawable.ic_reserva)
            .setContentTitle(titulo)
            .setContentText(mensaje)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        // Muestra las notificaciones
        val manager = NotificationManagerCompat.from(applicationContext)
        manager.notify(System.currentTimeMillis().toInt(), notification)
    }

}