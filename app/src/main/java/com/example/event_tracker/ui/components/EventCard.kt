package com.example.event_tracker.ui.components

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.event_tracker.api.TicketmasterEvent
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun EventCard(
    event: TicketmasterEvent,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = event.name,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(4.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Format date nicely
                val dateString = try {
                    val date = LocalDate.parse(event.dates.start.localDate)
                    val formatter = DateTimeFormatter.ofPattern("EEE, MMM d, yyyy")
                    formatter.format(date)
                } catch (e: Exception) {
                    event.dates.start.localDate
                }

                Text(
                    text = "📅 $dateString",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.weight(1f))

                event.dates.start.localTime?.let { time ->
                    val formattedTime = if (time.length >= 5) {
                        "${time.substring(0, 5)} ${if (time.substring(0, 2).toInt() >= 12) "PM" else "AM"}"
                    } else {
                        time
                    }
                    Text(
                        text = "⏰ $formattedTime",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            val venue = event._embedded.venues.firstOrNull()
            venue?.let {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "📍 ${it.name}, ${it.city.name}, ${it.country.name}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}