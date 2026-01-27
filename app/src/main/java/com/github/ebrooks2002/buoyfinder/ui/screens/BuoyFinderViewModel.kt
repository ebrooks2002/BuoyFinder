
package com.github.ebrooks2002.buoyfinder.ui.screens

import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.ebrooks2002.buoyfinder.model.AssetData
import com.github.ebrooks2002.buoyfinder.network.SPOTApi
import kotlinx.coroutines.launch
import retrofit2.HttpException
import android.location.Location
import com.github.ebrooks2002.buoyfinder.location.LocationFinder
import com.github.ebrooks2002.buoyfinder.location.RotationSensor
import com.github.ebrooks2002.buoyfinder.model.Message

sealed interface BuoyFinderUiState {
    data class Success(val assetData: AssetData) : BuoyFinderUiState
    object Error : BuoyFinderUiState
    object Loading : BuoyFinderUiState
}

/**
 * The view model for the Buoy Finder app.
 *
 * Tracks the state of the UI, launches coroutine to
 * asynchronously retrieve and hold user location and rotation data, and asset data.
 *
 * @author E. Brooks
 */

class BuoyFinderViewModel : ViewModel(){
    var buoyFinderUiState: BuoyFinderUiState by mutableStateOf(BuoyFinderUiState.Loading)
        private set
    var userLocation: Location? by mutableStateOf(null)
        private set
    var userRotation: Float? by mutableStateOf(null)
        private set

    // given userRotation, calculates direction (n, w, s, e). returns string.
    val headingDirection: String
        get() {
            val rot = userRotation ?: return "No Magnetometer"
            val directions = listOf("N", "NE", "E", "SE", "S", "SW", "W", "NW")
            val index = kotlin.math.round(rot / 45f).toInt() % 8
            val safeIndex = (index + 8) % 8
            return directions[safeIndex]
        }

    private val locUpdateInterval = 3000L // User location update interval length in milliseconds.

    /**
     * Collects the flow from getRotationUpdates and assigns it to the userRotation public variable.
     *
     * @param context The application context to retrieve the SensorManager object.
     */
    fun startRotationTracking(context: android.content.Context) {
        val rotationClient = RotationSensor(context)
        viewModelScope.launch {
            rotationClient.getRotationUpdates().collect { rotation ->
                userRotation = rotation
            }
        }
    }

    /**
     * Collects the flow from getLocationUpdates and assigns it to the userLocation public variable.
     *
     * @param context The application context to retrieve the LocationServices object.
     */

    fun startLocationTracking(context: android.content.Context) {
        val locationClient = LocationFinder(context)
        viewModelScope.launch {
            // Update every 2 seconds (2000ms)
            locationClient.getLocationUpdates(locUpdateInterval).collect { location ->
                userLocation = location
            }
        }
    }

    init {
        getAssetData()
    }

    /**
     * Launches a coroutine to asynchronously retrieve and hold asset data, while tracking UI State.
     */
    fun getAssetData() {
        viewModelScope.launch {
            buoyFinderUiState = BuoyFinderUiState.Loading
            buoyFinderUiState = try {
                val allMessages = mutableListOf<Message>()
                var listResult: AssetData? = null
                for (i in 0..0) {
                    val start = i * 50
                    val result = SPOTApi.retrofitService.getData(start = start)
                    if (listResult == null) {
                        listResult = result
                    }
                    val messages = result.feedMessageResponse?.messages?.list ?: emptyList()
                    allMessages.addAll(messages)
                    if (messages.size < 50) break
                }
                if (listResult != null) {
                    listResult.feedMessageResponse?.messages?.list = allMessages
                    Log.d("BuoyDebug", "Final combined count being sent to UI: ${allMessages.size}")
                    BuoyFinderUiState.Success(listResult)
                }
                else {
                    BuoyFinderUiState.Error
                }
            } catch (e: HttpException) {
                Log.e("BuoyViewModel", "Network request failed: ${e.code()} ${e.message()}", e)
                BuoyFinderUiState.Error
            } catch (e: Exception) {
                Log.e("BuoyViewModel", "Error fetching message ${e.message}", e)
                BuoyFinderUiState.Error
            }
        }
    }

    // 1. State to track which asset the user has selected
    var selectedAssetName by mutableStateOf<String?>(null)
        private set

    fun selectAsset(name: String) {
        selectedAssetName = name
    }

    /**
     * Processes raw AssetData and Sensor data into a clean state for the UI.
     */
    fun getNavigationState(assetData: AssetData): NavigationState {
        val messageList = assetData.feedMessageResponse?.messages?.list ?: emptyList()
        var assetSpeedDisplay = "0.00 km/h"

        // 1. Get all messages for the SELECTED asset, sorted by time (newest first)
        val assetHistory = messageList
            .filter { it.messengerName == selectedAssetName }
            .sortedByDescending { it.parseDate()?.time ?: 0L }

        Log.d("asset his", assetHistory.toString())

        // 2. We need at least 2 points to calculate speed
        if (assetHistory.size >= 2) {
            val latest = assetHistory[0]
            val previous = assetHistory[1]

            val time1 = latest.parseDate()?.time ?: 0L
            val time2 = previous.parseDate()?.time ?: 0L

            val timeDiffMs = time1 - time2

            if (timeDiffMs > 0) {
                val loc1 = Location("PointA").apply {
                    latitude = latest.latitude
                    longitude = latest.longitude
                }
                val loc2 = Location("PointB").apply {
                    latitude = previous.latitude
                    longitude = previous.longitude
                }

                val distanceMeters = loc1.distanceTo(loc2)

                // Speed = Distance (km) / Time (hours)
                val distanceKm = distanceMeters / 1000.0
                val timeHours = timeDiffMs / (1000.0 * 60.0 * 60.0)

                val speedKmh = distanceKm / timeHours

                assetSpeedDisplay = "%.2f km/h".format(speedKmh)
                Log.d("speed", assetSpeedDisplay)
            }
        } else {
            assetSpeedDisplay = "Calculating..." // Or "N/A" if only 1 message exists
        }
        // FILTER: Only keep the most recent message for each unique asset
        val latestMessagesPerAsset = messageList.distinctBy { it.messengerName }
        // Use latestMessagesPerAsset for the rest of the logic
        val uniqueAssets = messageList.mapNotNull { it.messengerName }.distinct().sorted()

        if (selectedAssetName == null && uniqueAssets.isNotEmpty()) {
            selectedAssetName = uniqueAssets.first()
        }

        val selectedMessage = messageList.find { it.messengerName == selectedAssetName }

        val time = selectedMessage?.parseDate()

        val now = System.currentTimeMillis()

        var myHeading = 0f

        var bearingToBuoy = 0f

        val diffMinutes = if (time != null) {
            (now - time.time) / (1000 * 60)
        } else {
            Long.MAX_VALUE // If no date, treat as "very old"
        }

        Log.d("diffMinutes", diffMinutes.toString())
        val color = when {
            diffMinutes <= 15 -> "#00A86B" // Green
            diffMinutes <= 30 -> "#ccae16" // Yellow
            else -> "#FF0000"              // Red
        }

        // UI String: Asset Name
        val displayName = selectedMessage?.messengerName?.substringAfterLast("_") ?: "Select an Asset"

        // UI String: Position
        val position = selectedMessage?.let {
            "Lat: ${it.latitude},\nLong:${it.longitude}"
        } ?: "Position not available"

        var gpsInfo = "Waiting for GPS Location..."
        if (userLocation != null && selectedMessage != null) {
            val buoyLoc = Location("Buoy").apply {
                latitude = selectedMessage.latitude
                longitude = selectedMessage.longitude
            }

            val distanceKm = userLocation!!.distanceTo(buoyLoc) / 1000
            bearingToBuoy = userLocation!!.bearingTo(buoyLoc)

            // only update myHeading if user is moving above 0.5 meters per second.
            if (userLocation!!.hasSpeed() && userLocation!!.speed > 0.5f) {
                myHeading = userLocation!!.bearing
            }

            gpsInfo = """
            To Asset: %.2f km
            Lat: % .4f 
            Lon: % .4f
            Bearing: %.0f°
            Heading: %.0f°
            Facing: %.0f° %s
        """.trimIndent().format(distanceKm,
                userLocation!!.latitude,
                userLocation!!.longitude,
                bearingToBuoy, myHeading,
                userRotation ?: 0f,
                headingDirection)
        }

        return NavigationState(
            messages = latestMessagesPerAsset,
            selectedAssetName = selectedAssetName,
            displayName = displayName,
            position = position,
            gpsInfo = gpsInfo,
            uniqueAssets = uniqueAssets,
            movingHeading = myHeading,
            formattedDate = selectedMessage?.formattedDate ?: "Date not available",
            formattedTime = selectedMessage?.formattedTime ?: "Time not available",
            diffMinutes = diffMinutes.toString(),
            userRotation = userRotation,
            bearingToBuoy = bearingToBuoy,
            assetSpeedDisplay = assetSpeedDisplay,
            color = color
        )
    }

    /**
     * Data class to hold pre-processed UI info
     */
    data class NavigationState(
        val messages : List<Message>,
        val selectedAssetName: String?,
        val displayName: String,
        val position: String,
        val gpsInfo: String,
        val uniqueAssets: List<String>,
        val formattedDate: String,
        val formattedTime: String,
        val diffMinutes: String,
        val movingHeading: Float,
        val userRotation: Float?,
        val bearingToBuoy: Float,
        val assetSpeedDisplay: String,
        val color: String
    )

}