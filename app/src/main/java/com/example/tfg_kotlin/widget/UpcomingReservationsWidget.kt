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
        val prefs = context.getSharedPreferences("widget_data", Context.MODE_PRIVATE)
        val lastSala = prefs.getString("last_sala", null)
        val lastPuesto = prefs.getString("last_puesto", null)

        // Estrategia "Cache-First": Pintamos inmediatamente la última info conocida
        // para que Nova Launcher vea un widget estable y no parpadee.
        for (appWidgetId in appWidgetIds) {
            val views = RemoteViews(context.packageName, R.layout.widget_upcoming_reservations)
            
            // Llenamos con caché si existe
            if (lastSala != null) views.setTextViewText(R.id.text_proxima_sala, lastSala)
            if (lastPuesto != null) views.setTextViewText(R.id.text_proximo_puesto, lastPuesto)
            
            updateStaticUI(context, views)
            appWidgetManager.updateAppWidget(appWidgetId, views)
        }

        // Luego disparamos el trabajador para refrescar los datos reales
        triggerWorker(context)
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == ACTION_REFRESH) {
            triggerWorker(context)
        }
    }

    private fun triggerWorker(context: Context) {
        // Evitamos peticiones redundantes (debounce manual)
        val prefs = context.getSharedPreferences("widget_data", Context.MODE_PRIVATE)
        val lastRequest = prefs.getLong("last_request_time", 0)
        val now = System.currentTimeMillis()
        
        if (now - lastRequest < 5000) return // Ignorar si fue hace menos de 5 seg

        prefs.edit().putLong("last_request_time", now).apply()

        try {
            val workRequest = OneTimeWorkRequestBuilder<ReservationsUpdateWorker>().build()
            WorkManager.getInstance(context).enqueueUniqueWork(
                "WidgetUpdateWork",
                androidx.work.ExistingWorkPolicy.REPLACE, // Usamos REPLACE para asegurar frescura
                workRequest
            )
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    companion object {
        const val ACTION_REFRESH = "com.example.tfg_kotlin.widget.ACTION_REFRESH"

        fun updateStaticUI(context: Context, views: RemoteViews) {
            // Restaurar el botón de refresco y el click general
            val refreshIntent = Intent(context, UpcomingReservationsWidget::class.java).apply {
                action = ACTION_REFRESH
            }
            val refreshPendingIntent = PendingIntent.getBroadcast(
                context, 0, refreshIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.btn_refresh, refreshPendingIntent)

            val appIntent = Intent(context, Principal::class.java)
            val appPendingIntent = PendingIntent.getActivity(
                context, 0, appIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_title, appPendingIntent)
        }
    }
}
