package com.example.indivassignment6q4

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import com.example.indivassignment6q4.ui.theme.IndivAssignment6Q4Theme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            IndivAssignment6Q4Theme {
                GameScreen()
            }
        }
    }
}

@Composable
fun GameScreen() {
    val context = LocalContext.current

    // Ball State (Start near middle)
    var ballPos by remember { mutableStateOf(Offset(500f, 800f)) }
    val ballRadius = 40f

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val maxW = constraints.maxWidth.toFloat()
        val maxH = constraints.maxHeight.toFloat()

        // Sensor Logic
        DisposableEffect(Unit) {
            val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
            val accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

            val sensorListener = object : SensorEventListener {
                override fun onSensorChanged(event: SensorEvent?) {
                    event?.let {
                        val x = it.values[0]
                        val y = it.values[1]

                        // Speed Multiplier
                        val speed = 8f 

                        // Calculate new position
                        // X: Tilting right (negative sensor X) -> Move Right (+ screen X)
                        // Y: Tilting top-down (positive sensor Y) -> Move Down (+ screen Y)
                        var newX = ballPos.x - (x * speed)
                        var newY = ballPos.y + (y * speed)

                        // Clamp values to keep ball on screen
                        newX = newX.coerceIn(ballRadius, maxW - ballRadius)
                        newY = newY.coerceIn(ballRadius, maxH - ballRadius)

                        ballPos = Offset(newX, newY)
                    }
                }

                override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
            }

            sensorManager.registerListener(
                sensorListener,
                accelerometer,
                SensorManager.SENSOR_DELAY_GAME
            )

            onDispose {
                sensorManager.unregisterListener(sensorListener)
            }
        }

        Canvas(modifier = Modifier.fillMaxSize()) {
            // Draw Ball
            drawCircle(
                color = Color.Blue,
                radius = ballRadius,
                center = ballPos
            )
        }
    }
}
