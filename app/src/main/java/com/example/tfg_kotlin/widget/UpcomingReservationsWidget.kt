package com.example.tfg_kotlin.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.example.tfg_kotlin.Principal
import com.example.tfg_kotlin.R

class UpcomingReservationsWidget : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        // Enviar UI inicial para que el launcher no falle ("widget loading...")
        for (appWidgetId in appWidgetIds) {
            val views = RemoteViews(context.packageName, R.layout.widget_upcoming_reservations)
            updateStaticUI(context, views)
            appWidgetManager.updateAppWidget(appWidgetId, views)
        }

        // Enviar tarea a WorkManager para actualización de datos reales en segundo plano
        triggerWorker(context)
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == ACTION_REFRESH) {
            triggerWorker(context)
        }
    }

    private fun triggerWorker(context: Context) {
        try {
            val workRequest = OneTimeWorkRequestBuilder<ReservationsUpdateWorker>().build()
            WorkManager.getInstance(context).enqueueUniqueWork(
                "WidgetUpdateWork",
                androidx.work.ExistingWorkPolicy.KEEP,
                workRequest
            )
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    companion object {
        const val ACTION_REFRESH = "com.example.tfg_kotlin.widget.ACTION_REFRESH"

        /**
         * Actualiza la UI visual básica que no depende de la lista (Título, Botones, Intents)
         */
        fun updateStaticUI(context: Context, views: RemoteViews) {
            // Click en el botón de actualización
            val refreshIntent = Intent(context, UpcomingReservationsWidget::class.java)
            refreshIntent.action = ACTION_REFRESH
            val refreshPendingIntent = PendingIntent.getBroadcast(
                context, 0, refreshIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.btn_refresh, refreshPendingIntent)

            // Click general para abrir la app
            val appIntent = Intent(context, Principal::class.java)
            val appPendingIntent = PendingIntent.getActivity(
                context, 0, appIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            
            views.setOnClickPendingIntent(R.id.widget_title, appPendingIntent)
            // Se han eliminado los `setOnClickPendingIntent` de las secciones para prevenir el parpadeo
            // visual en lanzadores que reiteran el foco de RemoteViews.
        }
    }
}
