package com.aleksvgrig.mapglider

import android.app.Application
import com.google.android.libraries.places.api.Places

class MapGliderApp : Application() {
    override fun onCreate() {
        super.onCreate()
        if (!Places.isInitialized()) {
            Places.initializeWithNewPlacesApiEnabled(applicationContext, BuildConfig.MAPS_API_KEY)
        }
    }
}
