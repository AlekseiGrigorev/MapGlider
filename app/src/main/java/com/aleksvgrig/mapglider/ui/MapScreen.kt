package com.aleksvgrig.mapglider.ui

import android.Manifest
import android.annotation.SuppressLint
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withLink
import androidx.compose.ui.text.withStyle
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.windowsizeclass.WindowHeightSizeClass
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aleksvgrig.mapglider.BuildConfig
import com.aleksvgrig.mapglider.R
import com.aleksvgrig.mapglider.data.JoystickPosition
import com.aleksvgrig.mapglider.data.JoystickSideAction
import com.aleksvgrig.mapglider.data.SettingsRepository
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapType
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.rememberCameraPositionState
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

@OptIn(ExperimentalPermissionsApi::class, ExperimentalMaterial3Api::class)
@SuppressLint("MissingPermission")
@Composable
fun MapScreen(
    windowSizeClass: WindowSizeClass,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val fusedLocationClient = remember { LocationServices.getFusedLocationProviderClient(context) }
    val settingsRepository = remember { SettingsRepository(context) }
    
    val locationPermissionsState = rememberMultiplePermissionsState(
        listOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
        )
    )

    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.builder()
            .target(LatLng(1.35, 103.87))
            .zoom(15f)
            .tilt(45f)
            .build()
    }
    
    var joystickOffset by remember { mutableStateOf(Offset.Zero) }
    var showSettings by remember { mutableStateOf(false) }
    var showAboutDialog by remember { mutableStateOf(false) }
    var currentSpeed by remember { mutableFloatStateOf(0f) }
    
    val selectedMapType by settingsRepository.mapTypeFlow
        .collectAsStateWithLifecycle(initialValue = MapType.NORMAL)
    val joystickPosition by settingsRepository.joystickPositionFlow
        .collectAsStateWithLifecycle(initialValue = JoystickPosition.CENTER)
    val joystickSideAction by settingsRepository.joystickSideActionFlow
        .collectAsStateWithLifecycle(initialValue = JoystickSideAction.ROTATE)
    val tilt by settingsRepository.tiltFlow
        .collectAsStateWithLifecycle(initialValue = 45f)
    val joystickSize by settingsRepository.joystickSizeFlow
        .collectAsStateWithLifecycle(initialValue = 1.0f)
    val joystickSensitivity by settingsRepository.joystickSensitivityFlow
        .collectAsStateWithLifecycle(initialValue = 0.5f)
    val hideButtonsInFlight by settingsRepository.hideButtonsInFlightFlow
        .collectAsStateWithLifecycle(initialValue = true)

    LaunchedEffect(tilt) {
        cameraPositionState.move(
            CameraUpdateFactory.newCameraPosition(
                CameraPosition.builder(cameraPositionState.position)
                    .tilt(tilt)
                    .build()
            )
        )
    }
    
    FlightLoop(cameraPositionState, joystickOffset, joystickSideAction, joystickSensitivity) {
        currentSpeed = it
    }

    val buttonsVisible = !hideButtonsInFlight || joystickOffset == Offset.Zero

    val uiSettings = remember(buttonsVisible) {
        MapUiSettings(
            zoomControlsEnabled = buttonsVisible,
            compassEnabled = buttonsVisible,
            myLocationButtonEnabled = false
        )
    }

    val mapProperties = remember(locationPermissionsState.allPermissionsGranted, selectedMapType) {
        MapProperties(
            mapType = selectedMapType,
            isMyLocationEnabled = locationPermissionsState.allPermissionsGranted
        )
    }

    LaunchedEffect(locationPermissionsState.allPermissionsGranted) {
        if (locationPermissionsState.allPermissionsGranted) {
            try {
                val result = fusedLocationClient.getCurrentLocation(
                    Priority.PRIORITY_HIGH_ACCURACY,
                    null
                ).await()
                result?.let {
                    cameraPositionState.animate(
                        CameraUpdateFactory.newLatLngZoom(
                            LatLng(it.latitude, it.longitude),
                            15f
                        )
                    )
                }
            } catch (_: Exception) { }
        }
    }

    val isExpanded = windowSizeClass.widthSizeClass == WindowWidthSizeClass.Expanded
    val isCompactHeight = windowSizeClass.heightSizeClass == WindowHeightSizeClass.Compact

    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            GoogleMap(
                modifier = Modifier.fillMaxSize(),
                cameraPositionState = cameraPositionState,
                properties = mapProperties,
                uiSettings = uiSettings
            )

            // Speed Overlay (Top-Left)
            Box(
                modifier = Modifier
                    .statusBarsPadding()
                    .padding(16.dp)
                    .align(Alignment.TopStart)
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f),
                    shape = MaterialTheme.shapes.medium,
                    tonalElevation = 4.dp
                ) {
                    Text(
                        text = stringResource(id = R.string.speed_format, currentSpeed.toInt()),
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            // Top-Right Buttons Overlay (Settings + My Location)
            if (buttonsVisible) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .statusBarsPadding()
                        .padding(16.dp),
                    contentAlignment = Alignment.TopEnd
                ) {
                    Column(
                        horizontalAlignment = Alignment.End
                    ) {
                        FloatingActionButton(
                            onClick = { showSettings = true },
                            modifier = Modifier.size(48.dp),
                            containerColor = MaterialTheme.colorScheme.surface,
                            contentColor = MaterialTheme.colorScheme.onSurface
                        ) {
                            Icon(Icons.Default.Settings, contentDescription = stringResource(R.string.settings_icon_desc))
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        FloatingActionButton(
                            onClick = { showAboutDialog = true },
                            modifier = Modifier.size(48.dp),
                            containerColor = MaterialTheme.colorScheme.surface,
                            contentColor = MaterialTheme.colorScheme.onSurface
                        ) {
                            Icon(Icons.AutoMirrored.Filled.HelpOutline, contentDescription = stringResource(R.string.about_button))
                        }
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        FloatingActionButton(
                            onClick = {
                                scope.launch {
                                    cameraPositionState.animate(
                                        CameraUpdateFactory.newCameraPosition(
                                            CameraPosition.builder(cameraPositionState.position)
                                                .bearing(0f)
                                                .build()
                                        )
                                    )
                                }
                            },
                            modifier = Modifier.size(48.dp),
                            containerColor = MaterialTheme.colorScheme.surface,
                            contentColor = MaterialTheme.colorScheme.onSurface
                        ) {
                            Icon(Icons.Default.Explore, contentDescription = stringResource(R.string.north_icon_desc))
                        }
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        FloatingActionButton(
                            onClick = {
                                if (locationPermissionsState.allPermissionsGranted) {
                                    scope.launch {
                                        val result = fusedLocationClient.getCurrentLocation(
                                            Priority.PRIORITY_HIGH_ACCURACY,
                                            null
                                        ).await()
                                        result?.let {
                                            cameraPositionState.animate(
                                                CameraUpdateFactory.newLatLngZoom(
                                                    LatLng(it.latitude, it.longitude),
                                                    15f
                                                )
                                            )
                                        }
                                    }
                                } else {
                                    locationPermissionsState.launchMultiplePermissionRequest()
                                }
                            },
                            modifier = Modifier.size(48.dp),
                            containerColor = MaterialTheme.colorScheme.surface,
                            contentColor = MaterialTheme.colorScheme.onSurface
                        ) {
                            Icon(Icons.Default.MyLocation, contentDescription = stringResource(R.string.my_location_icon_desc))
                        }
                    }
                }
            }

            // Joystick Overlay
            val joystickAlignment = if (isCompactHeight) {
                when (joystickPosition) {
                    JoystickPosition.LEFT -> Alignment.BottomStart
                    JoystickPosition.CENTER -> Alignment.BottomCenter
                    JoystickPosition.RIGHT -> Alignment.BottomEnd
                }
            } else {
                Alignment.BottomCenter
            }
            val joystickPaddingEnd = if (isCompactHeight && joystickPosition == JoystickPosition.RIGHT && isExpanded) 64.dp else 0.dp
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .navigationBarsPadding()
                    .padding(
                        bottom = if (isCompactHeight) 16.dp else 64.dp,
                        end = joystickPaddingEnd
                    ),
                contentAlignment = joystickAlignment
            ) {
                val baseSize = if (isCompactHeight) 100.dp else 120.dp
                val baseDotSize = if (isCompactHeight) 35.dp else 40.dp
                Joystick(
                    size = baseSize * joystickSize,
                    dotSize = baseDotSize * joystickSize,
                    onJoystickUpdate = {
                        joystickOffset = it
                    }
                )
            }

            // Settings Sheet
            if (showSettings) {
                val sheetState = rememberModalBottomSheetState(
                    skipPartiallyExpanded = isCompactHeight
                )
                ModalBottomSheet(
                    onDismissRequest = { showSettings = false },
                    sheetState = sheetState,
                    contentWindowInsets = { WindowInsets(0) }
                ) {
                    SettingsContent(
                        currentMapType = selectedMapType,
                        currentJoystickPosition = joystickPosition,
                        currentJoystickSideAction = joystickSideAction,
                        currentTilt = tilt,
                        currentJoystickSize = joystickSize,
                        currentJoystickSensitivity = joystickSensitivity,
                        hideButtonsInFlight = hideButtonsInFlight,
                        onMapTypeSelected = { 
                            scope.launch {
                                settingsRepository.saveMapType(it)
                                showSettings = false
                            }
                        },
                        onJoystickPositionSelected = {
                            scope.launch {
                                settingsRepository.saveJoystickPosition(it)
                            }
                        },
                        onJoystickSideActionSelected = {
                            scope.launch {
                                settingsRepository.saveJoystickSideAction(it)
                            }
                        },
                        onTiltChanged = {
                            scope.launch {
                                settingsRepository.saveTilt(it)
                            }
                        },
                        onJoystickSizeChanged = {
                            scope.launch {
                                settingsRepository.saveJoystickSize(it)
                            }
                        },
                        onJoystickSensitivityChanged = {
                            scope.launch {
                                settingsRepository.saveJoystickSensitivity(it)
                            }
                        },
                        onHideButtonsInFlightChanged = {
                            scope.launch {
                                settingsRepository.saveHideButtonsInFlight(it)
                            }
                        }
                    )
                }
            }
        }
    }

    if (showAboutDialog) {
        AboutDialog(onDismiss = { showAboutDialog = false })
    }
}

@Composable
fun SettingsContent(
    currentMapType: MapType,
    currentJoystickPosition: JoystickPosition = JoystickPosition.CENTER,
    currentJoystickSideAction: JoystickSideAction = JoystickSideAction.ROTATE,
    currentTilt: Float = 45f,
    currentJoystickSize: Float = 1.0f,
    currentJoystickSensitivity: Float = 0.5f,
    hideButtonsInFlight: Boolean = true,
    onMapTypeSelected: (MapType) -> Unit,
    onJoystickPositionSelected: (JoystickPosition) -> Unit = {},
    onJoystickSideActionSelected: (JoystickSideAction) -> Unit = {},
    onTiltChanged: (Float) -> Unit = {},
    onJoystickSizeChanged: (Float) -> Unit = {},
    onJoystickSensitivityChanged: (Float) -> Unit = {},
    onHideButtonsInFlightChanged: (Boolean) -> Unit = {}
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
            .padding(bottom = 32.dp)
    ) {
        Text(
            text = stringResource(R.string.map_settings_title),
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(bottom = 16.dp)
        )
        
        HorizontalDivider(modifier = Modifier.padding(bottom = 16.dp))

        Text(
            text = stringResource(R.string.tilt_label, currentTilt.toInt()),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        Slider(
            value = currentTilt,
            onValueChange = onTiltChanged,
            valueRange = 0f..90f,
            steps = 89,
            modifier = Modifier.padding(horizontal = 16.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))
        HorizontalDivider(modifier = Modifier.padding(bottom = 16.dp))

        Text(
            text = stringResource(R.string.joystick_size_label, currentJoystickSize),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        Slider(
            value = currentJoystickSize,
            onValueChange = onJoystickSizeChanged,
            valueRange = 1.0f..2.0f,
            steps = 9,
            modifier = Modifier.padding(horizontal = 16.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))
        HorizontalDivider(modifier = Modifier.padding(bottom = 16.dp))

        Text(
            text = stringResource(R.string.joystick_sensitivity_label, currentJoystickSensitivity),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        Slider(
            value = currentJoystickSensitivity,
            onValueChange = onJoystickSensitivityChanged,
            valueRange = 0.1f..1.0f,
            steps = 17, // 0.1, 0.15, 0.2, ... 1.0
            modifier = Modifier.padding(horizontal = 16.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))
        HorizontalDivider(modifier = Modifier.padding(bottom = 16.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.hide_buttons_in_flight_label),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.weight(1f)
            )
            Switch(
                checked = hideButtonsInFlight,
                onCheckedChange = onHideButtonsInFlightChanged
            )
        }

        Spacer(modifier = Modifier.height(8.dp))
        HorizontalDivider(modifier = Modifier.padding(bottom = 16.dp))
        
        Text(
            text = stringResource(R.string.map_type_label),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        
        val mapTypes = listOf(
            MapType.NORMAL to stringResource(R.string.map_type_normal),
            MapType.SATELLITE to stringResource(R.string.map_type_satellite),
            MapType.HYBRID to stringResource(R.string.map_type_hybrid),
            MapType.TERRAIN to stringResource(R.string.map_type_terrain)
        )
        
        Column(Modifier.selectableGroup()) {
            mapTypes.forEach { (type, label) ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .selectable(
                            selected = (type == currentMapType),
                            onClick = { onMapTypeSelected(type) },
                            role = Role.RadioButton
                        )
                        .padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = (type == currentMapType),
                        onClick = null
                    )
                    Spacer(Modifier.width(16.dp))
                    Text(text = label, style = MaterialTheme.typography.bodyLarge)
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        HorizontalDivider(modifier = Modifier.padding(bottom = 16.dp))

        Text(
            text = stringResource(R.string.joystick_position_label),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        val joystickPositions = listOf(
            JoystickPosition.LEFT to stringResource(R.string.joystick_left),
            JoystickPosition.CENTER to stringResource(R.string.joystick_center),
            JoystickPosition.RIGHT to stringResource(R.string.joystick_right)
        )

        Column(Modifier.selectableGroup()) {
            joystickPositions.forEach { (position, label) ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .selectable(
                            selected = (position == currentJoystickPosition),
                            onClick = { onJoystickPositionSelected(position) },
                            role = Role.RadioButton
                        )
                        .padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = (position == currentJoystickPosition),
                        onClick = null
                    )
                    Spacer(Modifier.width(16.dp))
                    Text(text = label, style = MaterialTheme.typography.bodyLarge)
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        HorizontalDivider(modifier = Modifier.padding(bottom = 16.dp))

        Text(
            text = stringResource(R.string.joystick_side_action_label),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        val joystickSideActions = listOf(
            JoystickSideAction.ROTATE to stringResource(R.string.joystick_action_rotate),
            JoystickSideAction.SHIFT to stringResource(R.string.joystick_action_shift)
        )

        Column(Modifier.selectableGroup()) {
            joystickSideActions.forEach { (action, label) ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .selectable(
                            selected = (action == currentJoystickSideAction),
                            onClick = { onJoystickSideActionSelected(action) },
                            role = Role.RadioButton
                        )
                        .padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = (action == currentJoystickSideAction),
                        onClick = null
                    )
                    Spacer(Modifier.width(16.dp))
                    Text(text = label, style = MaterialTheme.typography.bodyLarge)
                }
            }
        }
    }
}

@Composable
fun AboutDialog(onDismiss: () -> Unit) {
    val privacyPolicyUrl = "https://github.com/AlekseiGrigorev/MapGlider/blob/main/privacy-policy.md"

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(text = stringResource(R.string.about_title))
        },
        text = {
            Column {
                Text(
                    text = stringResource(R.string.app_name),
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = stringResource(R.string.version_label, BuildConfig.VERSION_NAME),
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = stringResource(R.string.api_level_label, android.os.Build.VERSION.SDK_INT),
                    style = MaterialTheme.typography.bodyMedium
                )

                Spacer(modifier = Modifier.height(16.dp))

                val annotatedString = buildAnnotatedString {
                    val text = stringResource(R.string.privacy_policy)
                    withLink(LinkAnnotation.Url(url = privacyPolicyUrl)) {
                        withStyle(
                            style = SpanStyle(
                                color = MaterialTheme.colorScheme.primary,
                                textDecoration = TextDecoration.Underline
                            )
                        ) {
                            append(text)
                        }
                    }
                }

                Text(
                    text = annotatedString,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(R.string.close_button))
            }
        }
    )
}
