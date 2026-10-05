package com.example.example

import androidx.lifecycle.ViewModel

class WeatherViewModel(
    private val repository: WeatherRepository
) : ViewModel() {

    var city: String = ""
        private set

    var weather: Weather? = null
        private set

    fun onCityChanged(value: String) {
        city = value
    }

    fun searchWeather() {
        weather = repository.getWeather(city)
    }
}