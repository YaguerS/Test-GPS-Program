package com.example.prueba

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.IBinder
import android.os.Looper
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat

/**
 * Foreground Android service that continuously listens for GPS location updates and logs
 * basic GNSS-related data (lat/lon/alt/speed/accuracy).
 *
 * Why foreground?
 * - Android may stop background services unless they run as a foreground service.
 * - This service calls [startForeground] in [onCreate] using a persistent notification.
 *
 * How to control it:
 * - Send an [Intent] with [ACTION_START] to begin tracking.
 * - Send an [Intent] with [ACTION_STOP] to stop tracking.
 */
class GpsForegroundService : Service() {

    companion object {
        /** Notification channel id used for the foreground service notification. */
        const val CHANNEL_ID = "gps_foreground_channel"

        /** Stable notification id used when calling startForeground(...). */
        const val NOTIFICATION_ID = 1001

        /** Intent action to start requesting location updates. */
        const val ACTION_START = "ACTION_START_GPS"

        /** Intent action to stop requesting location updates. */
        const val ACTION_STOP = "ACTION_STOP_GPS"
    }

    /** Android system service used to request/remove location updates. */
    private lateinit var locationManager: LocationManager

    /** Simple guard to prevent requesting updates multiple times. */
    private var isTracking = false

    /**
     * Listener that receives location updates from [LocationManager].
     *
     * Each time GPS produces a new [Location], we forward it to [handleLocationUpdate].
     */
    private val locationListener = object : LocationListener {
        override fun onLocationChanged(location: Location) {
            handleLocationUpdate(location)
        }

        // Called when a provider (GPS/network/etc.) becomes enabled.
        override fun onProviderEnabled(provider: String) {}

        // Called when a provider (GPS/network/etc.) becomes disabled.
        override fun onProviderDisabled(provider: String) {}
    }

    /**
     * Lifecycle: called when the service is first created.
     *
     * Responsibilities:
     * - Initialize [locationManager]
     * - Create the notification channel (required on Android 8+)
     * - Start the service in the foreground with a persistent notification
     */
    override fun onCreate() {
        super.onCreate()

        // Get the system LocationManager instance.
        locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager

        // Android O+ requires a notification channel before a foreground notification.
        createNotificationChannel()

        // Must be called quickly after service start; otherwise Android may stop the service.
        startForeground(NOTIFICATION_ID, buildNotification())
    }

    /**
     * Lifecycle: called whenever the service receives a start command.
     *
     * We use intent actions to decide whether to start or stop tracking.
     */
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            // Explicit start action.
            ACTION_START -> startTracking()

            // Explicit stop action.
            ACTION_STOP -> stopTracking()

            // Default behavior: start tracking if action is missing/unknown.
            else -> startTracking()
        }

        // If the system kills the service, try to recreate it with the last intent.
        return START_STICKY
    }

    /**
     * Lifecycle: called when the service is being destroyed.
     *
     * We ensure we stop location updates to avoid leaks and unnecessary battery usage.
     */
    override fun onDestroy() {
        super.onDestroy()
        stopTracking()
    }

    /** Foreground service is not meant to be bound to an activity. */
    override fun onBind(intent: Intent?): IBinder? = null

    /**
     * Request GPS location updates.
     *
     * Includes:
     * - Idempotency check (avoid duplicate requestLocationUpdates)
     * - Permission check (ACCESS_FINE_LOCATION)
     * - Calls LocationManager.requestLocationUpdates
     */
    private fun startTracking() {
        // If we are already tracking, do nothing.
        if (isTracking) return

        // Verify runtime permission before requesting updates.
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            // If missing permission, stop the service rather than crashing.
            stopSelf()
            return
        }

        // Mark tracking as active before requesting updates.
        isTracking = true

        // Request updates from the GPS provider.
        // Parameters:
        // - provider: GPS only (not network)
        // - minTimeMs: 1000ms minimum time between updates
        // - minDistanceM: 0f => receive even small movements
        // - listener: our [locationListener]
        // - looper: Main looper (callbacks on the main thread)
        locationManager.requestLocationUpdates(
            LocationManager.GPS_PROVIDER,
            1000L,
            0f,
            locationListener,
            Looper.getMainLooper()
        )
    }

    /**
     * Stop requesting location updates.
     *
     * Safe to call multiple times due to the [isTracking] guard.
     */
    private fun stopTracking() {
        // If we are not tracking, do nothing.
        if (!isTracking) return

        // Mark tracking as stopped.
        isTracking = false

        // Remove our listener from LocationManager.
        locationManager.removeUpdates(locationListener)
    }

    /**
     * Called by [locationListener] whenever a new [Location] is received.
     *
     * Extracts the fields we care about and forwards them to [logGps].
     */
    private fun handleLocationUpdate(location: Location) {
        // Extract useful location properties.
        val lat = location.latitude
        val lon = location.longitude
        val alt = location.altitude
        val speed = location.speed
        val accuracy = location.accuracy

        // Centralized log formatting.
        logGps(lat, lon, alt, speed, accuracy)
    }

    /**
     * Build the persistent notification shown while this service runs in the foreground.
     */
    private fun buildNotification(): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("GPS Tracking Active")
            .setContentText("Collecting GNSS data in background")
            .setSmallIcon(android.R.drawable.ic_menu_mylocation)
            .setOngoing(true) // Makes the notification non-dismissible (typical for ongoing services).
            .build()
    }

    /**
     * Create the notification channel used by the foreground service notification.
     *
     * Note: Required on Android 8.0+ (API 26+). If already created, this call is safe.
     */
    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "GPS Service",
            NotificationManager.IMPORTANCE_LOW // Low importance to reduce user disruption.
        )

        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(channel)
    }

    /**
     * Helper method to check whether we have fine location permission.
     *
     * (Currently not used by the service logic; kept as a utility.)
     */
    private fun hasLocationPermission(): Boolean {
        return checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED
    }

    /**
     * Log GPS values to Logcat.
     *
     * Tag: "GPS"
     * Format: LAT=... LON=... ALT=... SPEED=... ACC=...
     */
    private fun logGps(lat: Double, lon: Double, alt: Double, speed: Float, acc: Float) {
        Log.d("GPS", "LAT=$lat LON=$lon ALT=$alt SPEED=$speed ACC=$acc")
    }
}


