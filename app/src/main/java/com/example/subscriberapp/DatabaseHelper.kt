package com.example.subscriberapp

import android.annotation.SuppressLint
import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.util.Log
import com.google.android.gms.maps.model.LatLng

class DatabaseHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        private const val DATABASE_NAME = "subscriberApp.db"
        private const val DATABASE_VERSION = 1
        private const val TABLE_NAME = "LocationData"
        private const val COL_ID = "id"
        private const val COL_STUDENT_ID = "studentId"
        private const val COL_SPEED = "speed"
        private const val COL_TIMESTAMP = "timestamp"
        private const val COL_LATITUDE = "latitude"
        private const val COL_LONGITUDE = "longitude"
    }

    override fun onCreate(db: SQLiteDatabase?) {
        val createTable = """
            CREATE TABLE $TABLE_NAME (
                $COL_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COL_STUDENT_ID TEXT,
                $COL_SPEED REAL,
                $COL_TIMESTAMP INTEGER,
                $COL_LATITUDE REAL,
                $COL_LONGITUDE REAL
            )
        """
        db?.execSQL(createTable)
        Log.d("DatabaseHelper", "Database and table created successfully")
    }

    override fun onUpgrade(db: SQLiteDatabase?, oldVersion: Int, newVersion: Int) {
        db?.execSQL("DROP TABLE IF EXISTS $TABLE_NAME")
        onCreate(db)
        Log.d("DatabaseHelper", "Database upgraded from version $oldVersion to $newVersion")
    }

    // Insert location data into the database
    fun insertLocationData(studentId: String, speed: Double, timestamp: Long, latitude: Double, longitude: Double) {
        val db = writableDatabase
        val contentValues = ContentValues().apply {
            put(COL_STUDENT_ID, studentId)
            put(COL_SPEED, speed)
            put(COL_LATITUDE, latitude)
            put(COL_LONGITUDE, longitude)
            put(COL_TIMESTAMP, timestamp)
        }
        db.insert(TABLE_NAME, null, contentValues)
        db.close()
    }

    // Fetch location data for a specific device/student within a time range
    fun getLocationDataForDevice(studentId: String, startDate: Long, endDate: Long): List<LocationData> {
        val db: SQLiteDatabase = this.readableDatabase
        val cursor: Cursor = db.rawQuery(
            "SELECT latitude, longitude FROM $TABLE_NAME WHERE $COL_STUDENT_ID = ? AND $COL_TIMESTAMP BETWEEN ? AND ? ORDER BY $COL_TIMESTAMP ASC",
            arrayOf(studentId, startDate.toString(), endDate.toString())
        )

        val locationDataList = mutableListOf<LocationData>()
        while (cursor.moveToNext()) {
            val latitude = cursor.getDouble(cursor.getColumnIndexOrThrow("latitude"))
            val longitude = cursor.getDouble(cursor.getColumnIndexOrThrow("longitude"))
            locationDataList.add(LocationData(latitude, longitude))
        }
        cursor.close()
        return locationDataList
    }

    // Fetch speed stats (min, max, avg) for a specific device/student within a time range
    fun getSpeedStatsForDevice(studentId: String, startDate: Long, endDate: Long): SpeedStats {
        val db: SQLiteDatabase = this.readableDatabase
        val cursor: Cursor = db.rawQuery(
            "SELECT MIN($COL_SPEED) AS minSpeed, MAX($COL_SPEED) AS maxSpeed, AVG($COL_SPEED) AS avgSpeed FROM $TABLE_NAME WHERE $COL_STUDENT_ID = ? AND $COL_TIMESTAMP BETWEEN ? AND ?",
            arrayOf(studentId, startDate.toString(), endDate.toString())
        )

        var stats = SpeedStats(0.0, 0.0, 0.0)
        if (cursor.moveToFirst()) {
            stats = SpeedStats(
                cursor.getDouble(cursor.getColumnIndexOrThrow("minSpeed")),
                cursor.getDouble(cursor.getColumnIndexOrThrow("maxSpeed")),
                cursor.getDouble(cursor.getColumnIndexOrThrow("avgSpeed"))
            )
        }
        cursor.close()
        return stats
    }

    // Fetch device's location path (list of LatLng points) for rendering on the map
    fun getDevicePath(studentId: String): List<LatLng> {
        val path = mutableListOf<LatLng>()
        val db = this.readableDatabase
        val cursor = db.rawQuery("SELECT latitude, longitude FROM $TABLE_NAME WHERE $COL_STUDENT_ID = ? ORDER BY $COL_TIMESTAMP", arrayOf(studentId))

        if (cursor.moveToFirst()) {
            do {
                val lat = cursor.getDouble(cursor.getColumnIndexOrThrow("latitude"))
                val lng = cursor.getDouble(cursor.getColumnIndexOrThrow("longitude"))
                path.add(LatLng(lat, lng))
            } while (cursor.moveToNext())
        }
        cursor.close()
        return path
    }

    // Fetch all student IDs
    @SuppressLint("Range")
    fun getAllStudentIds(): List<String> {
        val studentIds = mutableListOf<String>()
        val db = readableDatabase
        val query = "SELECT DISTINCT $COL_STUDENT_ID FROM $TABLE_NAME"
        val cursor = db.rawQuery(query, null)

        Log.d("DatabaseHelper", "Fetching all student IDs")

        if (cursor.moveToFirst()) {
            do {
                val studentId = cursor.getString(cursor.getColumnIndex(COL_STUDENT_ID))
                studentIds.add(studentId)
                Log.d("DatabaseHelper", "Found studentId: $studentId")
            } while (cursor.moveToNext())
        } else {
            Log.d("DatabaseHelper", "No student IDs found.")
        }

        cursor.close()
        db.close()
        return studentIds
    }

    // Fetch all devices (students) with min and max speed
    fun getAllDevices(): List<Device> {
        val devices = mutableListOf<Device>()
        val db = readableDatabase
        val cursor = db.rawQuery(
            "SELECT $COL_STUDENT_ID, MIN($COL_SPEED) AS minSpeed, MAX($COL_SPEED) AS maxSpeed FROM $TABLE_NAME GROUP BY $COL_STUDENT_ID",
            null
        )

        if (cursor.moveToFirst()) {
            do {
                val studentId = cursor.getString(cursor.getColumnIndexOrThrow(COL_STUDENT_ID))
                val minSpeed = cursor.getDouble(cursor.getColumnIndexOrThrow("minSpeed"))
                val maxSpeed = cursor.getDouble(cursor.getColumnIndexOrThrow("maxSpeed"))
                devices.add(Device(studentId, minSpeed, maxSpeed))
            } while (cursor.moveToNext())
        }
        cursor.close()
        return devices
    }

    // New method to fetch location data for a device (studentId) without time range
    fun getLocationDataForDevice(studentId: String): List<LocationData> {
        val db: SQLiteDatabase = this.readableDatabase
        val cursor: Cursor = db.rawQuery(
            "SELECT latitude, longitude FROM $TABLE_NAME WHERE $COL_STUDENT_ID = ? ORDER BY $COL_TIMESTAMP ASC",
            arrayOf(studentId)
        )

        val locationDataList = mutableListOf<LocationData>()
        while (cursor.moveToNext()) {
            val latitude = cursor.getDouble(cursor.getColumnIndexOrThrow("latitude"))
            val longitude = cursor.getDouble(cursor.getColumnIndexOrThrow("longitude"))
            locationDataList.add(LocationData(latitude, longitude))
        }
        cursor.close()
        return locationDataList
    }
}
