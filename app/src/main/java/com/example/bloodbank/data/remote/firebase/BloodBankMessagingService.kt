package com.example.bloodbank.data.remote.firebase

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.example.bloodbank.R
import com.example.bloodbank.presentation.main.MainActivity
import com.example.bloodbank.worker.SyncFcmTokenWorker
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlin.random.Random

/**
 * BloodBankMessagingService
 *
 * Handles incoming Firebase Cloud Messaging (FCM) push notifications.
 *
 * Declared in AndroidManifest.xml under <service> with
 * the MESSAGING_EVENT intent filter.
 */
class BloodBankMessagingService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        
        // Dispatch WorkManager to sync token reliably
        val data = Data.Builder()
            .putString(SyncFcmTokenWorker.KEY_TOKEN, token)
            .build()
            
        val request = OneTimeWorkRequestBuilder<SyncFcmTokenWorker>()
            .setInputData(data)
            .build()
            
        WorkManager.getInstance(applicationContext).enqueueUniqueWork(
            "SyncFcmTokenWork",
            ExistingWorkPolicy.REPLACE,
            request
        )
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)

        val title = message.notification?.title ?: message.data["title"] ?: "BloodBank"
        val body  = message.notification?.body  ?: message.data["body"]  ?: "You have a new message."
        val type  = message.data["type"]

        showNotification(title, body)
    }

    private fun showNotification(title: String, body: String) {
        val channelId = "bloodbank_notifications"
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Create Channel for Android O and above
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "General Notifications",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifications for blood requests and chats"
            }
            notificationManager.createNotificationChannel(channel)
        }

        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notification = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.ic_nav_home) // Use a valid vector icon here
            .setContentTitle(title)
            .setContentText(body)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()

        notificationManager.notify(Random.nextInt(), notification)
    }
}
