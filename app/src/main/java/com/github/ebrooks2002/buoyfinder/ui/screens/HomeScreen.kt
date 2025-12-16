package com.github.ebrooks2002.buoyfinder.ui.screens


import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize

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
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.Color
import androidx.compose.material3.Button
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.SegmentedButtonDefaults.Icon
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon

@Composable
fun HomeScreen(
    buoyFinderUiState: BuoyFinderUiState,
    onGetDataClicked: () -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(0.dp)
) {

    when (buoyFinderUiState) {
        is BuoyFinderUiState.Loading -> LoadingScreen()
        is BuoyFinderUiState.Success -> ResultScreen(
            buoyFinderUiState.assetData)
        is BuoyFinderUiState.Error -> ErrorScreen()
        is BuoyFinderUiState.Idle -> IdleScreen(onGetDataClicked)
    }
}

@Composable
fun IdleScreen(onGetDataClicked: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Button(onClick = onGetDataClicked) {
            Text(text = "Get Asset Data", fontSize = 20.sp)
        }
    }
}

@Composable
fun ResultScreen(assetData: AssetData, modifier: Modifier = Modifier) {
    val messages = assetData.feedMessageResponse?.messages?.list ?: emptyList()
    val uniqueAssets = messages.mapNotNull { it.messengerName }.distinct().sorted()

    var selectedAssetName by remember { mutableStateOf(uniqueAssets.firstOrNull()) }

    val selectedMessage = messages.find { it.messengerName == selectedAssetName }

    val assetName = selectedMessage?.messengerName ?: "Select an Asset"
    val position = if (selectedMessage != null) {
        "Loc: ${selectedMessage.latitude}, ${selectedMessage.longitude}"
    } else { "Position not available" }
    val dateTime = selectedMessage?.dateTime ?: "Time not available"

    DropDownMenu(
        availableAssets = uniqueAssets,
        onAssetSelected = { newName -> selectedAssetName = newName },
        currentSelection = selectedAssetName
    )

    Box(
        modifier = Modifier.fillMaxSize(),
        Alignment.Center
    ) {
        Column (
            modifier = Modifier
                .fillMaxWidth(0.9F)
                .fillMaxHeight(0.3F)
                .border(width = 2.dp, color = Color.Black),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement =  Arrangement.Center
        ) {
            Text(
                modifier = Modifier.padding(12.dp),
                fontWeight = FontWeight.Bold,
                fontSize = 24.sp,
                text = "Asset: $assetName" // Updated label
            )
            Text(
                modifier = Modifier.padding(12.dp),
                fontSize = 24.sp,
                text = position
            )
            Text(
                modifier = Modifier.padding(12.dp),
                fontSize = 24.sp,
                text = dateTime
            )
        }
    }
}

@Composable
fun DropDownMenu(
    availableAssets: List<String>,
    onAssetSelected: (String) -> Unit,
    currentSelection: String?
) {
    var expanded by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        // The container for the Menu
        Box(modifier = Modifier.padding(top = 100.dp)) { // Adjust this padding to move it up/down
            Button(onClick = { expanded = true }) {
                Text(text = currentSelection ?: "Select Asset")
                Icon(
                    imageVector = Icons.Filled.ArrowDropDown,
                    contentDescription = null
                )
            }

            // The actual list of items
            androidx.compose.material3.DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                availableAssets.forEach { assetName ->
                    androidx.compose.material3.DropdownMenuItem(
                        text = { Text(text = assetName) },
                        onClick = {
                            onAssetSelected(assetName) // Tell parent (ResultScreen) to update
                            expanded = false // Close menu
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun ErrorScreen(modifier: Modifier = Modifier) {
    Box (
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(
            modifier = Modifier.padding(14.dp),
            fontSize = 45.sp,
            textAlign = TextAlign.Center,
            lineHeight = 50.sp,
            text = "Error retrieving data"
        )
    }
}
@Composable
fun LoadingScreen(modifier: Modifier = Modifier) {
    Box (
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ){
        Text(
            modifier = Modifier.padding(14.dp),
            fontSize = 45.sp,
            textAlign = TextAlign.Center,
            lineHeight = 40.sp,
            text = "Loading"
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
    com.github.ebrooks2002.buoyfinder.ui.theme.BuoyFinderTheme {
        androidx.compose.material3.Surface(
            modifier = Modifier.fillMaxSize()
        ) {
            HomeScreen(
                buoyFinderUiState = BuoyFinderUiState.Idle,
                onGetDataClicked = {}

            )
        }
    }
}




