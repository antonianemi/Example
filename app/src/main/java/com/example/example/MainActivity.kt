package com.example.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import com.example.example.ui.theme.ExampleTheme

/**
 * Activity principal de la aplicación que actúa como punto de entrada y contenedor de la UI basada en Jetpack Compose.
 *
 * ### Decisión de Arquitectura y Puntos Críticos de Funcionamiento / Rendimiento:
 *
 * 1. **Gestión de Ciclo de Vida y Retención de Estado (`by viewModels`):**
 *    - **Por qué:** Se utiliza el delegado `by viewModels` con una fábrica personalizada (`WeatherViewModel.Factory`) para
 *      inyectar el repositorio desde el contenedor global (`ExampleApplication.container`).
 *    - **Beneficios:** Garantiza que el [WeatherViewModel] sobreviva a cambios de configuración (como rotación de pantalla),
 *      evitando pérdida de estado, peticiones de red duplicadas y reconstrucciones innecesarias.
 *
 * 2. **Diseño Inmersivo Edge-to-Edge (`enableEdgeToEdge()` + `Scaffold`):**
 *    - **Por qué:** Extiende la interfaz por debajo de las barras del sistema (status y navigation bar) alineado a las guías modernas de Android.
 *    - **Beneficios:** Proporciona una interfaz limpia e inmersiva. Aplicar `padding(innerPadding)` evita que los elementos interactivos
 *      queden tapados por los controles del sistema.
 *
 * 3. **Flujo Unidireccional de Datos (UDF) y Reactividad (`WeatherUiState`):**
 *    - **Por qué:** La UI reacciona de forma exhaustiva a un estado sellado (`Initial`, `Loading`, `Success`, `Error`).
 *    - **Beneficios:** Evita estados inconsistentes o contradictorios en la pantalla y desactiva controles durante la carga
 *      para evitar peticiones de red repetidas.
 *
 * 4. **Carga Eficiente de Recursos e Imágenes con Coil (`SubcomposeAsyncImage` + `LocalContext.current`):**
 *    - **Por qué:** Utiliza el contexto de Android mediante `LocalContext.current` para delegar la descarga e íconos de la red a Coil.
 *    - **Beneficios:** Manejo automático de caché de 2 niveles (disco y memoria RAM), optimización del ciclo de vida y slots
 *      de contingencia (`loading` y `error`) para mantener una UX fluida.
 */
class MainActivity : ComponentActivity() {
    // Delegate ViewModel creation, allowing it to survive configuration changes
    private val viewModel: WeatherViewModel by viewModels {
        // Provide a custom factory since WeatherViewModel requires a repository constructor parameter
        WeatherViewModel.Factory(
            // Retrieve the weatherRepository from the application-level dependency container
            (application as ExampleApplication).container.weatherRepository
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()

        setContent {
            ExampleTheme {
                Scaffold(
                    modifier = Modifier.fillMaxSize()
                ) { innerPadding ->
                    WeatherScreen(
                        viewModel = viewModel,
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}

@Composable
fun WeatherScreen(
    viewModel: WeatherViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Weather Search",
            style = MaterialTheme.typography.headlineLarge
        )

        Spacer(modifier = Modifier.height(24.dp))

        OutlinedTextField(
            value = viewModel.city,
            onValueChange = viewModel::onCityChanged,
            modifier = Modifier.fillMaxWidth(),
            label = { Text("City") },
            placeholder = { Text("Enter a US city (e.g., Chicago)") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { viewModel.searchWeather() })
        )

        Spacer(modifier = Modifier.height(12.dp))

        Button(
            // `onClick`: Referencia a función (function reference) que ejecuta la búsqueda de clima
            // en el ViewModel al hacer clic, manteniendo la UI desvinculada de la lógica de negocio.
            onClick = viewModel::searchWeather,
            // `modifier`: Ajusta el diseño para expandir el botón a todo el ancho disponible
            // del contenedor padre (Column), mejorando la usabilidad y el área táctil.
            modifier = Modifier.fillMaxWidth(),
            // `enabled`: Control reactivo del estado del botón según `uiState`.
            // Cuando cambia a `Loading`, se evalúa como `false` (desactivando el botón,
            // volviéndolo opaco/gris y bloqueando clics adicionales para evitar peticiones duplicadas).
            enabled = uiState !is WeatherUiState.Loading
        ) {
            Text("Search")
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Evalúa de forma exhaustiva el estado actual de la UI (`uiState`)
        when (val state = uiState) {
            // Estado inicial: Muestra un texto guía antes de que el usuario realice alguna búsqueda
            is WeatherUiState.Initial -> {
                Text(
                    text = "Search for a US city to view current weather information.",
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            // Estado de carga: Muestra un indicador circular de progreso centrado mientras se obtienen los datos
            is WeatherUiState.Loading -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
            // Estado de éxito: Muestra la tarjeta con la información meteorológica obtenida exitosamente
            is WeatherUiState.Success -> {
                WeatherCard(weather = state.weather)
            }
            // Estado de error: Muestra una tarjeta con el mensaje de error y acción para reintentar la búsqueda
            is WeatherUiState.Error -> {
                ErrorCard(
                    message = state.message,
                    onRetry = viewModel::searchWeather
                )
            }
        }
    }
}

@Composable
fun WeatherCard(
    weather: Weather,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = weather.city,
                style = MaterialTheme.typography.headlineMedium
            )

            Spacer(modifier = Modifier.height(8.dp))

            // `SubcomposeAsyncImage` (parte de la librería Coil para Jetpack Compose):
            // Carga e ilustra de manera asíncrona la imagen o ícono del clima desde la URL remota.
            SubcomposeAsyncImage(
                // `model`: Construye la petición de imagen mediante Coil ImageRequest.
                // - LocalContext.current: Obtiene el `Context` de Android actual dentro de Compose. Coil lo requiere
                //   para acceder a `context.cacheDir` (caché en disco), gestionar el límite de memoria RAM,
                //   monitorear la conexión a red y vincular la carga al ciclo de vida.
                // - .data(weather.iconUrl): Especifica la URL origen de la imagen.
                // - .crossfade(true): Aplica una animación suave de transición cuando la imagen termina de cargarse.
                model = ImageRequest.Builder(LocalContext.current)
                    .data(weather.iconUrl)
                    .crossfade(true)
                    .build(),
                contentDescription = weather.condition,
                modifier = Modifier.size(96.dp),
                // `loading`: Composable secundario que se renderiza mientras Coil descarga la imagen de internet.
                loading = {
                    Box(
                        modifier = Modifier.size(96.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(32.dp))
                    }
                },
                // `error`: Composable de contingencia (fallback) que se muestra si la descarga falla o no hay red.
                error = {
                    Text(
                        text = "🌤️",
                        style = MaterialTheme.typography.displayLarge
                    )
                }
            )

            Text(
                text = weather.temperature,
                style = MaterialTheme.typography.displayMedium
            )

            Text(
                text = weather.condition,
                style = MaterialTheme.typography.bodyLarge
            )

            Spacer(modifier = Modifier.height(24.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                WeatherDetail(
                    label = "Feels like",
                    value = weather.feelsLike
                )
                WeatherDetail(
                    label = "Humidity",
                    value = weather.humidity
                )
                WeatherDetail(
                    label = "Wind",
                    value = weather.wind
                )
            }
        }
    }
}

@Composable
fun ErrorCard(
    message: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Error",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onErrorContainer
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onErrorContainer
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = onRetry) {
                Text("Retry")
            }
        }
    }
}

@Composable
fun WeatherDetail(
    label: String,
    value: String
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall
        )
    }
}
