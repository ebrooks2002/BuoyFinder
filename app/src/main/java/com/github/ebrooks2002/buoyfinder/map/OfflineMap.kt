package com.github.ebrooks2002.buoyfinder.ui.map

import android.content.Context
import android.graphics.RectF
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
import org.maplibre.android.style.expressions.Expression


@Composable
fun OfflineMap(
    modifier: Modifier = Modifier,
    assetData: AssetData,
    viewmodel: BuoyFinderViewModel = BuoyFinderViewModel()
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    // create a nav state object containing attributes like position, asset name, ect.
    val assetState = viewmodel.getNavigationState(assetData)

    val featureCollection = remember(assetState.messages) {
        val features = assetState.messages.map { message ->
            val feature = Feature.fromGeometry(Point.fromLngLat(message.longitude, message.latitude))

            feature.addStringProperty(
                "name",
                message.messengerName?.substringAfterLast("_") ?: "Unknown"
            )
            feature.addStringProperty("time", message.formattedTime ?: "Unknown Time")
            feature.addStringProperty("date", message.formattedDate ?: "Unknown Date")
            feature.addStringProperty("color", assetState.color)
            feature.addStringProperty("position", (message.latitude.toString() + ", " + message.longitude.toString()) ?: "Unknown Position"
            )

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
                                PropertyFactory.circleRadius(6f),
                                PropertyFactory.circleColor(Expression.get("color")),
                                PropertyFactory.circleStrokeWidth(1f),
                                PropertyFactory.circleStrokeColor(android.graphics.Color.WHITE)
                            )
                            style.addLayer(circleLayer)
                        }

                        val uiSettings = map.uiSettings
                        uiSettings.isZoomGesturesEnabled = true
                        uiSettings.isScrollGesturesEnabled = true
                        uiSettings.isRotateGesturesEnabled = false
                        uiSettings.isTiltGesturesEnabled = false

                        map.addOnMapClickListener { latLng ->
                            // 1. Convert click location to screen pixels
                            val point = map.projection.toScreenLocation(latLng)

                            val hitBox =
                                RectF(point.x - 15, point.y - 15, point.x + 15, point.y + 15)

                            // 2. See if there is a buoy under that pixel
                            val features = map.queryRenderedFeatures(hitBox, "buoys-layer")

                            if (features.isNotEmpty()) {
                                val feature = features[0]
                                val name = feature.getStringProperty("name")
                                val time = feature.getStringProperty("time")
                                val date = feature.getStringProperty("date")

                                val info = "$name\n$time\n$date"
                                showBuoyPopup(context, mapView, point.x, point.y, info)
                                true
                            } else {
                                false // let the map handle the click normally
                            }
                        }

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

private fun showBuoyPopup(
    context: Context,
    parentView: android.view.View,
    anchorX: Float,
    anchorY: Float,
    content: String
) {
    val textView = android.widget.TextView(context).apply {
        text = content
        textSize = 14f
        setPadding(32, 24, 32, 24)
        setTextColor(android.graphics.Color.WHITE)
        val shape = android.graphics.drawable.GradientDrawable().apply {
            setColor(android.graphics.Color.argb(225, 0, 0, 0))
            cornerRadius = 20f
        }
        background = shape
    }

    val popup = android.widget.PopupWindow(
        textView,
        android.view.ViewGroup.LayoutParams.WRAP_CONTENT,
        android.view.ViewGroup.LayoutParams.WRAP_CONTENT,
        true
    )

    // NEW: Get the map's position on the actual phone screen
    val screenPos = IntArray(2)
    parentView.getLocationInWindow(screenPos)

    // Calculate absolute coordinates: Map Position + Pin Offset
    val finalX = (screenPos[0] + anchorX).toInt()
    val finalY = (screenPos[1] + anchorY).toInt()

    popup.showAtLocation(parentView, android.view.Gravity.NO_GRAVITY, finalX, finalY)
}

