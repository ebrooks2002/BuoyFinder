package com.github.ebrooks2002.buoyfinder.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.github.ebrooks2002.buoyfinder.ui.screens.BuoyFinderViewModel
import com.github.ebrooks2002.buoyfinder.ui.screens.HomeScreen
import androidx.lifecycle.viewmodel.compose.viewModel


@Composable
fun BuoyFinderApp() {
    Surface(
        modifier = Modifier.fillMaxSize()
    ) {
        val buoyFinderViewModel: BuoyFinderViewModel = viewModel()

        HomeScreen(
            buoyFinderUiState = buoyFinderViewModel.buoyFinderUiState,
            onGetDataClicked = { buoyFinderViewModel.getAssetData() }
        )
    }
}
