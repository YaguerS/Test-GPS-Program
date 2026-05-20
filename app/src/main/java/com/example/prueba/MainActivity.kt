package com.example.prueba

import PermissionManager
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.core.app.ActivityCompat

class MainActivity : ComponentActivity(), PermissionManager.Callback{

    private lateinit var permissionManager: PermissionManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        permissionManager = PermissionManager(this, this)

        permissionManager.start()
    }

    override fun onReady() {
        // 🚀 SAFE TO START SERVICE HERE
        Log.d("PERMISSION","READY")
        //ServiceStarter.start(this)
    }

    override fun onDenied(permission: String) {
        Log.d("PERMISSION", "DENIED: $permission")
        // handle denial (UI, retry, etc.)
    }
}