package com.example.event_tracker.api

import retrofit2.http.GET
import retrofit2.http.Query
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object TicketmasterClient {
    private const val BASE_URL = "https://app.ticketmaster.com/discovery/v2/"

    val api: TicketmasterService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(TicketmasterService::class.java)
    }
}

interface TicketmasterService {
    @GET("events.json")
    suspend fun getEventsNearYou(
        @Query("latlong") latLong: String,
        @Query("apikey") apiKey: String = "I8MOCz9APEXFQQe9DA5IBc8fouJrET2A"
    ): TicketmasterResponse
}
