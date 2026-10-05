package com.example.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.example.ui.theme.ExampleTheme

class MainActivity : ComponentActivity() {

    private val viewModel = WeatherViewModel(
        WeatherRepository()
    )

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
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Weather",
            style = MaterialTheme.typography.headlineLarge
        )

        Spacer(
            modifier = Modifier.height(24.dp)
        )

        OutlinedTextField(
            value = viewModel.city,
            onValueChange = viewModel::onCityChanged,
            modifier = Modifier.fillMaxWidth(),
            label = {
                Text("City")
            },
            placeholder = {
                Text("Enter a US city")
            },
            singleLine = true
        )

        Spacer(
            modifier = Modifier.height(12.dp)
        )

        Button(
            onClick = viewModel::searchWeather,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Search")
        }

        Spacer(
            modifier = Modifier.height(32.dp)
        )

        viewModel.weather?.let { weather ->
            WeatherCard(weather)
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

            Spacer(
                modifier = Modifier.height(16.dp)
            )

            Text(
                text = "☀️",
                style = MaterialTheme.typography.displayLarge
            )

            Text(
                text = weather.temperature,
                style = MaterialTheme.typography.displayMedium
            )

            Text(
                text = weather.condition,
                style = MaterialTheme.typography.bodyLarge
            )

            Spacer(
                modifier = Modifier.height(24.dp)
            )

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

@Preview(showBackground = true)
@Composable
fun WeatherScreenPreview() {
    ExampleTheme {
        val repository = WeatherRepository()
        val viewModel = WeatherViewModel(repository)
        WeatherScreen(
            viewModel = viewModel
        )
    }
}