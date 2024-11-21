package com.example.subscriberapp

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.PolylineOptions
import com.hivemq.client.mqtt.MqttGlobalPublishFilter
import com.hivemq.client.mqtt.mqtt5.Mqtt5BlockingClient
import com.hivemq.client.mqtt.mqtt5.Mqtt5Client
import com.hivemq.client.mqtt.mqtt5.exceptions.Mqtt5ConnAckException
import kotlinx.coroutines.*
import java.util.UUID

class MainActivity : AppCompatActivity(), OnMapReadyCallback {

    private var client: Mqtt5BlockingClient? = null
    private lateinit var googleMap: GoogleMap
    private lateinit var databaseHelper: DatabaseHelper
    private lateinit var recyclerView: RecyclerView
    private lateinit var deviceAdapter: DeviceAdapter

    private lateinit var deviceList: List<Device>
    private val topic = "assignment/location"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        Log.d("SubscriberApp", "App started, initializing...")

        // Initialize the database helper for SQLite
        databaseHelper = DatabaseHelper(this)

        // Initialize RecyclerView
        recyclerView = findViewById(R.id.deviceListRecyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)

        Log.d("SubscriberApp", "RecyclerView initialized.")

        // Fetch device stats and populate the list
        deviceList = getDeviceList()
        deviceAdapter = DeviceAdapter(deviceList) { studentId ->
            navigateToReportScreen(studentId)
        }
        recyclerView.adapter = deviceAdapter

        // Initialize Google Maps
        val mapFragment = supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        // Configure and connect the MQTT client
        setupMqttClient()
        connectToBroker()
    }

    private fun getDeviceList(): List<Device> {
        val devices = mutableListOf<Device>()
        val studentIds = databaseHelper.getAllStudentIds()

        // Set the date range (e.g., last 24 hours)
        val endDate = System.currentTimeMillis()  // Current time
        val startDate = endDate - 86400000L // 24 hours ago

        for (studentId in studentIds) {
            val stats = databaseHelper.getSpeedStatsForDevice(studentId, startDate, endDate)
            devices.add(Device(studentId, stats.minSpeed, stats.maxSpeed))
        }
        return devices
    }

    private fun setupMqttClient() {
        client = Mqtt5Client.builder()
            .identifier(UUID.randomUUID().toString())
            .serverHost("broker-816034662.sundaebytestt.com")
            .serverPort(1883)
            .build()
            .toBlocking()
    }

    private fun connectToBroker() {
        Log.d("SubscriberApp", "Attempting to connect to broker...")
        try {
            client?.connect()
            Log.d("SubscriberApp", "Connected to broker successfully")
            Toast.makeText(this, "Connected to broker", Toast.LENGTH_SHORT).show()
            subscribeToTopic(topic)
        } catch (e: Mqtt5ConnAckException) {
            Log.e("SubscriberApp", "Connection rejected: ${e.message}", e)
            Toast.makeText(this, "Connection rejected: ${e.message}", Toast.LENGTH_LONG).show()
        } catch (e: Exception) {
            Log.e("SubscriberApp", "Error connecting to broker", e)
            Toast.makeText(this, "Error connecting to broker", Toast.LENGTH_SHORT).show()
        }
    }

    private fun subscribeToTopic(topic: String) {
        try {
            // Subscribe to the topic
            client?.subscribeWith()
                ?.topicFilter(topic)
                ?.qos(com.hivemq.client.mqtt.datatypes.MqttQos.AT_LEAST_ONCE)
                ?.send()
            Log.d("SubscriberApp", "Subscribed to topic: $topic")

            // Start listening for messages in a background thread
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    client?.publishes(MqttGlobalPublishFilter.ALL)?.use { publishes ->
                        while (true) {
                            val publish = publishes.receive() // Blocking call to receive messages
                            val payload = publish.payload.orElse(null)?.let { buffer ->
                                val byteArray = ByteArray(buffer.remaining())
                                buffer.get(byteArray)
                                byteArray
                            }

                            val message = payload?.let { String(it, Charsets.UTF_8) }
                            if (message != null) {
                                Log.d("SubscriberApp", "Received message: $message")
                                withContext(Dispatchers.Main) {
                                    handleIncomingMessage(message)
                                }
                            }
                        }
                    }
                } catch (e: Exception) {
                    Log.e("SubscriberApp", "Error receiving messages", e)
                }
            }
        } catch (e: Exception) {
            Log.e("SubscriberApp", "Error subscribing to topic", e)
            Toast.makeText(this, "Error subscribing to topic", Toast.LENGTH_SHORT).show()
            retrySubscription(topic)
        }
    }

    private fun retrySubscription(topic: String) {
        GlobalScope.launch {
            delay(3000) // Retry after 3 seconds
            Log.d("SubscriberApp", "Retrying subscription to topic: $topic")
            Toast.makeText(this@MainActivity, "Retrying subscription...", Toast.LENGTH_SHORT).show()
            subscribeToTopic(topic)
        }
    }

    private fun handleIncomingMessage(message: String) {
        Log.d("SubscriberApp", "Received MQTT message: $message")
        try {
            val parts = message.split("|")
            if (parts.size < 4) {
                Log.e("SubscriberApp", "Invalid message format: $message")
                return
            }

            val studentId = parts[0].substringAfter(":").trim()
            val speed = parts[1].substringAfter(":").substringBefore(" km/h").trim().toDouble()
            val timestamp = parts[2].substringAfter(":").trim().toLong()
            val locationParts = parts[3].substringAfter(":").split(",")
            val latitude = locationParts[0].trim().toDouble()
            val longitude = locationParts[1].trim().toDouble()

            Log.d("SubscriberApp", "Parsed data - StudentId: $studentId, Speed: $speed, Timestamp: $timestamp, Latitude: $latitude, Longitude: $longitude")

            // Perform database operation on a background thread
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    databaseHelper.insertLocationData(studentId, speed, timestamp, latitude, longitude)

                    // On success, update the UI on the main thread
                    withContext(Dispatchers.Main) {
                        Log.d("SubscriberApp", "Data successfully stored in SQLite")
                        Toast.makeText(this@MainActivity, "Data stored successfully", Toast.LENGTH_SHORT).show()
                        loadDevices() // Refresh devices
                        deviceList = getDeviceList()  // Re-fetch updated device list
                        updateRecyclerView(deviceList) // Update RecyclerView
                    }
                } catch (e: Exception) {
                    // Handle any errors that occurred during database insertion
                    withContext(Dispatchers.Main) {
                        Log.e("SubscriberApp", "Failed to store data in SQLite", e)
                        Toast.makeText(this@MainActivity, "Failed to store data", Toast.LENGTH_LONG).show()
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("SubscriberApp", "Error parsing or saving message data", e)
            Toast.makeText(this@MainActivity, "Error processing data", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onMapReady(map: GoogleMap) {
        Log.d("SubscriberApp", "Google Map is ready.")
        googleMap = map

        // Fetch location data for all devices and plot their paths
        val studentIds = databaseHelper.getAllStudentIds()
        Log.d("SubscriberApp", "Fetched ${studentIds.size} student devices for path rendering.")

        for (studentId in studentIds) {
            val locations = databaseHelper.getLocationDataForDevice(studentId)
            if (locations.isEmpty()) {
                Log.w("SubscriberApp", "No location data found for student ID: $studentId")
            } else {
                val pathData = locations.map { LatLng(it.latitude, it.longitude) }
                drawPolylineOnMap(pathData)
            }
        }
    }

    private fun drawPolylineOnMap(pathPoints: List<LatLng>) {
        if (pathPoints.isNotEmpty()) {
            val polylineOptions = PolylineOptions().addAll(pathPoints).color(Color.BLUE).width(5f)
            googleMap.addPolyline(polylineOptions)

            // Optionally, move the camera to show the entire path
            val bounds = LatLngBounds.builder()
            pathPoints.forEach { bounds.include(it) }
            googleMap.moveCamera(CameraUpdateFactory.newLatLngBounds(bounds.build(), 50))
        }
    }

    private fun loadDevices() {
        val devices = databaseHelper.getAllDevices()
        if (devices.isNotEmpty()) {
            updateRecyclerView(devices)
        }
    }

    private fun updateRecyclerView(devices: List<Device>) {
        deviceAdapter.updateDevices(devices)
    }

    private fun navigateToReportScreen(studentId: String) {
        val intent = Intent(this, ReportActivity::class.java)
        intent.putExtra("studentId", studentId)
        startActivity(intent)
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d("SubscriberApp", "Activity destroyed, disconnecting from broker...")
        disconnectFromBroker()
    }

    private fun disconnectFromBroker() {
        try {
            client?.disconnect()
            Log.d("SubscriberApp", "Disconnected from broker")
            Toast.makeText(this, "Disconnected from broker", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Log.e("SubscriberApp", "Error disconnecting from broker", e)
        }
    }
}
