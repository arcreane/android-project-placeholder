package com.example.event_tracker

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.LaunchedEffect
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.annotation.RequiresApi
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import com.example.event_tracker.api.TicketmasterEvent
import com.example.event_tracker.ui.components.*
import com.example.event_tracker.ui.theme.Event_trackerTheme
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.*

sealed class ScreenState {
    data object Welcome : ScreenState()
    data object GenreSelection : ScreenState()
    data object MainMap : ScreenState()
}

class MainActivity : ComponentActivity() {
    @RequiresApi(Build.VERSION_CODES.O)
    @OptIn(ExperimentalAnimationApi::class)
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

                    AnimatedContent(
                        targetState = screen,
                        transitionSpec = {
                            fadeIn() + slideInHorizontally { it } with
                                    fadeOut() + slideOutHorizontally { -it }
                        },
                        label = "Screen Transition"
                    ) { currentScreen ->
                        when (currentScreen) {
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
}

@Composable
fun RequestLocationPermission(onPermissionGranted: () -> Unit) {
    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) onPermissionGranted()
    }

    LaunchedEffect(Unit) {
        if (ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            onPermissionGranted()
        } else {
            launcher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }
}

@Composable
fun WelcomeScreen(onNext: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFFDF3E7)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(24.dp)
        ) {
            Text(
                text = "🎉 ConcertRadar",
                style = MaterialTheme.typography.displayLarge.copy(
                    color = Color.Black
                )
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Find live music events around you",
                style = MaterialTheme.typography.bodyLarge.copy(
                    color = Color.Black
                )
            )

            Spacer(modifier = Modifier.height(40.dp))

            Button(
                onClick = onNext,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = MaterialTheme.shapes.medium,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFB7CFA0)
                )
            ) {
                Text("Get Started", color = Color.Black)
            }
        }
    }
}


@Composable
fun GenreSelectionScreen(onGenreSelected: (String) -> Unit) {
    val genres = listOf("Rock", "Pop", "Classical", "Jazz", "EDM", "Hip-Hop", "Metal", "Country")

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFFDF3E7))
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "What music do you like?",
            style = MaterialTheme.typography.titleLarge.copy(
                color = Color.Black
            )
        )

        Spacer(modifier = Modifier.height(32.dp))

        LazyColumn {
            items(genres.chunked(2)) { rowGenres ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    rowGenres.forEach { genre ->
                        Button(
                            onClick = { onGenreSelected(genre) },
                            modifier = Modifier
                                .weight(1f)
                                .height(56.dp),
                            shape = MaterialTheme.shapes.medium,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFFB7CFA0)
                            )
                        ) {
                            Text(genre, color = Color.Black)
                        }
                    }

                    if (rowGenres.size < 2) {
                        Spacer(Modifier.weight(1f))
                    }
                }
            }
        }
    }
}


@RequiresApi(Build.VERSION_CODES.O)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapScreen(genre: String) {
    val context = LocalContext.current
    val viewModel = remember { EventViewModel() }
    val events by viewModel.events.collectAsState()
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var userLocation by remember { mutableStateOf<LatLng?>(null) }
    var searchText by remember { mutableStateOf("") }
    var selectedEvent by remember { mutableStateOf<TicketmasterEvent?>(null) }
    var permissionGranted by remember { mutableStateOf(false) }

    val filteredEvents = remember(searchText, events) {
        if (searchText.isBlank()) {
            events
        } else {
            events.filter {
                val city = it._embedded.venues.firstOrNull()?.city?.name ?: ""
                city.contains(searchText, ignoreCase = true) ||
                        it.name.contains(searchText, ignoreCase = true)
            }
        }
    }

    // Request location permission in the main composable body
    RequestLocationPermission {
        permissionGranted = true
    }

    // Fetch location and events only when permission is granted
    LaunchedEffect(permissionGranted) {
        if (permissionGranted) {
            isLoading = true
            errorMessage = null

            val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
            val hasPermission = ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED

            if (hasPermission) {
                try {
                    fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                        location?.let {
                            val latLng = LatLng(it.latitude, it.longitude)
                            userLocation = latLng
                            viewModel.fetchEvents(it.latitude, it.longitude)
                        } ?: run {
                            val defaultLocation = LatLng(48.8566, 2.3522) // Fallback to Paris
                            userLocation = defaultLocation
                            viewModel.fetchEvents(defaultLocation.latitude, defaultLocation.longitude)
                        }
                        isLoading = false
                    }.addOnFailureListener {
                        errorMessage = "Couldn't get your location. Please search for a city."
                        isLoading = false
                    }
                } catch (e: Exception) {
                    errorMessage = "Location error: ${e.localizedMessage}"
                    isLoading = false
                }
            } else {
                errorMessage = "Location permission is required to find nearby events."
                isLoading = false
            }
        }
    }

    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(
            userLocation ?: LatLng(48.8566, 2.3522),
            12f
        )
    }

    LaunchedEffect(userLocation) {
        userLocation?.let {
            cameraPositionState.position = CameraPosition.fromLatLngZoom(it, 12f)
        }
    }

    Scaffold(
        topBar = {
            SmallTopAppBar(
                title = { Text("Concerts: $genre", color = Color.Black) }, // Title in black color
                colors = TopAppBarDefaults.smallTopAppBarColors(
                    containerColor = Color(0xFFB7CFA0),  // Button color in top bar (same as button color)
                    titleContentColor = Color.Black // Title text in black color
                )
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(Color(0xFFFDF3E7)) // Background color set to #FDF3E7
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    EnhancedCompass()
                }

                ConcertSearchBar(
                    value = searchText,
                    onValueChange = { searchText = it },
                    onSearch = {
                        if (searchText.isNotBlank()) {
                            isLoading = true
                            errorMessage = null
                            viewModel.fetchEventsByCity(searchText)
                            isLoading = false
                        }
                    }
                )

                Box(modifier = Modifier.weight(1f)) {
                    when {
                        isLoading -> LoadingIndicator()
                        errorMessage != null -> ErrorState(errorMessage!!)
                        else -> {
                            Column {
                                GoogleMap(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(240.dp),
                                    cameraPositionState = cameraPositionState,
                                    properties = MapProperties(
                                        isMyLocationEnabled = permissionGranted
                                    )
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

                                if (filteredEvents.isEmpty() && !isLoading) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .weight(1f),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "No events found for \"$searchText\"",
                                            style = MaterialTheme.typography.bodyLarge,
                                            color = Color.Black // Text in black
                                        )
                                    }
                                } else {
                                    Text(
                                        text = "Nearby Events (${filteredEvents.size})",
                                        style = MaterialTheme.typography.titleMedium,
                                        modifier = Modifier.padding(16.dp, 8.dp),
                                        color = Color.Black // Text in black
                                    )

                                    LazyColumn {
                                        items(filteredEvents) { event ->
                                            EventCard(
                                                event = event,
                                                onClick = { selectedEvent = event }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            selectedEvent?.let { event ->
                EventDetailsDialog(
                    event = event,
                    onDismiss = { selectedEvent = null },
                    onMoreInfo = {
                        val url = event.url
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                        context.startActivity(intent)
                    }
                )
            }
        }
    }
}


@Composable
fun EventDetailsDialog(
    event: TicketmasterEvent,
    onDismiss: () -> Unit,
    onMoreInfo: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = event.name,
                style = MaterialTheme.typography.titleLarge,
                color = Color.Black // Title text in black
            )
        },
        text = {
            Column(modifier = Modifier.padding(8.dp)) {
                val venue = event._embedded.venues.firstOrNull()

                Text(
                    text = "📅 Date: ${event.dates.start.localDate}",
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color.Black // Text in black
                )

                Spacer(modifier = Modifier.height(8.dp))

                event.dates.start.localTime?.let { time ->
                    Text(
                        text = "⏰ Time: $time",
                        style = MaterialTheme.typography.bodyLarge,
                        color = Color.Black // Text in black
                    )

                    Spacer(modifier = Modifier.height(8.dp))
                }

                venue?.let {
                    Text(
                        text = "🏟️ Venue: ${it.name}",
                        style = MaterialTheme.typography.bodyLarge,
                        color = Color.Black // Text in black
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "📍 Location: ${it.city.name}, ${it.country.name}",
                        style = MaterialTheme.typography.bodyLarge,
                        color = Color.Black // Text in black
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onMoreInfo,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFB7CFA0) // Button color in #B7CFA0
                )
            ) {
                Text("Buy Tickets", color = Color.Black) // Text color for button is black
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("Close", color = Color.Black) // Close button text in black
            }
        },
        containerColor = Color(0xFFFDF3E7) // Background of dialog in #FDF3E7
    )
}
