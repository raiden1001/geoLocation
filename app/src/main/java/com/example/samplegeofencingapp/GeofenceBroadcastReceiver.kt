package com.example.samplegeofencingapp

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.app.NotificationCompat
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofenceStatusCodes
import com.google.android.gms.location.GeofencingEvent


class GeofenceBroadcastReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val geofencingEvent = GeofencingEvent.fromIntent(intent)
        if (geofencingEvent!!.hasError()) {
            val errorMessage = GeofenceStatusCodes.getStatusCodeString(geofencingEvent.errorCode)
            Log.e("GeofenceBroadcast", errorMessage)
            return
        }
        val geofenceTransition = geofencingEvent.geofenceTransition
        if (geofenceTransition == Geofence.GEOFENCE_TRANSITION_ENTER) {
            val geofence = geofencingEvent.triggeringGeofences?.get(0)
            val geofenceId = geofence!!.requestId
            sendNotification(context, "Geofence entered: $geofenceId")
        }

        if (geofenceTransition == Geofence.GEOFENCE_TRANSITION_EXIT) {
            val geofence = geofencingEvent.triggeringGeofences?.get(0)
            val geofenceId = geofence!!.requestId
            sendNotification(context, "Geofence exited: $geofenceId")
        }
    }

    private fun sendNotification(context: Context, message: String) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val notification = NotificationCompat.Builder(context, "raiden1001")
            .setContentTitle("Geofence Notification")
            .setContentText(message)
            .setSmallIcon(R.drawable.ic_launcher_background)
            .build()
        notificationManager.notify(0, notification)
    }
}

