package com.github.ebrooks2002.buoyfinder.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.github.ebrooks2002.buoyfinder.model.AssetData
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.Color
import androidx.compose.material3.Button
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone
import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import android.location.Location
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Surface
import androidx.lifecycle.viewmodel.compose.viewModel
import com.github.ebrooks2002.buoyfinder.ui.theme.BuoyFinderTheme


@Composable
fun HomeScreen(
    buoyFinderUiState: BuoyFinderUiState,
    onGetDataClicked: () -> Unit,
    modifier: Modifier = Modifier,
    userLocation: Location?,
    onStartLocationUpdates: () -> Unit,
    userRotation : Float?,
    userDirection :String?,
    onStartRotationUpdates: () -> Unit,
    contentPadding: PaddingValues = PaddingValues(0.dp)
) {
    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val fineLocationGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true
        val coarseLocationGranted = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true

        if (fineLocationGranted || coarseLocationGranted) {
            onStartLocationUpdates()
            onStartRotationUpdates()
        }
    }
    LaunchedEffect(Unit) {
        locationPermissionLauncher.launch(
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.BODY_SENSORS
            )
        )
    }

    // handles offline persistence
    var currentAssetData by remember { mutableStateOf<AssetData?>(null) }

    if (buoyFinderUiState is BuoyFinderUiState.Success) {
           currentAssetData = buoyFinderUiState.assetData
    }

    if (currentAssetData != null) {
        ResultScreen(
            assetData = currentAssetData!!,
            onGetDataClicked = onGetDataClicked,
            userLocation = userLocation,
            userRotation = userRotation,
            userDirection = userDirection,
            loading = buoyFinderUiState is BuoyFinderUiState.Loading,
            error = buoyFinderUiState is BuoyFinderUiState.Error
        )
    }
    else {
        when (buoyFinderUiState) {
            is BuoyFinderUiState.Loading -> ErrorLoadingMessage(message="Loading")
            is BuoyFinderUiState.Error -> ErrorLoadingMessage(message="Error Fetching Data")
            else -> {}
        }
    }
}
@Composable
fun ResultScreen(assetData: AssetData,
                 onGetDataClicked: () -> Unit,
                 modifier: Modifier = Modifier,
                 userLocation: Location?,
                 userRotation: Float?,
                 userDirection: String?,
                 loading: Boolean,
                 error: Boolean) {

    val messages = assetData.feedMessageResponse?.messages?.list ?: emptyList()
    val uniqueAssets = messages.mapNotNull{ it.messengerName }.distinct().sorted()
    var selectedAssetName by remember { mutableStateOf(uniqueAssets.firstOrNull()) }
    val selectedMessage = messages.find { it.messengerName == selectedAssetName }
    val assetName = selectedMessage?.messengerName?.substringAfterLast("_") ?: "Select an Asset "
    val position = if (selectedMessage != null) {
        "Location: ${selectedMessage.latitude}, ${selectedMessage.longitude}"
    } else {
        "Position not available"
    }

    val formattedDate =  selectedMessage?.formattedDate ?: "Date not available"
    val formattedTime = selectedMessage?.formattedTime ?: "Time not available"

    var gpsInfo = "Waiting for GPS..."
    if (userLocation != null && selectedMessage != null) { // if user location and message come through, display info. otherwise we're waiting.
        // Create a Location object for the Buoy to do math
        val buoyLocation = Location("Buoy").apply {
            latitude = selectedMessage.latitude
            longitude = selectedMessage.longitude
        }
        // 1. Distance (Meters -> Kilometers)
        val distanceMeters = userLocation.distanceTo(buoyLocation)
        val distanceKm = distanceMeters / 1000
        val bearingToBuoy = userLocation.bearingTo(buoyLocation)
        val myHeading = userLocation.bearing

        gpsInfo = """
        Distance to Buoy: %.2f km
        Bearing to Buoy: %.0f°
        Currently Moving Towards: %.0f°
        Currently Pointed Towards: %.0f %s°
        """.trimIndent().format(distanceKm, bearingToBuoy, myHeading, userRotation, userDirection)

    }
    else {
        gpsInfo = "Waiting for GPS Location..."
    }

    Column(modifier = Modifier.fillMaxSize()
        .verticalScroll(rememberScrollState())
    ) {
        // 1. TOP BAR ROW (Holds both buttons)
        Row(
            modifier = Modifier
                .fillMaxWidth() // Place row at the top
                .padding(top = 25.dp, start = 16.dp, end = 16.dp), // Global padding
            horizontalArrangement = Arrangement.SpaceBetween, // Pushes items to edges
            verticalAlignment = Alignment.CenterVertically
        )
        {
            // Pass modifier weight(1f) to dropdown if you want it to shrink if text is huge
            DropDownMenu(
                availableAssets = uniqueAssets,
                onAssetSelected = { newName -> selectedAssetName = newName },
                currentSelection = selectedAssetName?.substringAfterLast("_")
            )
            Spacer(modifier = Modifier.width(8.dp))
            RefreshFeedButton(onGetDataClicked = onGetDataClicked)
            }

        if (loading) {
            DisplayRefreshMessage(color=Color.Gray, message="Refreshing data...")
        }
        if (error) {
            DisplayRefreshMessage(color=Color.Red, message="Offline - Showing last known data")
        }

        DisplayAssetData(assetName, position, outputDateFormat = formattedDate, outputTimeFormat = formattedTime, gpsInfo)
    }
}
@Composable
fun DisplayRefreshMessage(color: Color, message: String) {
    Text(
        text = message,
        fontSize = 12.sp,
        color = color,
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth().padding(4.dp)
    )
}
@Composable
fun DisplayAssetData(assetName: String,
                     position: String,
                     outputDateFormat: String,
                     outputTimeFormat: String,
                     gpsInfo: String? = null) {
        Column(
            modifier = Modifier
                .padding(top = 20.dp)
                .wrapContentHeight()
                .fillMaxWidth(0.95F),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                modifier = Modifier
                    .padding(10.dp)
                    .fillMaxWidth(),
                fontWeight = FontWeight.Bold,
                fontSize = 24.sp,
                text = "SPOT Tracker: $assetName " // Updated label
            )
            Text(
                modifier = Modifier
                    .padding(start = 10.dp)
                    .fillMaxWidth(),
                fontSize = 20.sp, text = position
            )
            Text(
                modifier = Modifier
                    .padding(start = 10.dp)
                    .fillMaxWidth(),
                fontSize = 20.sp, text = outputDateFormat
            )
            Text(
                modifier = Modifier
                    .padding(start = 10.dp)
                    .fillMaxWidth(),
                fontSize = 20.sp, text = outputTimeFormat
            )
            Spacer(
                modifier = Modifier.height(5.dp)
            )
            Text(
                modifier = Modifier
                    .padding(10.dp)
                    .fillMaxWidth(),
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                text = "My Device:"
            )
            if (gpsInfo != null) {
                Text(
                    modifier = Modifier
                        .padding(start=10.dp)
                        .fillMaxWidth(),
                    fontSize = 18.sp,
                    text = gpsInfo
                )
            }
        }
    }
@Composable
fun RefreshFeedButton(
    onGetDataClicked: () -> Unit,
)
    {
       Button(
           onClick = onGetDataClicked,
           modifier = Modifier.padding(top=40.dp, end=8.dp),
           colors = ButtonDefaults.buttonColors(containerColor = Color(0XFF453563))
       ) {
           Text(text = "Refresh",
               maxLines = 1)
       }
}
@Composable
fun DropDownMenu(
    availableAssets: List<String>, onAssetSelected: (String) -> Unit, currentSelection: String?,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    Box(modifier = Modifier.padding(top = 40.dp, start = 5.dp)) { // Adjust this padding to move it up/down
        Button(onClick = { expanded = true },
            colors = ButtonDefaults.buttonColors(containerColor = Color(0XFF453563))
        ) {
            Text(text = currentSelection ?: "Select Asset")
            Icon(
                imageVector = Icons.Filled.ArrowDropDown, contentDescription = null
            )
        }
        DropdownMenu(
            expanded = expanded, onDismissRequest = { expanded = false }) {
            availableAssets.forEach { assetName ->
                DropdownMenuItem(text = { Text(text = assetName) }, onClick = {
                    onAssetSelected(assetName)
                    expanded = false
                })
            }
        }
    }
}
@Composable
fun ErrorLoadingMessage(modifier: Modifier = Modifier, message: String) {
    Box(
        modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center
    ) {
        Text(
            modifier = Modifier.padding(14.dp),
            fontSize = 45.sp,
            textAlign = TextAlign.Center,
            lineHeight = 50.sp,
            text = message
        )
    }
}


@Preview(
    showBackground = true,       // 1. Adds a white background
    showSystemUi = true,         // 2. Adds status bar and nav bar
    device = "id:pixel_5"        // 3. Sets specific device dimensions (optional but helpful)
)
@Composable
fun HomeScreenPreview() {
    // 4. Wrap in your Theme and Surface to mimic the real app environment
    BuoyFinderTheme {
        Surface(
            modifier = Modifier.fillMaxSize()
        ) {
            val viewModel: BuoyFinderViewModel = viewModel()
            val context = LocalContext.current
            HomeScreen(
                buoyFinderUiState = BuoyFinderUiState.Success(AssetData()),
                onGetDataClicked = {viewModel.getAssetData()},
                userLocation = viewModel.userLocation,
                onStartLocationUpdates = {
                    viewModel.startLocationTracking(context)
                },
                userRotation = viewModel.userRotation,
                userDirection = viewModel.headingDirection,
                onStartRotationUpdates = {
                    viewModel.startRotationTracking(context)
                }
            )
        }
    }
}
