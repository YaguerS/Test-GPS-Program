import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
// NOTE: This file currently uses a nested Callback interface below.
// The external imports below were added by mistake in the skeleton and can be removed.
// Keeping this file focused: the only Callback contract used is the nested interface in this class.
// (Comments only request) NOTE: the skeleton included these imports by mistake.
// Do not change code per your instruction; leaving imports as-is for now.

/**
 * PermissionManager
 * -----------------
 * Centralizes runtime location permission flow:
 *  - Request ACCESS_FINE_LOCATION (foreground)
 *  - If granted, then request ACCESS_BACKGROUND_LOCATION (background)
 *
 * This class is intentionally separated from GPS/location update logic.
 * It only checks permissions, launches permission requests, and reports results
 * through the Callback interface.
 */
class PermissionManager(private val activity: ComponentActivity, private val callback: Callback){
    interface Callback{
        fun onReady()
        fun onDenied(permission: String)
    }

    enum class State {
        NONE,
        FINE_GRANTED,
        BACKGROUND_GRANTED,
        READY
    }

    private var state = State.NONE

    // ----------------------------------------
    // State machine (high-level)
    // ----------------------------------------
    // start() is the single entry point.
    // Flow:
    //  1) If fine + background already granted -> READY -> callback.onReady()
    //  2) If fine granted but background missing -> requestBackgroundPermission()
    //  3) If fine missing -> requestFinePermission()
    // Callbacks from the Activity Result API then advance the state:
    //  - After fine granted -> request background
    //  - After background granted -> READY -> callback.onReady()
    //  - On denial -> callback.onDenied(deniedPermission)

    // ----------------------------------------
    // Fine location launcher
    // ----------------------------------------
    // Result logic:
    // - granted -> request background permission
    // - denied   -> inform callback.onDenied(FINE)
    private val fineLocationLauncher = activity.registerForActivityResult(
        ActivityResultContracts.RequestPermission()) {
            granted -> if (granted) {
                state = State.FINE_GRANTED
                requestBackgroundPermission()
            } else {
                state = State.NONE
                callback.onDenied(Manifest.permission.ACCESS_FINE_LOCATION)
            }
        }
    
    // ----------------------------------------
    // Background location launcher
    // ----------------------------------------
    // Result logic:
    // - granted -> READY -> callback.onReady()
    // - denied   -> inform callback.onDenied(BACKGROUND)
    private val backgroundLocationLauncher = activity.registerForActivityResult(
        ActivityResultContracts.RequestPermission()) {
            granted -> if (granted) {
                state = State.READY
                callback.onReady()
            } else {
                state = State.FINE_GRANTED
                callback.onDenied(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
            }
        }

    /**
     * start()
     * ----
     * Single entry point to begin permission flow.
     * The method decides what to request based on what is already granted.
     */
    fun start() {
        when {
            hasFineLocation() && hasBackgroundLocation() -> {
                state = State.READY
                callback.onReady()
            }

            hasFineLocation() -> {
                state = State.FINE_GRANTED
                requestBackgroundPermission()
            } else -> {
                requestFinePermission()
            }
        }
    }

    /**
     * requestFinePermission()
     * ------------------------
     * Launches runtime permission prompt for ACCESS_FINE_LOCATION.
     */
    private fun requestFinePermission() {
        fineLocationLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
    }

    /**
     * requestBackgroundPermission()
     * -------------------------------
     * Launches runtime permission prompt for ACCESS_BACKGROUND_LOCATION.
     *
     * Important: on modern Android, background access is usually granted only
     * after foreground (fine) permission is already granted.
     */
    private fun requestBackgroundPermission() {
        backgroundLocationLauncher.launch(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
    }



    /**
     * hasFineLocation()
     * -----------------
     * Returns true if ACCESS_FINE_LOCATION is already granted.
     */
    private fun hasFineLocation(): Boolean {
        return ContextCompat.checkSelfPermission(activity, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
    }

    /**
     * hasBackgroundLocation()
     * ------------------------
     * Returns true if ACCESS_BACKGROUND_LOCATION is already granted.
     */
    private fun hasBackgroundLocation(): Boolean {
        return ContextCompat.checkSelfPermission(activity, Manifest.permission.ACCESS_BACKGROUND_LOCATION) == PackageManager.PERMISSION_GRANTED
    }


}