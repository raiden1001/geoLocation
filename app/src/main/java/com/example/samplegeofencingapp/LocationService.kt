package com.example.samplegeofencingapp

import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.IBinder

class GeofenceService : Service() {
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Do background work here
        // If the work is lengthy, consider using a separate thread or AsyncTask
        PendingIntent.getBroadcast(
            this,
            0,
            Intent(this, GeofenceBroadcastReceiver::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
        )
        // Stop the service once the work is done
        stopSelf()
        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }
}