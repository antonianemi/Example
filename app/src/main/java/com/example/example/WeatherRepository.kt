package com.example.example

class WeatherRepository {
    private val weatherData = mapOf(
        "Chicago" to Weather(
            city = "Chicago",
            temperature = "72°F",
            condition = "Clear sky",
            feelsLike = "70°F",
            humidity = "45%",
            wind = "8 mph"
        ),
        "New York" to Weather(
            city = "New York",
            temperature = "68°F",
            condition = "Partly cloudy",
            feelsLike = "67°F",
            humidity = "58%",
            wind = "11 mph"
        ),
        "Los Angeles" to Weather(
            city = "Los Angeles",
            temperature = "81°F",
            condition = "Sunny",
            feelsLike = "80°F",
            humidity = "38%",
            wind = "7 mph"
        ),
        "Miami" to Weather(
            city = "Miami",
            temperature = "86°F",
            condition = "Thunderstorms",
            feelsLike = "91°F",
            humidity = "78%",
            wind = "14 mph"
        ),
        "Seattle" to Weather(
            city = "Seattle",
            temperature = "59°F",
            condition = "Light rain",
            feelsLike = "58°F",
            humidity = "82%",
            wind = "6 mph"
        ),
        "Denver" to Weather(
            city = "Denver",
            temperature = "64°F",
            condition = "Partly cloudy",
            feelsLike = "63°F",
            humidity = "32%",
            wind = "10 mph"
        )
    )

    fun getWeather(city: String): Weather? {
        return weatherData[city]
    }
}