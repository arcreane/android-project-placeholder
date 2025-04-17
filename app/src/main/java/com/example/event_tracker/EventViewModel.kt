package com.example.event_tracker

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.event_tracker.api.TicketmasterClient
import com.example.event_tracker.api.TicketmasterEvent
import com.google.android.gms.maps.model.LatLng
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.util.Locale

data class EventsState(
    val events: List<TicketmasterEvent> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

// City to GPS coordinate map
val cityCoordinates = mapOf(
    "Paris" to LatLng(48.8566, 2.3522),
    "London" to LatLng(51.5074, -0.1278),
    "New York" to LatLng(40.7128, -74.0060),
    "Berlin" to LatLng(52.52, 13.405),
    "Rome" to LatLng(41.9028, 12.4964),
    "Madrid" to LatLng(40.4168, -3.7038),
    "Los Angeles" to LatLng(34.0522, -118.2437),
    "Tokyo" to LatLng(35.6895, 139.6917),
    "Barcelona" to LatLng(41.3851, 2.1734),
    "Amsterdam" to LatLng(52.3676, 4.9041),
    "Sydney" to LatLng(-33.8688, 151.2093),
    "Chicago" to LatLng(41.8781, -87.6298),
    "Miami" to LatLng(25.7617, -80.1918),
    "Toronto" to LatLng(43.6532, -79.3832),
    "Vienna" to LatLng(48.2082, 16.3738),
    "Prague" to LatLng(50.0755, 14.4378),
    "Dublin" to LatLng(53.3498, -6.2603),
    "Seattle" to LatLng(47.6062, -122.3321)
)

class EventViewModel : ViewModel() {
    private val _events = MutableStateFlow<List<TicketmasterEvent>>(emptyList())
    val events: StateFlow<List<TicketmasterEvent>> = _events

    private val _loadingState = MutableStateFlow(EventsState())
    val loadingState: StateFlow<EventsState> = _loadingState

    fun fetchEvents(lat: Double, lng: Double) {
        _loadingState.value = EventsState(isLoading = true)

        viewModelScope.launch {
            try {
                val response = TicketmasterClient.api.getEventsNearYou("$lat,$lng")
                val eventsList = response._embedded?.events ?: emptyList()
                _events.value = eventsList
                _loadingState.value = EventsState(events = eventsList)
            } catch (e: Exception) {
                _loadingState.value = EventsState(error = e.localizedMessage ?: "Error fetching events")
                println("❌ Error fetching events by location: ${e.localizedMessage}")
            }
        }
    }

    fun fetchEventsByCity(city: String) {
        val key = city.trim().replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.ROOT) else it.toString() }
        val coords = cityCoordinates[key]
        if (coords != null) {
            println("📍 Fetching events for $key at ${coords.latitude}, ${coords.longitude}")
            fetchEvents(coords.latitude, coords.longitude)
        } else {
            println("⚠️ City not recognized: $key — no coordinates found.")
            _loadingState.value = EventsState(error = "City not recognized: $key")
            _events.value = emptyList()
        }
    }
}