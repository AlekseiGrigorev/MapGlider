package com.aleksvgrig.mapglider.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import com.aleksvgrig.mapglider.data.JoystickSideAction
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.CameraPositionState
import kotlinx.coroutines.delay
import kotlin.math.asin
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.pow
import kotlin.math.sin
import kotlin.time.Duration.Companion.milliseconds

class FlightController(
    private val cameraPositionState: CameraPositionState
) {
    var currentSpeedKmh by mutableStateOf(0f)
        private set

    private var velocity = Offset.Zero
    private var targetVelocity = Offset.Zero
    private var sideAction = JoystickSideAction.ROTATE
    
    // Physics constants
    private val acceleration = 0.05f
    
    // Movement scaling
    private val moveScale = 5.0f // Restored to original
    private val rotationScale = 2.0f // degrees per frame

    fun updateInput(input: Offset, action: JoystickSideAction) {
        targetVelocity = input
        sideAction = action
    }

    suspend fun startFlightLoop() {
        var lastTime = System.currentTimeMillis()
        var accumulatedDistance = 0.0
        var accumulatedTimeMs = 0L

        while (true) {
            val currentTime = System.currentTimeMillis()
            val deltaTimeMs = currentTime - lastTime
            lastTime = currentTime
            
            // Apply physics: accelerate towards target velocity
            velocity = Offset(
                velocity.x + (targetVelocity.x - velocity.x) * acceleration,
                velocity.y + (targetVelocity.y - velocity.y) * acceleration
            )

            // Minimal velocity threshold to stop
            if (velocity.getDistance() < 0.001f && targetVelocity.getDistance() < 0.001f) {
                velocity = Offset.Zero
            }

            if (velocity != Offset.Zero) {
                val frameDistanceMeters = applyMovement(velocity, sideAction)
                
                // Accumulate data for smooth speed display
                accumulatedDistance += frameDistanceMeters
                accumulatedTimeMs += deltaTimeMs
                
                // Update UI speed every 250ms
                if (accumulatedTimeMs >= 250) {
                    val speedMps = accumulatedDistance / (accumulatedTimeMs / 1000.0)
                    currentSpeedKmh = (speedMps * 3.6).toFloat()
                    
                    accumulatedDistance = 0.0
                    accumulatedTimeMs = 0
                }
            } else {
                currentSpeedKmh = 0f
                accumulatedDistance = 0.0
                accumulatedTimeMs = 0
            }

            delay(16.milliseconds) // ~60 FPS
        }
    }

    private fun applyMovement(vel: Offset, action: JoystickSideAction): Double {
        val currentPos = cameraPositionState.position
        val zoom = currentPos.zoom
        
        // Dynamic speed based on zoom (original formula)
        val speedMultiplier = max(1.0, 2.0.pow((20.0 - zoom))).toFloat()
        
        // Calculate distances
        val moveDistY = -vel.y * moveScale * speedMultiplier
        
        var currentLatLng = currentPos.target
        currentLatLng = currentLatLng.computeOffset(moveDistY.toDouble(), currentPos.bearing.toDouble())

        var newBearing = currentPos.bearing
        var moveDistX = 0.0
        if (action == JoystickSideAction.ROTATE) {
            newBearing = (currentPos.bearing + vel.x * rotationScale) % 360f
        } else {
            moveDistX = (vel.x * moveScale * speedMultiplier).toDouble()
            currentLatLng = currentLatLng.computeOffset(moveDistX, (currentPos.bearing + 90.0) % 360.0)
        }
        
        val newPosition = CameraPosition.Builder()
            .target(currentLatLng)
            .zoom(zoom)
            .bearing(newBearing)
            .tilt(currentPos.tilt)
            .build()

        cameraPositionState.move(CameraUpdateFactory.newCameraPosition(newPosition))
        
        return kotlin.math.sqrt(moveDistX.pow(2) + moveDistY.toDouble().pow(2))
    }
}

@Composable
fun FlightLoop(
    cameraPositionState: CameraPositionState,
    joystickOffset: Offset,
    joystickSideAction: JoystickSideAction,
    onSpeedChanged: (Float) -> Unit = {}
) {
    val controller = remember(cameraPositionState) { FlightController(cameraPositionState) }
    
    LaunchedEffect(joystickOffset, joystickSideAction) {
        controller.updateInput(joystickOffset, joystickSideAction)
    }

    LaunchedEffect(controller) {
        controller.startFlightLoop()
    }
    
    LaunchedEffect(controller.currentSpeedKmh) {
        onSpeedChanged(controller.currentSpeedKmh)
    }
}

private fun LatLng.computeOffset(distance: Double, heading: Double): LatLng {
    val radius = 6371009.0 // Earth's radius in meters
    val dist = distance / radius
    val head = Math.toRadians(heading)
    val lat1 = Math.toRadians(latitude)
    val lng1 = Math.toRadians(longitude)
    val lat2 = asin(
        sin(lat1) * cos(dist) +
                cos(lat1) * sin(dist) * cos(head)
    )
    val lng2 = lng1 + atan2(
        sin(head) * sin(dist) * cos(lat1),
        cos(dist) - sin(lat1) * sin(lat2)
    )
    return LatLng(Math.toDegrees(lat2), Math.toDegrees(lng2))
}
