package com.example.example

import com.example.example.data.local.CityPreferencesRepository
import com.example.example.data.remote.OpenWeatherMapApi
import com.example.example.data.remote.WeatherResponseDto
import kotlinx.coroutines.flow.Flow
import retrofit2.HttpException
import java.io.IOException

/**
 * Repository interface exposing weather queries and local persistence.
 * Abstracts network and storage details away from the Presentation layer.
 */
interface WeatherRepository {
    suspend fun getWeather(city: String): Result<Weather>
    val lastSearchedCity: Flow<String?>
    suspend fun saveLastSearchedCity(city: String)
}

/**
 * Single source of truth for weather data.
 * Coordinates remote API requests and local preferences persistence.
 */
class WeatherRepositoryImpl(
    private val api: OpenWeatherMapApi,
    private val apiKey: String,
    private val cityPreferencesRepository: CityPreferencesRepository
) : WeatherRepository {

    override val lastSearchedCity: Flow<String?> = cityPreferencesRepository.lastSearchedCity

    override suspend fun saveLastSearchedCity(city: String) {
        cityPreferencesRepository.saveLastSearchedCity(city)
    }

    override suspend fun getWeather(city: String): Result<Weather> {
        val trimmedCity = city.trim()
        return try {
            // Attempt exact search query first (e.g. "New York") as OpenWeatherMap API resolves
            // exact city names cleanly. Fallback to appending ",US" if 404 occurs for unmatched queries.
            val response = try {
                api.getWeather(trimmedCity, apiKey)
            } catch (e: HttpException) {
                if (e.code() == 404 && !trimmedCity.contains(",")) {
                    api.getWeather("$trimmedCity,US", apiKey)
                } else {
                    throw e
                }
            }

            val weatherModel = mapDtoToDomain(response)
            if (weatherModel != null) {
                Result.success(weatherModel)
            } else {
                Result.failure(Exception("Incomplete weather data received from server"))
            }
        } catch (e: HttpException) {
            // Translate HTTP status codes into user-friendly error messages
            val errorMessage = when (e.code()) {
                404 -> "City '$trimmedCity' not found. Please enter a valid city name (e.g., New York, Denver, Miami)."
                401 -> "Invalid API Key. Please verify your OpenWeatherMap configuration."
                else -> "Server error (${e.code()}). Please try again later."
            }
            Result.failure(Exception(errorMessage, e))
        } catch (e: IOException) {
            // Protect against app crash when device has no internet or encounters socket timeout
            Result.failure(Exception("No internet connection. Please check your network.", e))
        } catch (e: Exception) {
            Result.failure(Exception(e.message ?: "An unexpected error occurred", e))
        }
    }

    /**
     * Maps raw OpenWeatherMap DTO into a presentation-ready Weather domain model.
     * Centralizes icon URL formatting and unit formatting.
     */
    private fun mapDtoToDomain(dto: WeatherResponseDto): Weather? {
        val name = dto.name ?: return null
        val main = dto.main ?: return null
        val weatherDesc = dto.weather?.firstOrNull() ?: return null

        // Format OpenWeatherMap standard icon URL (@2x for crisp high-density display)
        val iconCode = weatherDesc.icon ?: "01d"
        val iconUrl = "https://openweathermap.org/img/wn/$iconCode@2x.png"

        val temp = main.temp?.let { "${it.toInt()}°F" } ?: "N/A"
        val feelsLike = main.feelsLike?.let { "${it.toInt()}°F" } ?: "N/A"
        val humidity = main.humidity?.let { "$it%" } ?: "N/A"
        val windSpeed = dto.wind?.speed?.let { "$it mph" } ?: "N/A"
        val condition = weatherDesc.description?.replaceFirstChar { it.uppercase() } ?: "N/A"

        return Weather(
            city = name,
            temperature = temp,
            condition = condition,
            feelsLike = feelsLike,
            humidity = humidity,
            wind = windSpeed,
            iconUrl = iconUrl
        )
    }
}
