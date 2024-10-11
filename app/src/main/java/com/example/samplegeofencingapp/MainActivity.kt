package com.example.samplegeofencingapp

import android.content.pm.PackageManager
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import android.Manifest
import android.annotation.SuppressLint
import android.app.Dialog
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.core.app.ActivityCompat
import com.example.samplegeofencingapp.databinding.ActivityMainBinding
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingClient
import com.google.android.gms.location.GeofencingRequest
import com.google.android.gms.location.LocationServices
import android.location.Location
import android.view.View
import androidx.core.view.isVisible
import com.example.samplegeofencingapp.databinding.CustomProgressBarBinding

class MainActivity : AppCompatActivity() {
    private lateinit var dialog: Dialog
    private lateinit var binding: ActivityMainBinding
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var geofencingClient: GeofencingClient
    private lateinit var geofencePendingIntent: PendingIntent
    private lateinit var customProgress: CustomProgressBarBinding
    private val foregroundPermissionsNeeded = arrayOf(
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION,
        Manifest.permission.POST_NOTIFICATIONS,
    )
    private val backgroundPermissionsNeeded = arrayOf(
        Manifest.permission.ACCESS_BACKGROUND_LOCATION,
    )

    @RequiresApi(Build.VERSION_CODES.Q)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        customProgress = CustomProgressBarBinding.inflate(layoutInflater)
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        geofencingClient = LocationServices.getGeofencingClient(this)
        val view = binding.root
        setContentView(view)
        createNotifyChannel()
        if (checkAndRequestForegroundPermissions() && checkAndRequestBackgroundPermissions()) {
            startGeofencing()
        } else {
            Toast.makeText(
                this,
                "Permissions are not been given",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun createNotifyChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channelId = "raiden1001"
            val notificationManager: NotificationManager =
                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val existingChannel = notificationManager.getNotificationChannel(channelId)
            if (existingChannel == null) {
                val channelName = "Your Channel Name"
                val channelDescription = "Your Channel Description"
                val importance = NotificationManager.IMPORTANCE_HIGH
                val channel = NotificationChannel(channelId, channelName, importance).apply {
                    description = channelDescription
                }
                notificationManager.createNotificationChannel(channel)
                println("Channel created with ID: $channelId")
            } else {
                println("Channel already exists with ID: $channelId")
            }
        }
    }

    private fun startGeofencing() {
        binding.startBtn.setOnClickListener {

            runOnUiThread {
                customProgress.progressBar.visibility = View.VISIBLE
            }
            // Request the current location
            getCurrentLocation()
        }
    }

    private fun checkAndRequestForegroundPermissions(): Boolean {
        val foregroundPermissionsToRequest = foregroundPermissionsNeeded.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }.toTypedArray()

        if (foregroundPermissionsToRequest.isNotEmpty()) {
            showCustomPermissionDialog(FOREGROUND_PERMISSIONS_REQUEST_CODE)
            return false
        } else {
            return true
        }
    }

    private fun checkAndRequestBackgroundPermissions(): Boolean {
        val backgroundPermissionsToRequest = backgroundPermissionsNeeded.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }.toTypedArray()

        if (backgroundPermissionsToRequest.isNotEmpty()) {
            showCustomPermissionDialog(BACKGROUND_PERMISSIONS_REQUEST_CODE)
            return false
        } else {
            return true
        }
    }

    @SuppressLint("SetTextI18n")
    private fun showCustomPermissionDialog(requestCode: Int) {
        // Create a dialog to request permissions
        dialog = Dialog(this)
        dialog.setContentView(R.layout.dialog_permission_request)

        val permissionMessage: TextView = dialog.findViewById(R.id.permissionMessage)
        val btnRequestPermissions: Button = dialog.findViewById(R.id.btnRequestPermissions)

        // Set the message (optional: customize it further)
        permissionMessage.text =
            "This app needs the following permissions: ${foregroundPermissionsNeeded.joinToString(", ")}${backgroundPermissionsNeeded}"
        btnRequestPermissions.setOnClickListener {
            requestPermissions(requestCode)
            dialog.dismiss()
        }

        dialog.setCancelable(false) // Prevent closing the dialog without granting permissions
        dialog.show()
    }

    private fun requestPermissions(requestCode: Int) {
        when (requestCode) {
            FOREGROUND_PERMISSIONS_REQUEST_CODE -> {
                ActivityCompat.requestPermissions(
                    this,
                    foregroundPermissionsNeeded,
                    FOREGROUND_PERMISSIONS_REQUEST_CODE
                )

            }

            BACKGROUND_PERMISSIONS_REQUEST_CODE -> {
                ActivityCompat.requestPermissions(
                    this,
                    backgroundPermissionsNeeded,
                    BACKGROUND_PERMISSIONS_REQUEST_CODE
                )
            }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            FOREGROUND_PERMISSIONS_REQUEST_CODE -> {
                val allGranted = grantResults.all { it == PackageManager.PERMISSION_GRANTED }
                if (allGranted) {
                    restartApp()
                } else {
                    // Handle the case where permissions are denied
                    Toast.makeText(
                        this,
                        "Foreground Permissions denied. Feature unavailable.",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }

            BACKGROUND_PERMISSIONS_REQUEST_CODE -> {
                val allGranted = grantResults.all { it == PackageManager.PERMISSION_GRANTED }
                if (allGranted) {
                    restartApp()
                } else {
                    // Handle the case where permissions are denied
                    Toast.makeText(
                        this,
                        "Background Permissions denied. Feature unavailable.",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    private fun createGeofence(location: Location) {
        // Define the geofence
        val geofence = Geofence.Builder()
            .setRequestId(GEOFENCE_ID) // Unique ID for the geofence
            .setCircularRegion(
                location.latitude, // Latitude
                location.longitude, // Longitude
                300f // Radius in meters
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
        geofencePendingIntent = PendingIntent.getBroadcast(
            this,
            0,
            Intent(this, GeofenceBroadcastReceiver::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
        )

        // Add the geofence
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

    private fun getCurrentLocation() {
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED &&
            ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            // Request permissions if not granted
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                LOCATION_PERMISSION_REQUEST_CODE
            )
            return
        }
        // Get the last known location
        fusedLocationClient.lastLocation.addOnSuccessListener { location: Location? ->
            if (location != null) {
                customProgress.progressBar.isVisible = false
                // Create a geofence around the current location with a 200 meter radius
                createGeofence(location)
            } else {
                customProgress.progressBar.isVisible = false
                Toast.makeText(this, "Current location not available", Toast.LENGTH_SHORT).show()
            }
        }.addOnFailureListener { exception ->
            customProgress.progressBar.isVisible = false
            Toast.makeText(this, "Failed to get location: ${exception.message}", Toast.LENGTH_SHORT)
                .show()
        }
    }

    private fun restartApp() {
        val intent = packageManager.getLaunchIntentForPackage(packageName)
        intent?.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
        startActivity(intent)
        finish()
        Runtime.getRuntime().exit(0)
    }

    companion object {
        private const val FOREGROUND_PERMISSIONS_REQUEST_CODE = 100
        private const val BACKGROUND_PERMISSIONS_REQUEST_CODE = 101
        private const val LOCATION_PERMISSION_REQUEST_CODE = 1000
        private const val GEOFENCE_ID = "1001"
    }
}