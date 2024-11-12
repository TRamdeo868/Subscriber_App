package com.example.subscriberapp

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

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
        val createTable = "CREATE TABLE $TABLE_NAME ($COL_ID INTEGER PRIMARY KEY AUTOINCREMENT, $COL_STUDENT_ID TEXT, $COL_SPEED REAL, $COL_TIMESTAMP INTEGER, $COL_LATITUDE REAL, $COL_LONGITUDE REAL)"
        db?.execSQL(createTable)
    }

    override fun onUpgrade(db: SQLiteDatabase?, oldVersion: Int, newVersion: Int) {
        db?.execSQL("DROP TABLE IF EXISTS $TABLE_NAME")
        onCreate(db)
    }

    fun insertLocationData(studentId: String, speed: Double, timestamp: Long, latitude: Double, longitude: Double) {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(COL_STUDENT_ID, studentId)
            put(COL_SPEED, speed)
            put(COL_TIMESTAMP, timestamp)
            put(COL_LATITUDE, latitude)
            put(COL_LONGITUDE, longitude)
        }
        db.insert(TABLE_NAME, null, values)
        db.close()
    }
}
