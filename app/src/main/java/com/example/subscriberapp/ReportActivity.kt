package com.example.subscriberapp

import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import android.app.DatePickerDialog
import android.graphics.Color
import android.util.Log
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.PolylineOptions
import java.util.*

class ReportActivity : AppCompatActivity() {

    private lateinit var databaseHelper: DatabaseHelper
    private lateinit var studentId: String
    private lateinit var map: GoogleMap
    private lateinit var minSpeedTextView: TextView
    private lateinit var maxSpeedTextView: TextView
    private lateinit var avgSpeedTextView: TextView
    private lateinit var deviceIdTextView: TextView

    private var startDate: Long = 0L
    private var endDate: Long = System.currentTimeMillis()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_report)

        databaseHelper = DatabaseHelper(this)

        // Get student ID passed from the previous activity
        studentId = intent.getStringExtra("studentId") ?: return

        // Initialize UI elements
        deviceIdTextView = findViewById(R.id.deviceIdTextView)
        minSpeedTextView = findViewById(R.id.minSpeedTextView)
        maxSpeedTextView = findViewById(R.id.maxSpeedTextView)
        avgSpeedTextView = findViewById(R.id.avgSpeedTextView)

        // Set title for the report
        findViewById<TextView>(R.id.titleTextView).text = "Summary of $studentId"

        // Initialize map using SupportMapFragment
        val mapFragment = supportFragmentManager.findFragmentById(R.id.mapView) as SupportMapFragment
        mapFragment.getMapAsync { googleMap ->
            map = googleMap
            Log.d("ReportActivity", "Map is ready: $map")
            // After the map is ready, check if the time range has been selected before drawing the polyline
            if (startDate > 0 && endDate > 0) {
                loadReport()
            }
        }

        // Set up the time picker button
        findViewById<Button>(R.id.selectTimePeriodButton).setOnClickListener {
            showTimePickerDialog()
        }
    }

    private fun loadReport() {
        // Fetch speed stats for the selected device
        val stats = databaseHelper.getSpeedStatsForDevice(studentId, startDate, endDate)
        val locationData = databaseHelper.getLocationDataForDevice(studentId, startDate, endDate)

        // Display the speed stats
        minSpeedTextView.text = "Min Speed: ${stats.minSpeed} km/h"
        maxSpeedTextView.text = "Max Speed: ${stats.maxSpeed} km/h"
        avgSpeedTextView.text = "Avg Speed: ${stats.avgSpeed} km/h"
        deviceIdTextView.text = "Device ID: $studentId"

        // Display the map with the device's movement
        drawDevicePath(locationData)
    }

    private fun showTimePickerDialog() {
        // Show start date picker
        val calendar = Calendar.getInstance()

        val startDatePickerDialog = DatePickerDialog(
            this,
            { _, year, month, dayOfMonth ->
                calendar.set(year, month, dayOfMonth)
                startDate = calendar.timeInMillis
                showEndDatePickerDialog() // Show the end date picker after selecting the start date
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        )
        startDatePickerDialog.show()
    }

    private fun showEndDatePickerDialog() {
        // Show end date picker
        val calendar = Calendar.getInstance()

        val endDatePickerDialog = DatePickerDialog(
            this,
            { _, year, month, dayOfMonth ->
                calendar.set(year, month, dayOfMonth)
                endDate = calendar.timeInMillis

                // After selecting the time range, reload the report
                loadReport()  // Load the report only after both start and end dates are chosen
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        )
        endDatePickerDialog.show()
    }

    private fun drawDevicePath(locationData: List<LocationData>) {
        Log.d("ReportActivity", "Drawing polyline with ${locationData.size} points")

        // Check if location data contains any points
        if (locationData.isEmpty()) {
            Log.d("ReportActivity", "No location data to draw polyline.")
            return
        }

        val points = locationData.map { LatLng(it.latitude, it.longitude) }

        // Log the points to confirm
        points.forEach { point ->
            Log.d("ReportActivity", "Point: ${point.latitude}, ${point.longitude}")
        }

        // Draw the polyline
        val polylineOptions = PolylineOptions().addAll(points).width(5f).color(Color.BLUE)
        map.addPolyline(polylineOptions)

        // Move camera to the first point if available
        if (points.isNotEmpty()) {
            map.moveCamera(CameraUpdateFactory.newLatLngZoom(points.first(), 12f))
        } else {
            Log.d("ReportActivity", "No points available to move camera.")
        }

        // Optionally, use camera bounds to fit polyline within the map's view
        val builder = LatLngBounds.builder()
        points.forEach { builder.include(it) }
        val bounds = builder.build()
        map.moveCamera(CameraUpdateFactory.newLatLngBounds(bounds, 100))  // Add padding
    }

}
