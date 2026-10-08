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

/**
 * ViewModel encargado de la lógica de negocio, la coordinación con [WeatherRepository] y la gestión del estado de la UI.
 *
 * ### Decisiones de Arquitectura y Puntos Críticos de Funcionamiento / Rendimiento:
 *
 * 1. **Supervivencia a Cambios de Configuración (`ViewModel`):**
 *    - **Por qué:** Hereda de [ViewModel] para asociar su ciclo de vida al contenedor de pantalla en lugar de la Activity física.
 *    - **Beneficios:** Sobrevive a rotaciones de pantalla, evitando volver a realizar peticiones HTTP a la API y preservando el estado de la UI.
 *
 * 2. **Encapsulamiento de Estado Reactivo con Unidirectional Data Flow (`StateFlow` + Backing Property):**
 *    - **Por qué:** Mantiene un `_uiState` mutable privado y expone un `uiState` público e inmutable mediante `asStateFlow()`.
 *    - **Beneficios:** Garantiza que el ViewModel sea la única fuente de verdad (*Single Source of Truth*). La UI únicamente observa
 *      el estado de manera reactiva y no puede modificarlo directamente, previniendo inconsistencias de estado.
 *
 * 3. **Manejo Seguro de Asincronismo y Ciclo de Vida (`viewModelScope`):**
 *    - **Por qué:** Todas las operaciones de I/O de disco (DataStore) y red (Retrofit) se ejecutan dentro del ámbito de `viewModelScope`.
 *    - **Beneficios:** Cuando el ViewModel se destruye, todas las corrutinas activas se cancelan automáticamente, evitando fugas
 *      de memoria, consumo innecesario de red y excepciones por intentar actualizar la UI en componentes destruidos.
 *
 * 4. **Cláusula de Guarda contra Peticiones Redundantes (Debounce):**
 *    - **Por qué:** Verifica si `_uiState.value` ya se encuentra en estado `WeatherUiState.Loading` antes de lanzar una petición.
 *    - **Beneficios:** Previene la ejecución de peticiones HTTP duplicadas y la sobrecarga del servidor si el usuario interactúa repetidamente con el botón.
 *
 * 5. **Persistencia Condicional Únicamente en Respuestas Exitosas:**
 *    - **Por qué:** La llamada a `repository.saveLastSearchedCity(query)` se realiza dentro de la rama `onSuccess`.
 *    - **Beneficios:** Evita persistir búsquedas con errores o nombres de ciudades inválidos, asegurando que la recarga automática
 *      al iniciar la app (`loadLastSearchedCity`) sea siempre sobre un término de búsqueda válido.
 */
class WeatherViewModel(
    private val repository: WeatherRepository
) : ViewModel() {

    // Mutable Compose state for the city text input field
    var city by mutableStateOf("")
        private set

    // StateFlow for reactive UI state observation
    private val _uiState = MutableStateFlow<WeatherUiState>(WeatherUiState.Initial)
    val uiState: StateFlow<WeatherUiState> = _uiState.asStateFlow()

    init {
        // Automatically load previously searched city on app startup
        loadLastSearchedCity()
    }

    /**
     * Automatically loads the previously searched city on app startup.
     * Uses `viewModelScope.launch` because:
     * 1. `Flow.firstOrNull()` is a `suspend` function requiring a coroutine context.
     * 2. Asynchronous disk I/O (DataStore) runs without blocking the UI thread.
     * 3. Coroutine execution is bound to ViewModel lifecycle for automatic cancellation.
     */
    private fun loadLastSearchedCity() {
        viewModelScope.launch {
            val savedCity = repository.lastSearchedCity.firstOrNull()
            // Guard against invalid auto-search triggers:
            // 1. Fresh install / first run: DataStore has no saved value yet and returns null.
            // 2. Corrupted persistence: Protects against invalid values or manual edits during dev/testing.
            // 3. Schema/key migrations: Prevents searches if a preference key holds empty ("") or whitespace ("  ") strings.
            // 4. Configuration changes: `_uiState.value is WeatherUiState.Initial` avoids duplicate network calls on rotation.
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
        // Prevent launching duplicate network requests if currently loading
        if (_uiState.value is WeatherUiState.Loading) return

        _uiState.value = WeatherUiState.Loading

        viewModelScope.launch {
            val result = repository.getWeather(query)
            result.fold(
                onSuccess = { weather ->
                    _uiState.value = WeatherUiState.Success(weather)
                    // Persist city ONLY after a successful API response so an invalid query
                    // does not become the next startup search term.
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
        /**
         * Fábrica para crear instancias de WeatherViewModel inyectando su dependencia (WeatherRepository).
         * Utiliza el DSL `viewModelFactory` de Jetpack Lifecycle para simplificar la creación del ViewModel
         * cuando este requiere parámetros en su constructor.
         */
        fun Factory(repository: WeatherRepository): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                // Inicializa el WeatherViewModel pasándole el repositorio requerido
                WeatherViewModel(repository)
            }
        }
    }
}
