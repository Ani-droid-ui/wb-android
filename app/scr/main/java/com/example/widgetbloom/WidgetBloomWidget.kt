package com.example.widgetbloom

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import java.text.SimpleDateFormat
import java.util.*

class WidgetBloomWidget : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }

    override fun onEnabled(context: Context) {
        // Widget first created
    }

    override fun onDisabled(context: Context) {
        // Last widget removed
    }

    companion object {
        fun updateAppWidget(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetId: Int
        ) {
            val affirmations = listOf(
                "I am enough",
                "I radiate positivity",
                "I deserve peace",
                "I am growing every day",
                "I am proud of who I am"
            )

            // Get current time
            val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
            val currentTime = timeFormat.format(Date())

            // Get current date
            val dateFormat = SimpleDateFormat("EEEE, MMM d", Locale.getDefault())
            val currentDate = dateFormat.format(Date())

            // Random affirmation
            val affirmation = affirmations.random()

            // Create RemoteViews
            val views = RemoteViews(context.packageName, R.layout.widget_layout)
            views.setTextViewText(R.id.widget_clock, currentTime)
            views.setTextViewText(R.id.widget_date, currentDate)
            views.setTextViewText(R.id.widget_affirmation, affirmation)

            // Click to open main app
            val intent = Intent(context, MainActivity::class.java)
            val pendingIntent = PendingIntent.getActivity(
                context, 
                0, 
                intent, 
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_root, pendingIntent)

            // Update widget
            appWidgetManager.updateAppWidget(appWidgetId, views)
        }
    }
}
