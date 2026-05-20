package com.example.prueba
import android.Manifest

import android.app.Service
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager

import android.content.Context
import android.content.Intent

import android.location.Location
import android.location.LocationListener
import android.location.LocationManager

import android.os.IBinder
import android.os.Looper
import android.os.Build
import android.os.Bundle

import android.util.Log

import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import android.content.pm.PackageManager



class GpsForegroundService : Service() {

    companion object {
        const val CHANNEL_ID = "gps_foreground_channel"
        const val NOTIFICATION_ID = 1001
        
        const val ACTION_START = "ACTION_START_GPS"
        const val ACTION_STOP = "ACTION_STOP_GPS"
    }

    private lateinit var locationManager: LocationManager
    private var isTracking = false
    private val locationListener = object : LocationListener {
        override fun onLocationChanged(location: Location) {
            handleLocationUpdate(location)
        }

        override fun onProviderEnabled(provider: String) {}
        override fun onProviderDisabled(provider: String) {}
    }

    override fun onCreate(){
        
        super.onCreate()

        locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager

        createNotificationChannel()
        startForeground(NOTIFICATION_ID, buildNotification())
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        
        when (intent?.action) {
            ACTION_START -> startTracking()
            ACTION_STOP -> stopTracking()
            else -> startTracking()
        }
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        stopTracking()
    }
    
    override fun onBind(intent: Intent?): IBinder? = null

    private fun startTracking() {
        
        if (isTracking) return

        if (ActivityCompat.checkSelfPermission(this,
        Manifest.permission.ACCESS_FINE_LOCATION) 
        != PackageManager.PERMISSION_GRANTED) {
            stopSelf()
            return
        }

        isTracking = true

        locationManager.requestLocationUpdates(
            LocationManager.GPS_PROVIDER,
            1000L,
            0f,
            locationListener,
            Looper.getMainLooper())
    }

    private fun stopTracking() {
        
        if (!isTracking) return

        isTracking = false
        locationManager.removeUpdates(locationListener)
    }

    private fun handleLocationUpdate(location: Location) {
        
        val lat = location.latitude
        val lon = location.longitude
        val alt = location.altitude
        val speed = location.speed
        val accuracy = location.accuracy

        logGps(lat, lon, alt, speed, accuracy)
    }

    private fun buildNotification(): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
        .setContentTitle("GPS TrackingActive")
        .setContentText("Collecting GNSS data in background")
        .setSmallIcon(android.R.drawable.ic_menu_mylocation)
        .setOngoing(true)
        .build()
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID, "GPS Service", 
            NotificationManager.IMPORTANCE_LOW)
        
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(channel)
    }

    private fun hasLocationPermission(): Boolean {
        return checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) ==
        PackageManager.PERMISSION_GRANTED
    }







    private fun logGps(lat: Double, lon: Double, alt: Double, speed: Float, acc: Float) {
        Log.d("GPS", "LAT=$lat LON=$lon ALT=$alt SPEED=$speed ACC=$acc")
    }
}

