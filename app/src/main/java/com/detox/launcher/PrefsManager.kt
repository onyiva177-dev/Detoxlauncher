package com.detox.launcher

import android.content.Context

/**
 * Tiny wrapper around SharedPreferences for storing which apps
 * the user has pinned to the minimal home screen.
 */
class PrefsManager(context: Context) {

    private val prefs = context.getSharedPreferences("detox_launcher_prefs", Context.MODE_PRIVATE)
    private val PINNED_KEY = "pinned_packages"

    fun getPinned(): MutableSet<String> {
        return HashSet(prefs.getStringSet(PINNED_KEY, emptySet()) ?: emptySet())
    }

    fun togglePin(packageName: String) {
        val current = getPinned()
        if (current.contains(packageName)) {
            current.remove(packageName)
        } else {
            current.add(packageName)
        }
        prefs.edit().putStringSet(PINNED_KEY, current).apply()
    }

    fun isPinned(packageName: String): Boolean = getPinned().contains(packageName)
}
