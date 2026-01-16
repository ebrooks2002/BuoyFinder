package com.github.ebrooks2002.buoyfinder.ui.screens

import android.util.Log
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.North
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp

@Composable
fun Arrow(rotation: Float?,
          modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .border(2.dp, Color.Black),
        contentAlignment = Alignment.Center
    ) {
        if (rotation != null) {
            Log.d("degrees", rotation.toString())
            Icon(
                imageVector = Icons.Default.North,
                contentDescription = "Arrow",
                modifier = Modifier
                    .size(80.dp)
                    .graphicsLayer {
                        rotationZ = rotation
                    }
            )
        }
    }
}

