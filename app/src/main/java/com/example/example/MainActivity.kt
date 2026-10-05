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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.example.ui.theme.ExampleTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ExampleTheme {
                Scaffold(
                    modifier = Modifier.fillMaxSize()
                ) { innerPadding ->
                    WeatherScreen(
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}

@Composable
fun WeatherScreen(
    modifier: Modifier = Modifier
) {
    var city by remember {
        mutableStateOf("")
    }

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
        Spacer(modifier = Modifier.height(24.dp))
        OutlinedTextField(
            value = city,
            onValueChange = { city = it },
            modifier = Modifier.fillMaxWidth(),
            label = {
                Text("City")
            },
            placeholder = {
                Text("Enter a US city")
            },
            singleLine = true
        )
        Spacer(modifier = Modifier.height(12.dp))
        Button(
            onClick = {
                // We will connect the API later.
            },
            modifier = Modifier.fillMaxWidth()
        ) { Text("Search") }
        Spacer(modifier = Modifier.height(32.dp))
        WeatherCard()
    }
}

@Composable
fun WeatherCard(
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
                text = "Chicago",
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
                text = "72°F",
                style = MaterialTheme.typography.displayMedium
            )

            Text(
                text = "Clear sky",
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
                    value = "70°F"
                )

                WeatherDetail(
                    label = "Humidity",
                    value = "45%"
                )

                WeatherDetail(
                    label = "Wind",
                    value = "8 mph"
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
        WeatherScreen()
    }
}