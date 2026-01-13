/**
 *
 * Instantiates the view model and local context.
 * Calls the HomeScreen composable function.
 *
 * @Author E. Brooks
 */

package com.github.ebrooks2002.buoyfinder.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import com.github.ebrooks2002.buoyfinder.ui.screens.BuoyFinderViewModel
import com.github.ebrooks2002.buoyfinder.ui.screens.HomeScreen
import androidx.lifecycle.viewmodel.compose.viewModel
import com.github.ebrooks2002.buoyfinder.model.AssetData
import com.github.ebrooks2002.buoyfinder.ui.theme.BuoyFinderTheme

@Composable
fun BuoyFinderApp() {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color.LightGray

    ) {
        val buoyFinderViewModel: BuoyFinderViewModel = viewModel()
        val context = LocalContext.current

        HomeScreen(
            buoyFinderUiState = buoyFinderViewModel.buoyFinderUiState,
            onGetDataClicked = {buoyFinderViewModel.getAssetData()},
            userLocation = buoyFinderViewModel.userLocation,
            onStartLocationUpdates = {buoyFinderViewModel.startLocationTracking(context)},
            userRotation = buoyFinderViewModel.userRotation,
            userDirection = buoyFinderViewModel.headingDirection,
            onStartRotationUpdates = {buoyFinderViewModel.startRotationTracking(context)}
        )
    }
}

@Preview(
    showBackground = true,
    showSystemUi = true,
    device = "id:pixel_5"
)
@Composable
fun BuoyFinderAppPreview() {
    BuoyFinderTheme {
        HomeScreen(
            buoyFinderUiState = com.github.ebrooks2002.buoyfinder.ui.screens.BuoyFinderUiState.Success(
                AssetData()
            ),
            onGetDataClicked = {},
            userLocation = null,
            onStartLocationUpdates = {},
            userRotation = 0f,
            userDirection = "N",
            onStartRotationUpdates = {},
        )
    }
}