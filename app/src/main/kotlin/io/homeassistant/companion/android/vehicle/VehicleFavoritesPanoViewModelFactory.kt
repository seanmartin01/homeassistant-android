package io.homeassistant.companion.android.vehicle

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider


class VehicleFavoritesPanoViewModelFactory(): ViewModelProvider.Factory {

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return VehicleFavoritesPanoViewModel() as T
    }
}
