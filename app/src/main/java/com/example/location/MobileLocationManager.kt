package com.example.location

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.location.Location
import android.net.Uri
import android.os.Looper
import android.util.Log
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority

/**
 * MobileLocationManager:
 * Accesses real-time location through the user's mobile device using Google Play Services,
 * and launches external Google Maps view or navigation.
 */
class MobileLocationManager(
  private val context: Context,
  private val onLocationUpdated: (lat: Double, lng: Double, altitude: Double, accuracy: Float) -> Unit
) {
  private val fusedLocationClient: FusedLocationProviderClient =
    LocationServices.getFusedLocationProviderClient(context)

  private var isTracking = false

  private val locationCallback = object : LocationCallback() {
    override fun onLocationResult(result: LocationResult) {
      val loc = result.lastLocation ?: return
      onLocationUpdated(
        loc.latitude,
        loc.longitude,
        loc.altitude,
        loc.accuracy
      )
    }
  }

  @SuppressLint("MissingPermission")
  fun startLocationUpdates() {
    if (isTracking) return
    try {
      val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 2000L)
        .setMinUpdateIntervalMillis(1000L)
        .setMinUpdateDistanceMeters(0.5f)
        .build()

      fusedLocationClient.requestLocationUpdates(
        request,
        locationCallback,
        Looper.getMainLooper()
      )
      isTracking = true

      // Get immediate last location
      fusedLocationClient.lastLocation.addOnSuccessListener { loc: Location? ->
        loc?.let {
          onLocationUpdated(it.latitude, it.longitude, it.altitude, it.accuracy)
        }
      }
    } catch (e: Exception) {
      Log.e("MobileLocationManager", "Error starting location updates", e)
    }
  }

  fun stopLocationUpdates() {
    if (!isTracking) return
    try {
      fusedLocationClient.removeLocationUpdates(locationCallback)
    } catch (e: Exception) {
      Log.e("MobileLocationManager", "Error stopping location updates", e)
    }
    isTracking = false
  }

  companion object {
    /**
     * Opens Google Maps targeting the wheelchair's current mobile GPS location
     */
    fun openGoogleMaps(context: Context, lat: Double, lng: Double, label: String = "SmartChair Real-Time Position") {
      try {
        // Geo intent specifically for Google Maps App
        val uri = Uri.parse("geo:$lat,$lng?q=$lat,$lng(${Uri.encode(label)})")
        val mapIntent = Intent(Intent.ACTION_VIEW, uri).apply {
          setPackage("com.google.android.apps.maps")
          flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        if (mapIntent.resolveActivity(context.packageManager) != null) {
          context.startActivity(mapIntent)
        } else {
          // Fallback to web browser Google Maps
          val browserUri = Uri.parse("https://www.google.com/maps/search/?api=1&query=$lat,$lng")
          val browserIntent = Intent(Intent.ACTION_VIEW, browserUri).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
          }
          context.startActivity(browserIntent)
        }
      } catch (e: Exception) {
        Log.e("MobileLocationManager", "Failed to open Google Maps", e)
      }
    }
  }
}
