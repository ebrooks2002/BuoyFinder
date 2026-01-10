package com.github.ebrooks2002.buoyfinder.ui.map

import android.content.Context
import android.util.Log
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
import com.github.ebrooks2002.buoyfinder.model.AssetData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.maplibre.android.MapLibre
import org.maplibre.android.camera.CameraPosition
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.maps.MapView
import org.maplibre.android.maps.Style
import org.maplibre.android.style.sources.GeoJsonSource
import org.maplibre.android.style.layers.CircleLayer
import org.maplibre.android.style.layers.PropertyFactory
import org.maplibre.geojson.Feature
import org.maplibre.geojson.FeatureCollection
import org.maplibre.geojson.Point
import java.io.File
import java.io.FileOutputStream
import com.github.ebrooks2002.buoyfinder.ui.screens.BuoyFinderViewModel




@Composable
fun OfflineMap(
    modifier: Modifier = Modifier,
    assetData: AssetData,
    viewmodel: BuoyFinderViewModel = BuoyFinderViewModel()
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    // create a nav state object containing attributes like position, asset name, ect.
    val assetData = viewmodel.getNavigationState(assetData)

    val featureCollection = remember(assetData.messages) {
        val features = assetData.messages.map { message ->
            val feature = Feature.fromGeometry(
                Point.fromLngLat(message.longitude, message.latitude)
            )
            // ADD THESE PROPERTIES TO THE FEATURE
            feature.addStringProperty("name", message.messengerName?.substringAfterLast("_") ?: "Unknown")
            feature.addStringProperty("date", message.formattedDate ?: "Unknown Date")
            feature
        }
        FeatureCollection.fromFeatures(features)
    }



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
                            // ADD BUOY LAYER HERE
                            val sourceId = "buoys-source"
                            val source = GeoJsonSource(sourceId, featureCollection)
                            style.addSource(source)

                            val circleLayer = CircleLayer("buoys-layer", sourceId)
                            circleLayer.setProperties(
                                PropertyFactory.circleRadius(1f),
                                PropertyFactory.circleColor(android.graphics.Color.RED),
                                PropertyFactory.circleStrokeWidth(4f),
                                PropertyFactory.circleStrokeColor(android.graphics.Color.RED)
                            )
                            style.addLayer(circleLayer)
                        }

                        val uiSettings = map.uiSettings
                        uiSettings.isZoomGesturesEnabled = true
                        uiSettings.isScrollGesturesEnabled = true
                        uiSettings.isRotateGesturesEnabled = false
                        uiSettings.isTiltGesturesEnabled = false

                        setOnTouchListener { view, _ ->
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
            update = { _ ->
                // Refresh pins when new data comes in
                mapView.getMapAsync { map ->
                    val source = map.style?.getSourceAs<GeoJsonSource>("buoys-source")
                    source?.setGeoJson(featureCollection)
                }
            }
        )
    } else {
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