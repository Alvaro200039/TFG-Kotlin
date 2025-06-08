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

class ReservaWorker(context: Context, params: WorkerParameters) : Worker(context, params) {
    @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
    override fun doWork(): Result {
        val horaReserva = inputData.getString("hora_reserva") ?: return Result.failure()
        val sala = inputData.getString("nombre_sala") ?: "Sala"

        mostrarNotificacion("Recordatorio de reserva", "Tu reserva en $sala es a las $horaReserva.")
        return Result.success()
    }

    @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
    private fun mostrarNotificacion(titulo: String, mensaje: String) {
        val channelId = "canal_reservas"

        // Crear canal (solo Android 8+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Recordatorios de Reserva",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notificaciones para recordar reservas de sala"
            }
            val manager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }

        // 👉 Crear intent que abre la Activity deseada
        val intent = Intent(applicationContext, Activity_menu_creador::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("lanzada_desde_notificacion", true) // si necesitas pasar algo
        }

        val pendingIntent = PendingIntent.getActivity(
            applicationContext,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE // FLAG_IMMUTABLE es obligatorio en Android 12+
        )

        val notification = NotificationCompat.Builder(applicationContext, channelId)
            .setSmallIcon(R.drawable.ic_reserva)
            .setContentTitle(titulo)
            .setContentText(mensaje)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        val manager = NotificationManagerCompat.from(applicationContext)
        manager.notify(System.currentTimeMillis().toInt(), notification)
    }

}