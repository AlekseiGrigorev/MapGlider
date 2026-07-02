# MapGlider: Satellite Flight

## Project Brief

MapGlider: Satellite Flight. An application for flying over a Google Map controlled by a virtual joystick.

MapGlider is an immersive flight simulation app that allows users to soar over the world using high-resolution satellite imagery. By combining the power of Google Maps with intuitive virtual controls, it provides a "bird's-eye view" exploration experience.

### Features

* **Immersive Satellite Exploration**: Full-screen integration of high-resolution satellite maps for a realistic aerial perspective.
* **Virtual Joystick Flight Control**: An intuitive on-screen joystick that allows users to "fly" the camera in any direction, including altitude and tilt adjustments.
* **Dynamic Gliding Physics**: Smooth, momentum-based camera movements that simulate the feeling of gliding through the air rather than just panning a map.

### High-Level Technical Stack

* **Language**: Kotlin
* **UI Framework**: Jetpack Compose with Material Design 3 (M3)
* **Navigation**: Jetpack Navigation 3 (State-driven architecture)
* **Adaptive Strategy**: Compose Material Adaptive library (for optimized layouts across handsets, foldables, and tablets)
* **Map Engine**: Google Maps SDK for Android
* **Concurrency**: Kotlin Coroutines & Flow for smooth camera updates and input processing