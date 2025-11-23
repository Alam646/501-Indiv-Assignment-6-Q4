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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.onSizeChanged
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

    // Game State
    var ballPos by remember { mutableStateOf(Offset(100f, 100f)) }
    var isWin by remember { mutableStateOf(false) }
    var containerSize by remember { mutableStateOf(Size.Zero) }
    
    val ballRadius = 40f
    val goalRadius = 60f

    // Derived State for Walls & Goal (only valid when we know screen size)
    val walls = remember(containerSize) {
        if (containerSize == Size.Zero) emptyList()
        else {
            val maxW = containerSize.width
            val maxH = containerSize.height
            
            //  Maze Layout
            listOf(
                // Horizontal wall near top (forcing player right)
                Rect(left = 0f, top = 400f, right = maxW * 0.7f, bottom = 450f),

                // Horizontal wall in middle (forcing player left)
                Rect(left = maxW * 0.3f, top = 900f, right = maxW, bottom = 950f),
                
                // Vertical wall on left (forcing player down further)
                Rect(left = maxW * 0.3f, top = 950f, right = maxW * 0.3f + 50f, bottom = 1400f),
                
                // Final horizontal barrier near bottom (protecting goal)
                Rect(left = 0f, top = 1600f, right = maxW * 0.6f, bottom = 1650f)
            )
        }
    }
    
    val goalPos = remember(containerSize) {
        if (containerSize == Size.Zero) Offset.Zero
        else Offset(containerSize.width - 100f, containerSize.height - 100f)
    }

    // Sensor Logic (Active only when we have valid size)
    if (containerSize != Size.Zero) {
        val maxW = containerSize.width
        val maxH = containerSize.height

        DisposableEffect(maxW, maxH) {
            val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
            val accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

            val sensorListener = object : SensorEventListener {
                override fun onSensorChanged(event: SensorEvent?) {
                    if (isWin) return

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
                        // Check X
                        var collidesX = false
                        val ballRectX = Rect(center = Offset(newX, ballPos.y), radius = ballRadius)
                        for (wall in walls) {
                            if (ballRectX.overlaps(wall)) {
                                collidesX = true
                                break
                            }
                        }
                        if (!collidesX) ballPos = ballPos.copy(x = newX)

                        // Check Y
                        var collidesY = false
                        val ballRectY = Rect(center = Offset(ballPos.x, newY), radius = ballRadius)
                        for (wall in walls) {
                            if (ballRectY.overlaps(wall)) {
                                collidesY = true
                                break
                            }
                        }
                        if (!collidesY) ballPos = ballPos.copy(y = newY)
                        
                        // 3. Win Check
                        val dx = ballPos.x - goalPos.x
                        val dy = ballPos.y - goalPos.y
                        if (sqrt(dx * dx + dy * dy) < (ballRadius + goalRadius)) {
                            isWin = true
                        }
                    }
                }

                override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
            }

            sensorManager.registerListener(sensorListener, accelerometer, SensorManager.SENSOR_DELAY_GAME)
            onDispose { sensorManager.unregisterListener(sensorListener) }
        }
    }

    // UI Rendering
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF5F5F5)) // Light Gray Background
            .onSizeChanged {
                containerSize = Size(it.width.toFloat(), it.height.toFloat())
            }
    ) {
        // Only draw if we have initialized
        if (containerSize != Size.Zero) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                // 1. Draw Goal (Glowing Effect)
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(Color(0xFF00FF00), Color(0xFF004400)),
                        center = goalPos,
                        radius = goalRadius
                    ),
                    radius = goalRadius,
                    center = goalPos
                )
                drawCircle(
                    color = Color(0x3300FF00), // Glow ring
                    radius = goalRadius + 10f,
                    center = goalPos
                )
                
                // 2. Draw Walls (3D Effect)
                for (wall in walls) {
                    // Drop Shadow
                    drawRoundRect(
                        color = Color.Black.copy(alpha = 0.2f),
                        topLeft = wall.topLeft + Offset(8f, 8f),
                        size = wall.size,
                        cornerRadius = CornerRadius(8f, 8f)
                    )
                    // Main Wall
                    drawRoundRect(
                        brush = Brush.linearGradient(
                            colors = listOf(Color(0xFF666666), Color(0xFF333333)),
                            start = wall.topLeft,
                            end = wall.bottomLeft
                        ),
                        topLeft = wall.topLeft,
                        size = wall.size,
                        cornerRadius = CornerRadius(8f, 8f)
                    )
                    // Border
                    drawRoundRect(
                        color = Color(0xFF222222),
                        topLeft = wall.topLeft,
                        size = wall.size,
                        cornerRadius = CornerRadius(8f, 8f),
                        style = Stroke(width = 2f)
                    )
                }

                // 3. Draw Ball (3D Sphere Effect)
                val ballColorStart = if (isWin) Color(0xFFFF00FF) else Color(0xFF00FFFF)
                val ballColorEnd = if (isWin) Color(0xFF880088) else Color(0xFF000088)
                
                // Shadow
                drawCircle(
                    color = Color.Black.copy(alpha = 0.3f),
                    radius = ballRadius,
                    center = ballPos + Offset(10f, 10f)
                )
                // Sphere Gradient
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(Color.White, ballColorStart, ballColorEnd),
                        center = ballPos - Offset(12f, 12f),
                        radius = ballRadius * 1.3f
                    ),
                    radius = ballRadius,
                    center = ballPos
                )
            }
            
            // Reset Button (Top Right)
            Button(
                onClick = {
                    ballPos = Offset(100f, 100f)
                    isWin = false
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6200EE)),
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(32.dp)
            ) {
                Text("Reset")
            }

            // Win Overlay
            if (isWin) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .background(Color.White.copy(alpha = 0.95f), RoundedCornerShape(16.dp))
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        androidx.compose.foundation.layout.Column(
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "YOU WIN!",
                                fontSize = 40.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF00C853)
                            )
                            androidx.compose.foundation.layout.Spacer(modifier = Modifier.padding(8.dp))
                            Button(
                                onClick = {
                                    ballPos = Offset(100f, 100f)
                                    isWin = false
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00C853))
                            ) {
                                Text("Play Again")
                            }
                        }
                    }
                }
            }
        }
    }
}
