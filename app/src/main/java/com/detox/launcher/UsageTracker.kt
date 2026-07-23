package com.detox.launcher

import android.app.AppOpsManager
import android.app.usage.UsageStatsManager
import android.content.Context
import android.os.Process
import java.util.Calendar

/**
 * Reads Android's built-in UsageStatsManager to report how many minutes
 * each app has been used today. Requires the user to grant "Usage Access"
 * in system settings (see MainActivity.requestUsageAccessIfNeeded).
 */
class UsageTracker(private val context: Context) {

    fun hasPermission(): Boolean {
        val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        val mode = appOps.unsafeCheckOpNoThrow(
            AppOpsManager.OPSTR_GET_USAGE_STATS,
            Process.myUid(),
            context.packageName
        )
        return mode == AppOpsManager.MODE_ALLOWED
    }

    /** Returns a map of packageName -> minutes used since midnight today. */
    fun getTodayUsageMinutes(): Map<String, Long> {
        val usm = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        val cal = Calendar.getInstance()
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        val startTime = cal.timeInMillis
        val endTime = System.currentTimeMillis()

        val stats = usm.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, startTime, endTime)
        val result = HashMap<String, Long>()
        stats?.forEach { stat ->
            val minutes = stat.totalTimeInForeground / 1000 / 60
            if (minutes > 0) {
                result[stat.packageName] = (result[stat.packageName] ?: 0L) + minutes
            }
        }
        return result
    }
}
