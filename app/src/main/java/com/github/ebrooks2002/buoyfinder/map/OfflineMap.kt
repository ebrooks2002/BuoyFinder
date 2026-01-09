package com.github.ebrooks2002.buoyfinder.ui.map

import android.content.Context
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.maplibre.android.MapLibre
import org.maplibre.android.camera.CameraPosition
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.maps.MapView
import org.maplibre.android.maps.Style
import java.io.File
import java.io.FileOutputStream

@Composable
fun OfflineMap(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    // State to hold the prepared style URL
    var styleUrl by remember { mutableStateOf<String?>(null) }

    // 1. Initialize MapLibre (Once)
    DisposableEffect(Unit) {
        MapLibre.getInstance(context)
        onDispose { }
    }

    // 2. Prepare files in the background (Prevents UI Freeze)
    LaunchedEffect(context) {
        withContext(Dispatchers.IO) {
            val mbtilesFile = copyAssetToFiles(context, "ghana_offline_3857.mbtiles")
            val jsonFile = copyAssetToFiles(context, "styles.json")
            var jsonContent = jsonFile.readText()
            jsonContent = jsonContent.replace("{path_to_mbtiles}", mbtilesFile.absolutePath)
            jsonFile.writeText(jsonContent)
            styleUrl = "file://${jsonFile.absolutePath}"
        }
    }

    // 3. Render Map only when style is ready
    if (styleUrl != null) {
        val currentStyleUrl = styleUrl!!

        val mapView = remember {
            MapView(context).apply {
                // IMPORTANT: onCreate is required for MapLibre/Mapbox to function
                onCreate(null)
            }
        }

        // 4. Manage Lifecycle
        DisposableEffect(lifecycleOwner) {
            val observer = LifecycleEventObserver { _, event ->
                when (event) {
                    Lifecycle.Event.ON_START -> mapView.onStart()
                    Lifecycle.Event.ON_RESUME -> mapView.onResume()
                    Lifecycle.Event.ON_PAUSE -> mapView.onPause()
                    Lifecycle.Event.ON_STOP -> mapView.onStop()
                    Lifecycle.Event.ON_DESTROY -> mapView.onDestroy()
                    else -> {}
                }
            }
            lifecycleOwner.lifecycle.addObserver(observer)
            onDispose {
                lifecycleOwner.lifecycle.removeObserver(observer)
            }
        }

        AndroidView(
            factory = {
                mapView.apply {
                    getMapAsync { map ->
                        map.setStyle(Style.Builder().fromUri(currentStyleUrl)) { style ->
                        }
                        val uiSettings = map.uiSettings
                        uiSettings.isZoomGesturesEnabled = true
                        uiSettings.isScrollGesturesEnabled = true
                        uiSettings.isRotateGesturesEnabled = false
                        uiSettings.isTiltGesturesEnabled = false
                        mapView.setOnTouchListener { view, event ->
                            view.parent.requestDisallowInterceptTouchEvent(true)
                            false
                        }

                        map.cameraPosition = CameraPosition.Builder()
                            .target(LatLng(5.00928, -0.78918))
                            .zoom(6.0)
                            .build()
                    }
                }
            },
            modifier = modifier.fillMaxSize(),
            // update block is left empty or minimal to prevent re-initialization
            update = { _ -> }
        )
    } else {
        // Show loading while files are copying
        Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
    }
}

// Helper to copy file from assets to internal storage
private fun copyAssetToFiles(context: Context, fileName: String): File {
    val file = File(context.filesDir, fileName)
    if (!file.exists()) {
        try {
            context.assets.open(fileName).use { inputStream ->
                FileOutputStream(file).use { outputStream ->
                    inputStream.copyTo(outputStream)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    return file
}