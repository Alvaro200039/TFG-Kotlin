package com.example.tfg_kotlin.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.view.View
import android.widget.RemoteViews
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.tfg_kotlin.Principal
import com.example.tfg_kotlin.R
import com.example.tfg_kotlin.data.model.Reserva
import com.example.tfg_kotlin.data.model.TipoElemento
import com.example.tfg_kotlin.data.repository.ReservationRepository
import com.example.tfg_kotlin.util.DateFormats
import java.util.Date

class ReservationsUpdateWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    private val repository = ReservationRepository()

    override suspend fun doWork(): Result {
        val prefs = applicationContext.getSharedPreferences("widget_prefs", Context.MODE_PRIVATE)
        val userId = prefs.getString("user_id", null)
        val empresaId = prefs.getString("empresa_id", null)

        val appWidgetManager = AppWidgetManager.getInstance(applicationContext)
        val componentName = ComponentName(applicationContext, UpcomingReservationsWidget::class.java)
        val appWidgetIds = appWidgetManager.getAppWidgetIds(componentName)

        if (userId == null || empresaId == null) {
            for (id in appWidgetIds) updateErrorState(id, appWidgetManager)
            return Result.success()
        }

        try {
            val allReservations = repository.getReservationsByUser(empresaId, userId)
            val now = Date()
            val format = DateFormats.fullFormat
            
            val upcoming = allReservations.filter { reserva ->
                try {
                    val parts = reserva.fechaHora.split(" - ")
                    val datePart = if (parts.isNotEmpty()) parts[0].split(" ")[0] else ""
                    val endTimeStr = if (parts.size == 2) parts[1].trim() else null
                    val reservaEnd = if (endTimeStr != null) {
                        format.parse("$datePart $endTimeStr")
                    } else {
                        format.parse(reserva.fechaHora)
                    }
                    reservaEnd != null && reservaEnd.after(now)
                } catch (e: Exception) {
                    true
                }
            }.sortedBy { it.fechaHora }

            val nextSala = upcoming.firstOrNull { it.tipo.uppercase() == "SALA" }
            val nextPuesto = upcoming.firstOrNull { it.tipo.uppercase() == "PUESTO" }

            // Actualizar cada instancia del widget con los datos obtenidos
            for (appWidgetId in appWidgetIds) {
                updateWidgetUI(appWidgetId, appWidgetManager, nextSala, nextPuesto)
            }

        } catch (e: Exception) {
            return Result.failure()
        }

        return Result.success()
    }

    private fun updateWidgetUI(
        appWidgetId: Int, 
        appWidgetManager: AppWidgetManager, 
        sala: Reserva?, 
        puesto: Reserva?
    ) {
        val views = RemoteViews(applicationContext.packageName, R.layout.widget_upcoming_reservations)
        
        // Setup Estático (Botones, Click Intents)
        UpcomingReservationsWidget.updateStaticUI(applicationContext, views)

        val prefs = applicationContext.getSharedPreferences("widget_data", Context.MODE_PRIVATE)
        val editor = prefs.edit()

        // Actualizar datos de Sala
        val salaText = if (sala != null) {
            val timeRange = sala.fechaHora.substringAfter(" ")
            "${sala.nombreSala} - $timeRange"
        } else {
            applicationContext.getString(R.string.widget_no_reservations)
        }
        views.setTextViewText(R.id.text_proxima_sala, salaText)
        editor.putString("last_sala", salaText)

        // Actualizar datos de Puesto
        val puestoText = if (puesto != null) {
            "${puesto.piso} - Puesto ${puesto.nombreSala}"
        } else {
            applicationContext.getString(R.string.widget_no_reservations)
        }
        views.setTextViewText(R.id.text_proximo_puesto, puestoText)
        editor.putString("last_puesto", puestoText)
        
        editor.apply()

        // Asegurar que las secciones son visibles y ocultar error
        views.setViewVisibility(R.id.section_proxima_sala, View.VISIBLE)
        views.setViewVisibility(R.id.section_proximo_puesto, View.VISIBLE)
        views.setViewVisibility(R.id.widget_error_view, View.GONE)

        appWidgetManager.updateAppWidget(appWidgetId, views)
    }

    private fun updateErrorState(appWidgetId: Int, appWidgetManager: AppWidgetManager) {
        val views = RemoteViews(applicationContext.packageName, R.layout.widget_upcoming_reservations)
        UpcomingReservationsWidget.updateStaticUI(applicationContext, views)
        
        views.setViewVisibility(R.id.section_proxima_sala, View.GONE)
        views.setViewVisibility(R.id.section_proximo_puesto, View.GONE)
        views.setViewVisibility(R.id.widget_error_view, View.VISIBLE)
        
        appWidgetManager.updateAppWidget(appWidgetId, views)
    }
}
