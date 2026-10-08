package com.example.example

import android.content.Context
import com.example.example.data.local.CityPreferencesRepository
import com.example.example.data.local.CityPreferencesRepositoryImpl
import com.example.example.data.remote.OpenWeatherMapApi
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

/**
 * Contrato del contenedor de Inyección de Dependencias Manual (*Manual Dependency Injection*).
 * Mantiene dependencias de tipo Singleton asociadas al ciclo de vida de la Aplicación ([DefaultAppContainer]).
 *
 * ### ¿Por qué Scoping a Nivel de Aplicación?
 * 1. **Persistencia y Eficiencia:** El contenedor y sus Singletons (como [WeatherRepository], [Retrofit] y DataStore)
 *    sobreviven a cambios de configuración (ej. rotaciones de pantalla) y se comparten entre múltiples pantallas sin
 *    recrear clientes HTTP de red ni conexiones a bases de datos pesadas.
 * 2. **Gestión de Recursos y Cancelación:** Aunque las instancias del repositorio viven a nivel de aplicación,
 *    las llamadas a red activas y las corrutinas son invocadas y gestionadas por el `viewModelScope` de cada pantalla.
 *    Cuando el usuario abandona una pantalla, el ViewModel se destruye y cancela automáticamente todas sus corrutinas
 *    en ejecución. Por lo tanto, los repositorios persistentes no provocan tareas colgadas ni fugas de memoria.
 *
 * ### Riesgos y Consecuencias de NO considerar la separación de Scopes:
 *
 * - **Fugas de Memoria (*Memory Leaks*):** Almacenar referencias de objetos con ciclo de vida corto (ej. `Activity`, `View` o `Context` de vista)
 *   dentro de Singletons o componentes de Application Scope impide que el recolector de basura (*Garbage Collector*) libere la RAM
 *   al cerrar o rotar la pantalla, derivando eventualmente en un `OutOfMemoryError` (OOM).
 *
 * - **Tareas Huérfanas (*Hanging Tasks / Zombie Coroutines*):** Ejecutar operaciones asíncronas o peticiones HTTP usando un
 *   Scope de aplicación (`GlobalScope` o `applicationScope`) en lugar de `viewModelScope` provoca que las tareas continúen
 *   corriendo en segundo plano aunque el usuario haya salido de la pantalla, malgastando datos móviles, batería y CPU.
 *
 * - **Crashes por Interfaz Destruida:** Intentar notificar callbacks o actualizar la UI desde corrutinas sin cancelar asociadas a
 *   pantallas o vistas que ya han sido destruidas por el sistema operativo.
 *
 * - **Peticiones Duplicadas por Rotación:** Ejecutar peticiones asíncronas en el Scope de la Vista/Activity provoca que cada cambio
 *   de orientación de pantalla cancele y vuelva a lanzar la petición HTTP desde cero, generando lentitud y sobrecarga en el servidor.
 *
 * - **Contaminación de Estado (*State Leak*):** Guardar datos temporales de la interfaz (como texto ingresado en un formulario) en
 *   un Singleton a nivel de aplicación provoca que los datos persistan indebidamente si el usuario cierra y vuelve a abrir la pantalla más tarde.
 */
interface AppContainer {
    val weatherRepository: WeatherRepository
}

/**
 * Implementación predeterminada de [AppContainer] que utiliza inicializaciones perezosas (`by lazy`).
 *
 * ### Consideraciones de Diseño y Escalabilidad:
 *
 * 1. **Naturaleza Sin Estado (*Stateless Services*):**
 *    Los objetos expuestos en este contenedor (como [Retrofit], [WeatherRepository] y repositorios de preferencias)
 *    son servicios **stateless** o thread-safe. Múltiples ViewModels pueden utilizarlos simultáneamente sin necesidad de
 *    limpiar o reiniciar su estado entre llamadas, ya que el estado temporal de la pantalla reside únicamente en el [androidx.lifecycle.ViewModel].
 *
 * 2. **Inicialización Perezosa (`by lazy`):**
 *    Garantiza que los Singletons pesados (clientes de red, bases de datos o repositorios) se instancien únicamente
 *    cuando son accedidos por primera vez, reduciendo el tiempo de arranque de la aplicación (*App Startup Time*).
 *
 * 3. **Componentes Futuros para Concentrar en `AppContainer`:**
 *    - **Logging & Analytics:** `AnalyticsTracker` o `CrashLogger` para registrar eventos de forma desacoplada.
 *    - **Cliente HTTP e Interceptores:** `OkHttpClient` con `HttpLoggingInterceptor`, `AuthInterceptor` (tokens/API Keys) y `NetworkMonitor`.
 *    - **Persistencia Pesada:** Bases de datos locales de Room (`AppDatabase`) y sus DAOs.
 *    - **Gestión de Sesión:** `SessionManager` o `EncryptedSharedPreferences` para autenticación de usuarios.
 *    - **Servicios del Sistema:** `LocationProvider` (GPS) o `WorkManager` (tareas en segundo plano).
 *    - **Inyección de Hilos:** `DispatcherProvider` para parametrizar `Dispatchers.IO` y facilitar Pruebas Unitarias.
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
