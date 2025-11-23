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
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.indivassignment6q4.ui.theme.IndivAssignment6Q4Theme
import kotlin.math.sqrt

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

    // Ball State (Start near top-left)
    var ballPos by remember { mutableStateOf(Offset(100f, 100f)) }
    var isWin by remember { mutableStateOf(false) }
    
    val ballRadius = 40f
    val goalRadius = 60f

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val maxW = constraints.maxWidth.toFloat()
        val maxH = constraints.maxHeight.toFloat()
        
        // Define Goal Position (Bottom Right)
        val goalPos = remember(maxW, maxH) { Offset(maxW - 100f, maxH - 100f) }

        // Define Maze Walls (Rectangles)
        val walls = remember(maxW, maxH) {
            listOf(
                Rect(offset = Offset(0f, 300f), size = Size(600f, 50f)),     // Top horizontal
                Rect(offset = Offset(maxW - 600f, 800f), size = Size(600f, 50f)), // Middle horizontal
                Rect(offset = Offset(300f, 1200f), size = Size(50f, 400f)),   // Vertical barrier
                Rect(offset = Offset(0f, 1800f), size = Size(500f, 50f))      // Bottom horizontal
            )
        }

        // Sensor Logic
        DisposableEffect(Unit) {
            val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
            val accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

            val sensorListener = object : SensorEventListener {
                override fun onSensorChanged(event: SensorEvent?) {
                    if (isWin) return // Stop moving if won

                    event?.let {
                        val x = it.values[0]
                        val y = it.values[1]
                        val speed = 8f

                        // Calculate proposed new position
                        var newX = ballPos.x - (x * speed)
                        var newY = ballPos.y + (y * speed)

                        // 1. Screen Bounds Check
                        newX = newX.coerceIn(ballRadius, maxW - ballRadius)
                        newY = newY.coerceIn(ballRadius, maxH - ballRadius)

                        // 2. Wall Collision Check
                        // Check X movement
                        var collidesX = false
                        val ballRectX = Rect(
                            center = Offset(newX, ballPos.y),
                            radius = ballRadius
                        )
                        for (wall in walls) {
                            if (ballRectX.overlaps(wall)) {
                                collidesX = true
                                break
                            }
                        }
                        if (!collidesX) {
                            ballPos = ballPos.copy(x = newX)
                        }

                        // Check Y movement
                        var collidesY = false
                        val ballRectY = Rect(
                            center = Offset(ballPos.x, newY),
                            radius = ballRadius
                        )
                        for (wall in walls) {
                            if (ballRectY.overlaps(wall)) {
                                collidesY = true
                                break
                            }
                        }
                        if (!collidesY) {
                            ballPos = ballPos.copy(y = newY)
                        }
                        
                        // 3. Win Condition Check
                        // Calculate distance between ball and goal
                        val dx = ballPos.x - goalPos.x
                        val dy = ballPos.y - goalPos.y
                        val distance = sqrt(dx * dx + dy * dy)
                        
                        if (distance < (ballRadius + goalRadius)) {
                            isWin = true
                        }
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
            // Draw Goal
            drawCircle(
                color = Color.Green,
                radius = goalRadius,
                center = goalPos
            )
            
            // Draw Walls
            for (wall in walls) {
                drawRect(
                    color = Color.DarkGray,
                    topLeft = wall.topLeft,
                    size = wall.size
                )
            }

            // Draw Ball
            drawCircle(
                color = if (isWin) Color.Magenta else Color.Blue,
                radius = ballRadius,
                center = ballPos
            )
        }
        
        // Win Overlay
        if (isWin) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .background(Color.White.copy(alpha = 0.9f), RoundedCornerShape(16.dp))
                        .padding(32.dp)
                ) {
                    Text(
                        text = "YOU WIN!",
                        fontSize = 40.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Green
                    )
                }
            }
        }
    }
}
