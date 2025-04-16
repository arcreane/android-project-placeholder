package com.example.event_tracker.api

data class TicketmasterResponse(
    val _embedded: EmbeddedEvents?
)

data class EmbeddedEvents(
    val events: List<TicketmasterEvent>
)

data class TicketmasterEvent(
    val name: String,
    val dates: Dates,
    val url: String,
    val _embedded: VenueInfoWrapper
)


data class Dates(
    val start: StartDate
)

data class StartDate(
    val localDate: String,
    val localTime: String?
)

data class VenueInfoWrapper(
    val venues: List<Venue>
)

data class Venue(
    val name: String,
    val city: City,
    val country: Country,
    val location: Location?
)

data class City(
    val name: String
)

data class Location(
    val latitude: String,
    val longitude: String
)

data class Country(
    val name: String
)
