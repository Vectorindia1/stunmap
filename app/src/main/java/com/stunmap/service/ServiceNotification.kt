package com.stunmap.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.content.getSystemService
import com.stunmap.R
import com.stunmap.ui.MainActivity
import com.stunmap.util.Constants

object ServiceNotification {

    fun createChannel(context: Context) {
        val channel = NotificationChannel(
            Constants.NOTIFICATION_CHANNEL_ID,
            "STUNMAP Capture",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Active network capture session"
            setShowBadge(false)
        }
        context.getSystemService<NotificationManager>()?.createNotificationChannel(channel)
    }

    fun build(context: Context): Notification {
        val openIntent = PendingIntent.getActivity(
            context, 0,
            Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val stopIntent = PendingIntent.getService(
            context, 1,
            Intent(context, StunCaptureService::class.java).apply {
                action = StunCaptureService.ACTION_STOP
            },
            PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(context, Constants.NOTIFICATION_CHANNEL_ID)
            .setContentTitle("STUNMAP Active")
            .setContentText("Capturing STUN traffic...")
            .setSmallIcon(R.drawable.ic_radar)
            .setContentIntent(openIntent)
            .setOngoing(true)
            .addAction(R.drawable.ic_stop, "Stop", stopIntent)
            .setSilent(true)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .build()
    }
}
