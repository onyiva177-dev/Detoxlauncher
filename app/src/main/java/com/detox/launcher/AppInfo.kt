package com.detox.launcher

data class AppInfo(
    val label: String,
    val packageName: String,
    var isPinned: Boolean = false,
    var usageMinutesToday: Long = 0L
)
