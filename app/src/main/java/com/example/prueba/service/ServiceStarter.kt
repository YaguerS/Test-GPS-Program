package com.example.prueba

//import GpsForegroundService
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.content.ContextCompat

object ServiceStarter {

    fun startGpsService(context: Context) {
        val intent = Intent(context, GpsForegroundService::class.java).apply { 
            action = GpsForegroundService.ACTION_START
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            ContextCompat.startForegroundService(context, intent)
        } else {
            context.startService(intent)
        }
    }

    fun stopGpsService(context: Context){
        val intent = Intent(context, GpsForegroundService::class.java).apply {
            action = GpsForegroundService.ACTION_STOP
        }
        context.startService(intent)
    }
}