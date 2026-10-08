package com.example.example

import android.content.Context
import com.example.example.data.local.CityPreferencesRepository
import com.example.example.data.local.CityPreferencesRepositoryImpl
import com.example.example.data.remote.OpenWeatherMapApi
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

/**
 * Manual Dependency Injection container contract.
 * Holds application-scoped singleton dependencies.
 *
 * Why Application-Level Scoping?
 * 1. Persistence & Efficiency: The container and its singletons (like [WeatherRepository]) survive
 *    configuration changes (e.g., screen rotations) and are shared across screens without
 *    recreating expensive network clients or database connections.
 * 2. Resource Management & Cancellation: Although the repository instance lives at the application level,
 *    active network calls and coroutines are invoked and managed by each screen's ViewModel via
 *    [androidx.lifecycle.ViewModel] scope. When the user leaves a screen, the ViewModel is destroyed
 *    and cancels all ongoing network requests automatically. Thus, application-scoped repositories
 *    do not cause hanging tasks or memory leaks when screens are dismissed.
 */
interface AppContainer {
    val weatherRepository: WeatherRepository
}

/**
 * Default implementation of AppContainer using lazy initializations.
 * Ensures singletons are created only when accessed for optimal startup performance.
 */
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
