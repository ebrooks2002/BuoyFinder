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


sealed interface BuoyFinderUiState {
    data class Success(val assetData: AssetData) : BuoyFinderUiState
    object Error : BuoyFinderUiState
    object Loading : BuoyFinderUiState
    object Idle : BuoyFinderUiState
}

class BuoyFinderViewModel : ViewModel(){

    var buoyFinderUiState: BuoyFinderUiState by mutableStateOf(BuoyFinderUiState.Idle)
        private set


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