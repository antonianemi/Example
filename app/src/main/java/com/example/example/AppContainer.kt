package com.example.example

interface AppContainer {
    val weatherRepository: WeatherRepository
}

class DefaultAppContainer : AppContainer {
    override val weatherRepository: WeatherRepository by lazy {
        MexicoWeatherRepositoryImpl()
    }
}
