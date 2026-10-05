package com.example.example

import androidx.lifecycle.ViewModel
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

class WeatherViewModel(
    private val repository: WeatherRepository
) : ViewModel() {

    var city by mutableStateOf("")
        private set

    var weather by mutableStateOf<Weather?>(null)
        private set

    fun onCityChanged(value: String) {
        city = value
    }

    fun searchWeather() {
        weather = repository.getWeather(city)
    }
}