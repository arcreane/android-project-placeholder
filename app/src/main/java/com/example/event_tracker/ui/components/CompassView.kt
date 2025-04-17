package com.example.event_tracker.ui.components

import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.event_tracker.ui.theme.ConcertAccent
import com.example.event_tracker.ui.theme.ConcertPurple
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun EnhancedCompass() {
    val context = LocalContext.current
    val sensorManager = remember {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            context.getSystemService(SensorManager::class.java)
        } else null
    }
    var azimuth by remember { mutableFloatStateOf(0f) }
    val directions = listOf("N", "NE", "E", "SE", "S", "SW", "W", "NW")

    val sensorEventListener = remember {
        object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                if (event.sensor.type == Sensor.TYPE_ROTATION_VECTOR) {
                    val rotationMatrix = FloatArray(9)
                    SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values)
                    val orientationAngles = FloatArray(3)
                    SensorManager.getOrientation(rotationMatrix, orientationAngles)
                    azimuth = Math.toDegrees(orientationAngles[0].toDouble()).toFloat()
                    if (azimuth < 0) azimuth += 360f
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
            .height(80.dp),
        contentAlignment = Alignment.Center
    ) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Canvas(
                modifier = Modifier.size(80.dp)
            ) {
                val center = Offset(size.width / 2, size.height / 2)
                val radius = size.width / 2 - 10.dp.toPx()

                // Draw outer circle
                drawCircle(
                    color = Color.LightGray.copy(alpha = 0.3f),
                    radius = radius,
                    center = center,
                    style = Stroke(width = 2.dp.toPx())
                )

                // Draw direction ticks
                for (i in 0 until 360 step 45) {
                    val angle = Math.toRadians(i.toDouble())
                    val start = Offset(
                        center.x + (radius - 10.dp.toPx()) * cos(angle).toFloat(),
                        center.y + (radius - 10.dp.toPx()) * sin(angle).toFloat()
                    )
                    val end = Offset(
                        center.x + radius * cos(angle).toFloat(),
                        center.y + radius * sin(angle).toFloat()
                    )
                    drawLine(
                        color = Color.Gray,
                        start = start,
                        end = end,
                        strokeWidth = 2.dp.toPx(),
                        cap = StrokeCap.Round
                    )
                }

                // Draw north indicator
                rotate(degrees = -azimuth) {
                    drawLine(
                        color = ConcertPurple,
                        start = center,
                        end = Offset(
                            center.x,
                            center.y - radius * 0.8f
                        ),
                        strokeWidth = 3.dp.toPx(),
                        cap = StrokeCap.Round
                    )

                    // Draw south indicator
                    drawLine(
                        color = ConcertAccent,
                        start = center,
                        end = Offset(
                            center.x,
                            center.y + radius * 0.5f
                        ),
                        strokeWidth = 2.dp.toPx(),
                        cap = StrokeCap.Round
                    )

                    // Draw center dot
                    drawCircle(
                        color = ConcertPurple,
                        radius = 4.dp.toPx(),
                        center = center
                    )
                }
            }

            // Direction text
            val currentDirection = when {
                azimuth >= 337.5 || azimuth < 22.5 -> "N"
                azimuth >= 22.5 && azimuth < 67.5 -> "NE"
                azimuth >= 67.5 && azimuth < 112.5 -> "E"
                azimuth >= 112.5 && azimuth < 157.5 -> "SE"
                azimuth >= 157.5 && azimuth < 202.5 -> "S"
                azimuth >= 202.5 && azimuth < 247.5 -> "SW"
                azimuth >= 247.5 && azimuth < 292.5 -> "W"
                azimuth >= 292.5 && azimuth < 337.5 -> "NW"
                else -> "N"
            }

            Text(
                text = "${azimuth.toInt()}° $currentDirection",
                color = MaterialTheme.colorScheme.onBackground,
                fontSize = 12.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.align(Alignment.BottomCenter)
            )
        } else {
            Text(
                text = "Compass not available",
                color = MaterialTheme.colorScheme.onBackground,
                fontSize = 12.sp
            )
        }
    }
}