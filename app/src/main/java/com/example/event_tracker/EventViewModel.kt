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

// City to GPS coordinate map
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

class EventViewModel : ViewModel() {
    private val _events = MutableStateFlow<List<TicketmasterEvent>>(emptyList())
    val events: StateFlow<List<TicketmasterEvent>> = _events

    fun fetchEvents(lat: Double, lng: Double) {
        viewModelScope.launch {
            try {
                val response = TicketmasterClient.api.getEventsNearYou("$lat,$lng")
                _events.value = response._embedded?.events ?: emptyList()
            } catch (e: Exception) {
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
            _events.value = emptyList()
        }
    }
}
