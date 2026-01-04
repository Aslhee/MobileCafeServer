package com.project.mobilecafeserver.models

data class AppSelectionModel(
    val appName: String,
    val packageName: String,
    var isAllowed: Boolean = false
)