package com.example.cpn

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.compose.ui.graphics.Color
import androidx.core.app.NotificationCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.google.firebase.messaging.remoteMessage

class NotificationService : FirebaseMessagingService() {
    private val CHANNEL_ID = "fcm_default_channel" // Unique ID for your notification channel
    val TAG: String = "NotificationService"
    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d(TAG, "Here is Your --> Refreshed Token: $token")
    }

    // Know when i receive the notification from the FCM server
    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)
        Log.d(TAG, " Your are getting Notification From: ${message.from}")

        // Check if the message contains a notification payload (sent when your Ktor server sets .setNotification)
        message.notification?.let { notification ->
            Log.d(TAG, "Message Notification Title: ${notification.title}")
            Log.d(TAG, "Message Notification Body: ${notification.body}")

            // Display the notification to the user
            showNotification(notification.title, notification.body)
        }
    }

    // Creates and displays a simple notification based on the FCM message.
    fun showNotification(title: String?, body: String?) {

// Intent to open your MainActivity when the notification is tapped

        val intent: Intent = Intent(this, HomeScreen::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE
        )

        // Build the Notification
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.notificationicon)
            .setContentTitle(title)
            .setContentText(body)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)

        val notificationManager =
            getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Create the NotificationChannel for Android 8.0 (Oreo) and above
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "General Notification",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Notification for general updates from the App"
            }
            notificationManager.createNotificationChannel(channel)
        }

        // Show the notification
        notificationManager.notify(1, notification.build())
    }

}
