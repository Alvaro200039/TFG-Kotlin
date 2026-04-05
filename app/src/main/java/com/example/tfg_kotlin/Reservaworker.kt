package com.example.tfg_kotlin

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.annotation.RequiresPermission
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.Worker
import androidx.work.WorkerParameters

// Extensión para poder realizar acciones en segundo plano
class ReservaWorker(context: Context, params: WorkerParameters) : Worker(context, params) {
    // Requiere de permisos para mostrar notificaciones
    @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
    override fun doWork(): Result {
        // Obtiene la hora de la reserva
        val horaReserva = inputData.getString("hora_reserva") ?: return Result.failure()
        // Obtiene el nombre de la sala
        val sala = inputData.getString("nombre_sala") ?: "Sala"

        // Muestra notificaciones
        mostrarNotificacion(
            applicationContext.getString(R.string.title_recordatorio_reserva),
            applicationContext.getString(R.string.msg_recordatorio_reserva, sala, horaReserva)
        )
        return Result.success()
    }

    // Constructor de notificaciones
    @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
    private fun mostrarNotificacion(titulo: String, mensaje: String) {
        val channelId = "canal_reservas"

        // Crear canal
        val channel = NotificationChannel(
            channelId,
            applicationContext.getString(R.string.title_recordatorios_reserva_channel),
            // Necesario para que suene o aparezca en pantalla
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = applicationContext.getString(R.string.desc_canal_reservas)
        }
        // Guarda el canal
        val manager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.createNotificationChannel(channel)

        // Crear intent que abre la Activity correcta según el rol del usuario
        val esJefe = inputData.getBoolean("es_jefe", false)
        val targetClass = if (esJefe) MenuCreadorActivity::class.java else MenuEmpleadoActivity::class.java
        val intent = Intent(applicationContext, targetClass).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("lanzada_desde_notificacion", true)
        }

        // Requerido para las notificaciones
        val pendingIntent = PendingIntent.getActivity(
            applicationContext,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Creación de las notificaciones con el formato deseado
        val notification = NotificationCompat.Builder(applicationContext, channelId)
            .setSmallIcon(R.drawable.ic_reserva)
            .setContentTitle(titulo)
            .setContentText(mensaje)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        // Muestra las notificaciones
        val notificationManager = NotificationManagerCompat.from(applicationContext)
        notificationManager.notify(System.currentTimeMillis().toInt(), notification)
    }

}