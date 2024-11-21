package com.example.subscriberapp

import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.Polyline
import com.google.android.gms.maps.model.PolylineOptions

class MapManager(private val googleMap: GoogleMap) {

    private val devicePolylines = mutableMapOf<String, Polyline>()

    // Adds a polyline for a device
    fun addDevicePath(studentId: String, path: List<Pair<Double, Double>>) {
        val polylineOptions = PolylineOptions()
        path.forEach { (lat, lng) -> polylineOptions.add(LatLng(lat, lng)) }
        val polyline = googleMap.addPolyline(polylineOptions)
        devicePolylines[studentId] = polyline
    }

    // Updates the device's polyline with new location
    fun updateDeviceLocation(studentId: String, newLocation: Pair<Double, Double>) {
        devicePolylines[studentId]?.points?.add(LatLng(newLocation.first, newLocation.second))
    }

    // Focuses the map on the device's path
    fun focusOnDevice(studentId: String) {
        devicePolylines[studentId]?.let { polyline ->
            val bounds = LatLngBounds.builder()
            polyline.points.forEach { bounds.include(it) }
            googleMap.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds.build(), 100))
        }
    }
}
