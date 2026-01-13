/**
 * The entry point for the Buoy Finder app.
 * Creates an instance of the BuoyFinderApp() inside the onCreate function.
 *
 * @author Ethan Brooks
 */

package com.github.ebrooks2002.buoyfinder
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.github.ebrooks2002.buoyfinder.ui.theme.BuoyFinderTheme
import com.github.ebrooks2002.buoyfinder.ui.BuoyFinderApp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        org.maplibre.android.MapLibre.getInstance(this)
        enableEdgeToEdge()
        setContent {
            BuoyFinderTheme {
                    BuoyFinderApp()
            }
        }
    }
}
