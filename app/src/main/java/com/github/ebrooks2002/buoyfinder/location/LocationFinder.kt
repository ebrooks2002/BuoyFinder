package com.github.ebrooks2002.buoyfinder.location
import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.os.Looper
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

/**
 * Communicates with system hardware to get GPS data.
 *
 * Creates a fused location provider, requests location updates, exposes location data as a flow.
 *
 * @constructor The application context to retrieve the LocationServices object.
 * @author E. Brooks
 */
class LocationFinder(private val context: Context) {
    private val client = LocationServices.getFusedLocationProviderClient(context)

    /**
     * Returns a flow of location objects representing geographic location of device. When client stops listening,
     * all location updates are removed.
     *
     * @param interval The minimum time interval between location updates in milliseconds.
     */
    @SuppressLint("MissingPermission")
    fun getLocationUpdates(interval: Long): Flow<Location> {
        return callbackFlow {
            val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, interval)
                .build()

            val locationCallback = object : LocationCallback() {
                override fun onLocationResult(result: LocationResult) {
                    result.locations.lastOrNull()?.let { location ->
                        trySend(location)
                    }
                }
            }
            client.requestLocationUpdates(request, locationCallback, Looper.getMainLooper())
            awaitClose {
                client.removeLocationUpdates(locationCallback)
            }
        }
    }
}