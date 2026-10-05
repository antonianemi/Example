package com.example.example

class WeatherRepository {

    fun getWeather(city: String): Weather {
        return Weather(
            city = city,
            temperature = "72°F",
            condition = "Clear sky",
            feelsLike = "70°F",
            humidity = "45%",
            wind = "8 mph"
        )
    }
}