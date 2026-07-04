package com.aleksvgrig.mapglider.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
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
    private var velocity = Offset.Zero
    private var targetVelocity = Offset.Zero
    private var sideAction = JoystickSideAction.ROTATE
    
    // Physics constants
    private val acceleration = 0.05f
    
    // Movement scaling
    private val moveScale = 5.0f // meters per frame at max zoom
    private val rotationScale = 2.0f // degrees per frame

    fun updateInput(input: Offset, action: JoystickSideAction) {
        targetVelocity = input
        sideAction = action
    }

    suspend fun startFlightLoop() {
        while (true) {
            // Apply physics: accelerate towards target velocity using internal linear interpolation logic
            velocity = Offset(
                velocity.x + (targetVelocity.x - velocity.x) * acceleration,
                velocity.y + (targetVelocity.y - velocity.y) * acceleration
            )

            // Minimal velocity threshold to stop
            if (velocity.getDistance() < 0.001f && targetVelocity.getDistance() < 0.001f) {
                velocity = Offset.Zero
            }

            if (velocity != Offset.Zero) {
                applyMovement(velocity, sideAction)
            }

            delay(16.milliseconds) // ~60 FPS
        }
    }

    private fun applyMovement(vel: Offset, action: JoystickSideAction) {
        val currentPos = cameraPositionState.position
        val zoom = currentPos.zoom
        
        // Dynamic speed based on zoom (higher altitude = faster movement)
        val speedMultiplier = max(1.0, 2.0.pow((20.0 - zoom))).toFloat()
        
        // Calculate new LatLng (Forward/Backward)
        val moveDistY = -vel.y * moveScale * speedMultiplier
        var currentLatLng = currentPos.target
        currentLatLng = currentLatLng.computeOffset(moveDistY.toDouble(), currentPos.bearing.toDouble())

        // Calculate new Bearing OR Strafe
        var newBearing = currentPos.bearing
        if (action == JoystickSideAction.ROTATE) {
            newBearing = (currentPos.bearing + vel.x * rotationScale) % 360f
        } else {
            val moveDistX = vel.x * moveScale * speedMultiplier
            currentLatLng = currentLatLng.computeOffset(moveDistX.toDouble(), (currentPos.bearing + 90.0) % 360.0)
        }
        
        val newPosition = CameraPosition.Builder()
            .target(currentLatLng)
            .zoom(zoom)
            .bearing(newBearing)
            .tilt(currentPos.tilt)
            .build()

        cameraPositionState.move(CameraUpdateFactory.newCameraPosition(newPosition))
    }
}

@Composable
fun FlightLoop(
    cameraPositionState: CameraPositionState,
    joystickOffset: Offset,
    joystickSideAction: JoystickSideAction
) {
    val controller = remember(cameraPositionState) { FlightController(cameraPositionState) }
    
    LaunchedEffect(joystickOffset, joystickSideAction) {
        controller.updateInput(joystickOffset, joystickSideAction)
    }

    LaunchedEffect(Unit) {
        controller.startFlightLoop()
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
