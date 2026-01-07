package com.project.mobilecafeserver.models

data class HistoryModel(
    var id: String = "", // <--- ADD THIS (to store the record ID like "-Nx123...")
    val mobileId: String = "",
    val timeDuration: String = "", // e.g. "1 Hr"
    val amount: String = "",       // e.g. "20.00"
    val timestamp: String = "",    // e.g. "Jan 03 7:00 PM"
    // New flags to simulate data presence
    val hasFaceData: Boolean = false,
    val hasLocationData: Boolean = false
)