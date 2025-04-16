package com.example.event_tracker

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.hardware.*
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import com.example.event_tracker.api.TicketmasterEvent
import com.example.event_tracker.ui.theme.Event_trackerTheme
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.*
import java.util.Locale

sealed class ScreenState {
    data object Welcome : ScreenState()
    data object GenreSelection : ScreenState()
    data object MainMap : ScreenState()
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            Event_trackerTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    var screen by remember { mutableStateOf<ScreenState>(ScreenState.Welcome) }
                    var selectedGenre by remember { mutableStateOf("") }

                    when (screen) {
                        is ScreenState.Welcome -> WelcomeScreen {
                            screen = ScreenState.GenreSelection
                        }
                        is ScreenState.GenreSelection -> GenreSelectionScreen { genre ->
                            selectedGenre = genre
                            screen = ScreenState.MainMap
                        }
                        is ScreenState.MainMap -> MapScreen(selectedGenre)
                    }
                }
            }
        }
    }
}

@Composable
fun WelcomeScreen(onNext: () -> Unit) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("🎉 Welcome to ConcertRadar!", style = MaterialTheme.typography.headlineMedium)
            Spacer(modifier = Modifier.height(24.dp))
            Button(onClick = onNext) {
                Text("Next")
            }
        }
    }
}

@Composable
fun GenreSelectionScreen(onGenreSelected: (String) -> Unit) {
    val genres = listOf("Rock", "Pop", "Classical", "Jazz", "EDM", "Hip-Hop")

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("What kind of concerts do you like?", style = MaterialTheme.typography.titleLarge)
        Spacer(modifier = Modifier.height(16.dp))
        genres.forEach { genre ->
            Button(
                onClick = { onGenreSelected(genre) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
            ) {
                Text(genre)
            }
        }
    }
}

@Composable
fun MapScreen(genre: String) {
    val context = LocalContext.current
    val viewModel = remember { EventViewModel() }
    val events by viewModel.events.collectAsState()
    var userLocation by remember { mutableStateOf<LatLng?>(null) }
    var searchText by remember { mutableStateOf("") }
    var selectedEvent by remember { mutableStateOf<TicketmasterEvent?>(null) }
    val keyboardController = LocalSoftwareKeyboardController.current

    val filteredEvents = remember(searchText, events) {
        if (searchText.isBlank()) {
            events
        } else {
            events.filter {
                val city = it._embedded.venues.firstOrNull()?.city?.name ?: ""
                city.contains(searchText, ignoreCase = true)
            }
        }
    }

    val cityCoordinates = mapOf(
        "Paris" to LatLng(48.8566, 2.3522),
        "London" to LatLng(51.5074, -0.1278),
        "New York" to LatLng(40.7128, -74.0060),
        "Berlin" to LatLng(52.52, 13.405),
        "Rome" to LatLng(41.9028, 12.4964),
        "Madrid" to LatLng(40.4168, -3.7038),
        "Los Angeles" to LatLng(34.0522, -118.2437),
        "Tokyo" to LatLng(35.6895, 139.6917)
    )

    LaunchedEffect(Unit) {
        val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
        val hasPermission = ActivityCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        if (hasPermission) {
            fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                location?.let {
                    val latLng = LatLng(it.latitude, it.longitude)
                    userLocation = latLng
                    viewModel.fetchEvents(it.latitude, it.longitude)
                }
            }
        }
    }

    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(
            userLocation ?: LatLng(48.8566, 2.3522),
            14f
        )
    }

    Column {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            CompassDisplay()
        }

        TextField(
            value = searchText,
            onValueChange = { searchText = it },
            label = { Text("Search events by city") },
            singleLine = true,
            keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(
                onSearch = {
                    val key = searchText.trim().replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.ROOT) else it.toString() }
                    val coords = cityCoordinates[key]
                    if (coords != null) {
                        viewModel.fetchEvents(coords.latitude, coords.longitude)
                    } else {
                        println("City not found: $key")
                    }
                    keyboardController?.hide()
                }
            ),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        )

        GoogleMap(
            modifier = Modifier
                .fillMaxWidth()
                .height(300.dp),
            cameraPositionState = cameraPositionState
        ) {
            userLocation?.let {
                Marker(
                    state = rememberMarkerState(position = it),
                    title = "You are here"
                )
            }

            for (event in filteredEvents) {
                val venue = event._embedded.venues.firstOrNull()
                val lat = venue?.location?.latitude?.toDoubleOrNull()
                val lng = venue?.location?.longitude?.toDoubleOrNull()
                if (lat != null && lng != null) {
                    Marker(
                        state = rememberMarkerState(position = LatLng(lat, lng)),
                        title = event.name,
                        onClick = {
                            selectedEvent = event
                            true
                        }
                    )
                }
            }
        }

        selectedEvent?.let { event ->
            AlertDialog(
                onDismissRequest = { selectedEvent = null },
                title = { Text(event.name) },
                text = {
                    Column {
                        Text("📅 Date: ${event.dates.start.localDate} ${event.dates.start.localTime ?: ""}")
                        Text("📍 City: ${event._embedded.venues.firstOrNull()?.city?.name ?: "Unknown"}")
                        Text("🏟️ Venue: ${event._embedded.venues.firstOrNull()?.name ?: "Unknown"}")
                    }
                },
                confirmButton = {
                    TextButton(onClick = {
                        val url = event.url
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                        context.startActivity(intent)
                    }) {
                        Text("More Info")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { selectedEvent = null }) {
                        Text("Close")
                    }
                }
            )
        }

        if (filteredEvents.isEmpty()) {
            Text(
                text = "No events found for \"$searchText\"",
                modifier = Modifier.padding(16.dp)
            )
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(filteredEvents) { event ->
                    Column(modifier = Modifier
                        .fillMaxWidth()
                        .clickable { selectedEvent = event }
                        .padding(8.dp)
                    ) {
                        Text("${event.name} - ${event._embedded.venues.firstOrNull()?.city?.name ?: ""}")
                    }
                }
            }
        }
    }
}

@Composable
fun CompassDisplay() {
    val context = LocalContext.current
    val sensorManager = remember {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            context.getSystemService(SensorManager::class.java)
        } else null
    }
    var azimuth by remember { mutableFloatStateOf(0f) }

    val sensorEventListener = remember {
        object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                if (event.sensor.type == Sensor.TYPE_ROTATION_VECTOR) {
                    val rotationMatrix = FloatArray(9)
                    SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values)
                    val orientationAngles = FloatArray(3)
                    SensorManager.getOrientation(rotationMatrix, orientationAngles)
                    azimuth = Math.toDegrees(orientationAngles[0].toDouble()).toFloat()
                }
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }
    }

    DisposableEffect(Unit) {
        val rotationVector = sensorManager?.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
        if (rotationVector != null) {
            sensorManager.registerListener(sensorEventListener, rotationVector, SensorManager.SENSOR_DELAY_UI)
        }
        onDispose {
            sensorManager?.unregisterListener(sensorEventListener)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(100.dp),
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(android.R.drawable.ic_menu_compass),
            contentDescription = "Compass",
            modifier = Modifier
                .size(64.dp)
                .graphicsLayer {
                    rotationZ = -azimuth
                }
        )
    }
}