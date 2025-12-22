package com.github.ebrooks2002.buoyfinder.ui.screens

import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.ebrooks2002.buoyfinder.model.AssetData
import com.github.ebrooks2002.buoyfinder.network.SPOTApi
import kotlinx.coroutines.launch
import retrofit2.HttpException
import android.location.Location
import com.github.ebrooks2002.buoyfinder.location.LocationFinder
import com.github.ebrooks2002.buoyfinder.location.RotationSensor

sealed interface BuoyFinderUiState {
    data class Success(val assetData: AssetData) : BuoyFinderUiState
    object Error : BuoyFinderUiState
    object Loading : BuoyFinderUiState
}

class BuoyFinderViewModel : ViewModel(){
    public var buoyFinderUiState: BuoyFinderUiState by mutableStateOf(BuoyFinderUiState.Loading)
        private set
    public var userLocation: Location? by mutableStateOf(null)
        private set

    public var userRotation: Float? by mutableStateOf(null)
        private set


    fun startRotationTracking(context: android.content.Context) {
        val rotationClient = RotationSensor(context)
        viewModelScope.launch {
            rotationClient.getRotationUpdates().collect { rotation ->
                userRotation = rotation
            }
        }
    }

    fun startLocationTracking(context: android.content.Context) {
        val locationClient = LocationFinder(context)
        viewModelScope.launch {
            // Update every 2 seconds (2000ms)
            locationClient.getLocationUpdates(2000L).collect { location ->
                userLocation = location
            }
        }
    }

    init {
        getAssetData()
    }

    fun getAssetData() {
        viewModelScope.launch {
            buoyFinderUiState = BuoyFinderUiState.Loading
            buoyFinderUiState = try {
                val listResult = SPOTApi.retrofitService.getData()
                Log.d("BuoyDebug","List Result: $listResult")
                BuoyFinderUiState.Success(listResult)
            } catch (e: HttpException) {
                Log.e("BuoyViewModel","Network request failed: ${e.code()} ${e.message()}", e)
                BuoyFinderUiState.Error
            } catch (e: Exception){
                Log.e("BuoyViewModel","Error fetching message ${e.message}", e)
                BuoyFinderUiState.Error
            }
        }
    }
}