package com.example.example

import com.example.example.data.local.CityPreferencesRepository
import com.example.example.data.remote.OpenWeatherMapApi
import com.example.example.data.remote.WeatherResponseDto
import kotlinx.coroutines.flow.Flow
import retrofit2.HttpException
import java.io.IOException

interface WeatherRepository {
    suspend fun getWeather(city: String): Result<Weather>
    val lastSearchedCity: Flow<String?>
    suspend fun saveLastSearchedCity(city: String)
}

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
        return try {
            val queryCity = if (city.contains(",")) city else "$city,US"
            val response = api.getWeather(queryCity, apiKey)

            val weatherModel = mapDtoToDomain(response)
            if (weatherModel != null) {
                Result.success(weatherModel)
            } else {
                Result.failure(Exception("Incomplete weather data received from server"))
            }
        } catch (e: HttpException) {
            val errorMessage = when (e.code()) {
                404 -> "City not found. Please check the spelling and try again."
                401 -> "Invalid API Key. Please verify your OpenWeatherMap configuration."
                else -> "Server error (${e.code()}). Please try again later."
            }
            Result.failure(Exception(errorMessage, e))
        } catch (e: IOException) {
            Result.failure(Exception("No internet connection. Please check your network.", e))
        } catch (e: Exception) {
            Result.failure(Exception(e.message ?: "An unexpected error occurred", e))
        }
    }

    private fun mapDtoToDomain(dto: WeatherResponseDto): Weather? {
        val name = dto.name ?: return null
        val main = dto.main ?: return null
        val weatherDesc = dto.weather?.firstOrNull() ?: return null

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
