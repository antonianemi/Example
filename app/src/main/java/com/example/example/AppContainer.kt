package com.example.example

import android.content.Context
import com.example.example.data.local.CityPreferencesRepository
import com.example.example.data.local.CityPreferencesRepositoryImpl
import com.example.example.data.remote.OpenWeatherMapApi
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

interface AppContainer {
    val weatherRepository: WeatherRepository
}

class DefaultAppContainer(private val context: Context) : AppContainer {

    private val baseUrl = "https://api.openweathermap.org/"

    private val retrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(baseUrl)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    private val openWeatherMapApi: OpenWeatherMapApi by lazy {
        retrofit.create(OpenWeatherMapApi::class.java)
    }

    private val cityPreferencesRepository: CityPreferencesRepository by lazy {
        CityPreferencesRepositoryImpl(context)
    }

    override val weatherRepository: WeatherRepository by lazy {
        WeatherRepositoryImpl(
            api = openWeatherMapApi,
            apiKey = BuildConfig.OPENWEATHER_API_KEY,
            cityPreferencesRepository = cityPreferencesRepository
        )
    }
}
