package com.example.subscriberapp

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import org.json.JSONObject
import java.util.UUID
import com.hivemq.client.mqtt.mqtt5.Mqtt5AsyncClient
import com.hivemq.client.mqtt.mqtt5.Mqtt5Client


class MainActivity : AppCompatActivity() {

    private var client: Mqtt5AsyncClient? = null
    private lateinit var databaseHelper: DatabaseHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Initialize the database helper for SQLite
        databaseHelper = DatabaseHelper(this)

        // Configure the MQTT client
        client = Mqtt5Client.builder()
            .identifier(UUID.randomUUID().toString())
            .serverHost("broker-816034662.sundaebytestt.com")
            .serverPort(1883)
            .buildAsync()

        // Connect to the broker and subscribe to the topic
        connectToBroker()
    }

    private fun connectToBroker() {
        try {
            Log.d("SubscriberApp-ConnectBroker", "Connecting to broker...")
            client?.connect()
            Log.d("SubscriberApp-ConnectBroker", "Connected to broker")
            Toast.makeText(this, "Connected to broker", Toast.LENGTH_SHORT).show()

            // Subscribe to the "assignment/location" topic
            subscribeToTopic("assignment/location")
        } catch (e: Exception) {
            Toast.makeText(this, "An error occurred when connecting to broker", Toast.LENGTH_SHORT).show()
            Log.e("SubscriberApp-ConnectBroker", "Error connecting to broker", e)
        }
    }

    private fun subscribeToTopic(topic: String) {
        client?.let { mqttClient ->
            mqttClient.connectWith()
                .send()
                .whenComplete { _, throwable ->
                    if (throwable == null) {
                        Log.d("SubscriberApp-ConnectBroker", "Connected to broker")
                        Toast.makeText(this, "Connected to broker", Toast.LENGTH_SHORT).show()

                        mqttClient.subscribeWith()
                            .topicFilter(topic)
                            .callback { publish ->
                                val message = String(publish.payloadAsBytes, Charsets.UTF_8)
                                Log.d("SubscriberApp-Message", "Received message: $message")
                                handleIncomingMessage(message)
                            }
                            .send()
                            .whenComplete { _, subscribeThrowable ->
                                if (subscribeThrowable == null) {
                                    Log.d("SubscriberApp-Subscribe", "Subscribed to topic: $topic")
                                } else {
                                    Log.e("SubscriberApp-Subscribe", "Error subscribing to topic", subscribeThrowable)
                                }
                            }
                    } else {
                        Log.e("SubscriberApp-ConnectBroker", "Error connecting to broker", throwable)
                    }
                }
        }
    }

    private fun handleIncomingMessage(message: String) {
        try {
            // Parse the message assuming it's in JSON format
            val jsonObject = JSONObject(message)
            val studentId = jsonObject.getString("studentId")
            val speed = jsonObject.getDouble("speed")
            val timestamp = jsonObject.getLong("timestamp")
            val latitude = jsonObject.getDouble("latitude")
            val longitude = jsonObject.getDouble("longitude")

            // Save the data to the SQLite database
            databaseHelper.insertLocationData(studentId, speed, timestamp, latitude, longitude)
            Log.d("SubscriberApp-Database", "Data saved to SQLite: $message")
        } catch (e: Exception) {
            Log.e("SubscriberApp-MessageHandler", "Error parsing or saving message data", e)
        }
    }

    private fun disconnectFromBroker() {
        try {
            client?.disconnect()
            Toast.makeText(this, "Disconnected from broker", Toast.LENGTH_SHORT).show()
            Log.d("SubscriberApp", "Disconnected from broker")
        } catch (e: Exception) {
            Toast.makeText(this, "An error occurred when disconnecting from broker", Toast.LENGTH_SHORT).show()
            Log.e("SubscriberApp", "Error disconnecting from broker", e)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // Disconnect from the broker when the app is destroyed
        disconnectFromBroker()
    }
}
