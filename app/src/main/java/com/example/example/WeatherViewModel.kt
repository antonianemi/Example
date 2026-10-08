package com.example.example

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch

class WeatherViewModel(
    private val repository: WeatherRepository
) : ViewModel() {

    var city by mutableStateOf("")
        private set

    private val _uiState = MutableStateFlow<WeatherUiState>(WeatherUiState.Initial)
    val uiState: StateFlow<WeatherUiState> = _uiState.asStateFlow()

    init {
        loadLastSearchedCity()
    }

    private fun loadLastSearchedCity() {
        viewModelScope.launch {
            val savedCity = repository.lastSearchedCity.firstOrNull()
            if (!savedCity.isNullOrBlank() && _uiState.value is WeatherUiState.Initial) {
                city = savedCity
                searchWeatherInternal(savedCity)
            }
        }
    }

    fun onCityChanged(value: String) {
        city = value
    }

    fun searchWeather() {
        val trimmedCity = city.trim()
        if (trimmedCity.isBlank()) {
            _uiState.value = WeatherUiState.Error("Please enter a city name.")
            return
        }
        searchWeatherInternal(trimmedCity)
    }

    private fun searchWeatherInternal(query: String) {
        if (_uiState.value is WeatherUiState.Loading) return

        _uiState.value = WeatherUiState.Loading

        viewModelScope.launch {
            val result = repository.getWeather(query)
            result.fold(
                onSuccess = { weather ->
                    _uiState.value = WeatherUiState.Success(weather)
                    repository.saveLastSearchedCity(query)
                },
                onFailure = { error ->
                    _uiState.value = WeatherUiState.Error(
                        error.message ?: "Could not fetch weather data. Please try again."
                    )
                }
            )
        }
    }

    companion object {
        fun Factory(repository: WeatherRepository): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                WeatherViewModel(repository)
            }
        }
    }
}
