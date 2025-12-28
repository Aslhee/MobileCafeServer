package com.project.mobilecafeserver

/**
 * Data Class representing a Mobile Unit.
 * Default values (like = "") are required for Firebase to work correctly.
 */
data class DeviceModel(
    var deviceId: String = "",       // Unique ID (e.g., "mobile_01")
    var name: String = "",           // Display Name (e.g., "Unit 1")
    var status: String = "LOCKED",   // Current State: "LOCKED" or "ACTIVE"
    var endTime: Long = 0,            // Timestamp when time expires (in milliseconds)
    var savedTime: Long = 0 // <--- ADD THIS NEW FIELD (Time remaining in ms)
)