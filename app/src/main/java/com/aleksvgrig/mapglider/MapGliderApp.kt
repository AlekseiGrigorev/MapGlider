package com.aleksvgrig.mapglider

import android.app.Application
import android.util.Log
import com.google.android.libraries.places.api.Places

import com.google.android.gms.maps.MapsInitializer
import com.google.android.gms.maps.OnMapsSdkInitializedCallback

class MapGliderApp : Application(), OnMapsSdkInitializedCallback {
    override fun onCreate() {
        super.onCreate()
        MapsInitializer.initialize(applicationContext, MapsInitializer.Renderer.LATEST, this)
        if (!Places.isInitialized()) {
            Places.initializeWithNewPlacesApiEnabled(applicationContext, BuildConfig.MAPS_API_KEY)
        }
    }

    override fun onMapsSdkInitialized(renderer: MapsInitializer.Renderer) {
        when (renderer) {
            MapsInitializer.Renderer.LATEST -> Log.d("MapsRenderer", "The latest version of the renderer is used.")
            MapsInitializer.Renderer.LEGACY -> Log.d("MapsRenderer", "The legacy version of the renderer is used.")
        }
    }
}
