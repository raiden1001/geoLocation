package com.example.samplegeofencingapp

import android.Manifest
import android.app.PendingIntent
import android.app.job.JobParameters
import android.app.job.JobService
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.core.app.ActivityCompat
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingClient
import com.google.android.gms.location.GeofencingRequest
import com.google.android.gms.location.LocationServices

class GeofenceJobService : JobService() {

    private lateinit var geofencingClient: GeofencingClient

    override fun onStartJob(params: JobParameters?): Boolean {
        // Start the geofence creation process in a background thread

        val extras = params?.extras
        val latitude = extras?.getDouble("latitude")
        val longitude = extras?.getDouble("longitude")
        val geofenceId = extras?.getString("geofenceId")

        geofencingClient = LocationServices.getGeofencingClient(this)

        Thread {
            createGeofence(latitude!!,longitude!!,geofenceId!!)
            jobFinished(params, false) // Indicate job is finished
        }.start()
        return true // Job is ongoing
    }

    override fun onStopJob(params: JobParameters?): Boolean {
        // Handle any cleanup if necessary
        return false // Return true if you want to reschedule the job
    }

    private fun createGeofence(latitude:Double, longitude:Double, geofenceId: String ) {
        // Define the geofence
        val geofence = Geofence.Builder()
            .setRequestId(geofenceId)
            .setCircularRegion(
                latitude,
                longitude,
                300f
            )
            .setExpirationDuration(Geofence.NEVER_EXPIRE)
            .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER or Geofence.GEOFENCE_TRANSITION_EXIT)
            .build()

        // Create a geofencing request
        val geofencingRequest = GeofencingRequest.Builder()
            .setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER)
            .addGeofence(geofence)
            .build()

        // Create a pending intent
        val geofencePendingIntent = PendingIntent.getBroadcast(
            applicationContext,
            0,
            Intent(applicationContext, GeofenceBroadcastReceiver::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
        )

        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        geofencingClient.addGeofences(geofencingRequest, geofencePendingIntent)
            .addOnSuccessListener {
                Toast.makeText(this, "Geofence added!", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error adding geofence: ${e.message}", Toast.LENGTH_SHORT)
                    .show()
            }

    }
}
