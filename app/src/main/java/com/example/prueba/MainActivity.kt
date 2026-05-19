package com.example.prueba

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.core.app.ActivityCompat

class MainActivity : ComponentActivity() {

    private lateinit var locationManager: LocationManager

    private val locationListener = object : LocationListener {

        override fun onLocationChanged(location: Location) {

            val lat = location.latitude
            val lon = location.longitude
            val alt = location.altitude
            val speed = location.speed
            val accuracy = location.accuracy

            Log.d(
                "GPS",
                "LAT=$lat LON=$lon ALT=$alt SPEED=$speed ACC=$accuracy"
            )
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        locationManager =
            getSystemService(Context.LOCATION_SERVICE) as LocationManager

        if (
            ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {

            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                1001
            )

            return
        }

        locationManager.requestLocationUpdates(
            LocationManager.GPS_PROVIDER,
            1000L,
            0f,
            locationListener
        )
    }

    override fun onDestroy() {
        super.onDestroy()

        locationManager.removeUpdates(locationListener)
    }
}