package com.qdash.core.utils

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.qdash.MainActivity
import com.qdash.domain.model.AppNotification

object SystemNotificationHelper {
    private const val CHANNEL_ID = "kdach_alerts"
    private const val CHANNEL_NAME = "تنبيهات قداشّ"
    private const val CHANNEL_DESC = "تنبيهات الميزانية، الديون والمدخرات"
    const val EXTRA_DEEP_LINK_ROUTE = "extra_deep_link_route"
    const val EXTRA_NOTIFICATION_ID = "extra_notification_id"

    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel(CHANNEL_ID, CHANNEL_NAME, importance).apply {
                description = CHANNEL_DESC
            }
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun showNotification(context: Context, appNotification: AppNotification) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            appNotification.deepLinkRoute?.let { putExtra(EXTRA_DEEP_LINK_ROUTE, it) }
            if (appNotification.id > 0L) {
                putExtra(EXTRA_NOTIFICATION_ID, appNotification.id)
            }
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            appNotification.id.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Using standard material notification building
        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(com.qdash.R.drawable.ic_notification)
            .setContentTitle(appNotification.title)
            .setContentText(appNotification.message)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                if (context.checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) == android.content.pm.PackageManager.PERMISSION_GRANTED) {
                    NotificationManagerCompat.from(context).notify(appNotification.id.toInt(), builder.build())
                }
            } else {
                NotificationManagerCompat.from(context).notify(appNotification.id.toInt(), builder.build())
            }
        } catch (e: SecurityException) {
            e.printStackTrace()
        }
    }
}
