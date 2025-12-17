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
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentSize
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
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.ui.graphics.RectangleShape
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
            buoyFinderUiState.assetData,
            onGetDataClicked
        )
        is BuoyFinderUiState.Error -> ErrorScreen()
    }
}

@Composable
fun ResultScreen(assetData: AssetData,
                 onGetDataClicked: () -> Unit,
                 modifier: Modifier = Modifier) {
    val messages = assetData.feedMessageResponse?.messages?.list ?: emptyList()
    val uniqueAssets = messages.mapNotNull { it.messengerName }.distinct().sorted()
    var selectedAssetName by remember { mutableStateOf(uniqueAssets.firstOrNull()) }
    val selectedMessage = messages.find { it.messengerName == selectedAssetName }
    val assetName = selectedMessage?.messengerName?.substringAfterLast("_") ?: "Select an Asset "
    val position = if (selectedMessage != null) {
        "Loc: ${selectedMessage.latitude}, ${selectedMessage.longitude}"
    } else {
        "Position not available"
    }
    val dateTime = selectedMessage?.dateTime ?: "Time not available"
    Box(modifier = Modifier.fillMaxSize()) {

        // 1. TOP BAR ROW (Holds both buttons)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter) // Place row at the top
                .padding(top = 25.dp, start = 16.dp, end = 16.dp), // Global padding
            horizontalArrangement = Arrangement.SpaceBetween, // Pushes items to edges
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Pass modifier weight(1f) to dropdown if you want it to shrink if text is huge
            DropDownMenu(
                availableAssets = uniqueAssets,
                onAssetSelected = { newName -> selectedAssetName = newName },
                currentSelection = selectedAssetName?.substringAfterLast("_")
            )

            // Add a little spacer just in case
            Spacer(modifier = Modifier.width(8.dp))

            RefreshFeedButton(onGetDataClicked = onGetDataClicked)
        }

        // 2. ASSET DATA DISPLAY (Middle of screen)
        DisplayAssetData(assetName, position, dateTime)
    }
}

@Composable
fun DisplayAssetData(assetName: String, position: String, dateTime: String) {
    Box(
        modifier = Modifier.fillMaxSize(),
        Alignment.TopCenter

    ) {
        Column(
            modifier = Modifier
                .padding(top=150.dp)
                .wrapContentHeight()
                .fillMaxWidth(0.95F)
                .border(width = 2.dp, color = Color.Black),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                modifier = Modifier.padding(12.dp)
                    .fillMaxWidth(),
                fontWeight = FontWeight.Bold,
                fontSize = 24.sp,
                text = "Asset: $assetName " // Updated label
            )
            Text(
                modifier = Modifier.padding(12.dp)
                    .fillMaxWidth(),
                fontSize = 24.sp, text = position
            )
            Text(
                modifier = Modifier.padding(12.dp)
                    .fillMaxWidth(),
                fontSize = 24.sp, text = dateTime
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
           modifier = Modifier.padding(top=50.dp, end=10.dp)
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

    // The container for the Menu
    Box(modifier = Modifier.padding(top = 50.dp, start = 30.dp)) { // Adjust this padding to move it up/down
        Button(onClick = { expanded = true }) {
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
fun ErrorScreen(modifier: Modifier = Modifier) {
    Box(
        modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center
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
    Box(
        modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center
    ) {
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
                buoyFinderUiState = BuoyFinderUiState.Success(AssetData()), onGetDataClicked = {}

            )
        }
    }
}




