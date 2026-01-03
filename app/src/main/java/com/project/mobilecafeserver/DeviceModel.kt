package com.project.mobilecafeserver

import com.google.firebase.database.Exclude

/**
 * Data Class representing a Mobile Unit.
 * Default values (like = "") are required for Firebase to work correctly.
 */
data class DeviceModel(
    var deviceId: String = "",       // Unique ID (e.g., "mobile_01")
    var name: String = "",           // Display Name (e.g., "Unit 1")
    var status: String = "LOCKED",   // Current State: "LOCKED" or "ACTIVE"
    var endTime: Long = 0,            // Timestamp when time expires (in milliseconds)
    var savedTime: Long = 0,// <--- ADD THIS NEW FIELD (Time remaining in ms)
    var batteryLevel: Int = 0, // <--- ADD THIS NEW FIELD (Default 0)

    // This exists only in the app memory, NOT in Firebase
    @get:Exclude
    var isSelected: Boolean = false
)