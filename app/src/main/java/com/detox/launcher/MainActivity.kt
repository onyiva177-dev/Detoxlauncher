package com.detox.launcher

import android.app.role.RoleManager
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import android.widget.ListView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import kotlin.math.abs

class MainActivity : AppCompatActivity() {

    private lateinit var prefs: PrefsManager
    private lateinit var usageTracker: UsageTracker
    private lateinit var listView: ListView
    private lateinit var titleView: TextView
    private lateinit var adapter: AppListAdapter
    private lateinit var gestureDetector: GestureDetector

    // Whether we're currently showing "all apps" (after swipe-left) or just pinned favorites
    private var showingAllApps = false

    private val SWIPE_THRESHOLD = 100
    private val SWIPE_VELOCITY_THRESHOLD = 100

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        prefs = PrefsManager(this)
        usageTracker = UsageTracker(this)

        listView = findViewById(R.id.app_list)
        titleView = findViewById(R.id.screen_title)

        adapter = AppListAdapter(
            layoutInflater,
            emptyList(),
            onClick = { app -> launchApp(app) },
            onLongPress = { app -> togglePin(app) }
        )
        listView.adapter = adapter

        setupGestures()
        requestOnboardingIfNeeded()
        refreshList()
    }

    override fun onResume() {
        super.onResume()
        // Coming back to the launcher (user pressed Home, or closed an app) — refresh
        // so pin state and today's usage minutes are current.
        refreshList()
    }

    // ---------- Onboarding: default launcher role + usage access ----------

    private fun requestOnboardingIfNeeded() {
        // 1. Ask to become the default Home app (Android 10+/API 29+ has a clean Role API;
        //    on older versions the system shows its own "always/just once" chooser the first
        //    time the user presses Home, so there's nothing extra to trigger there).
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val roleManager = getSystemService(RoleManager::class.java)
            if (roleManager != null &&
                roleManager.isRoleAvailable(RoleManager.ROLE_HOME) &&
                !roleManager.isRoleHeld(RoleManager.ROLE_HOME)
            ) {
                val intent = roleManager.createRequestRoleIntent(RoleManager.ROLE_HOME)
                startActivity(intent)
            }
        }

        // 2. Ask for Usage Access so we can show per-app screen time.
        if (!usageTracker.hasPermission()) {
            Toast.makeText(
                this,
                "Please grant \"Usage Access\" so Detox Launcher can show screen time",
                Toast.LENGTH_LONG
            ).show()
            startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
        }
    }

    // ---------- Building the list ----------

    private fun refreshList() {
        val pm = packageManager
        val launcherIntent = Intent(Intent.ACTION_MAIN, null).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }
        val resolved = pm.queryIntentActivities(launcherIntent, PackageManager.MATCH_ALL)

        val usage = if (usageTracker.hasPermission()) usageTracker.getTodayUsageMinutes() else emptyMap()
        val pinned = prefs.getPinned()

        val allApps = resolved
            .map { info ->
                val pkg = info.activityInfo.packageName
                AppInfo(
                    label = info.loadLabel(pm).toString(),
                    packageName = pkg,
                    isPinned = pinned.contains(pkg),
                    usageMinutesToday = usage[pkg] ?: 0L
                )
            }
            .distinctBy { it.packageName }
            .sortedBy { it.label.lowercase() }

        val toShow = if (showingAllApps) {
            allApps
        } else {
            val pinnedApps = allApps.filter { it.isPinned }
            pinnedApps.ifEmpty {
                // Nothing pinned yet — show all apps so the phone is still usable,
                // and nudge the user to long-press to pin favorites.
                allApps
            }
        }

        titleView.text = if (showingAllApps) "All Apps" else "Favorites"
        adapter.updateItems(toShow)
    }

    private fun launchApp(app: AppInfo) {
        val intent = packageManager.getLaunchIntentForPackage(app.packageName)
        if (intent != null) {
            startActivity(intent)
        } else {
            Toast.makeText(this, "Can't open ${app.label}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun togglePin(app: AppInfo) {
        prefs.togglePin(app.packageName)
        val nowPinned = prefs.isPinned(app.packageName)
        Toast.makeText(
            this,
            if (nowPinned) "Pinned ${app.label}" else "Unpinned ${app.label}",
            Toast.LENGTH_SHORT
        ).show()
        refreshList()
    }

    // ---------- Gestures ----------
    // Swipe LEFT  -> reveal full app drawer (all installed apps)
    // Swipe RIGHT -> Google News
    // Swipe UP    -> Chrome web search
    // Swipe DOWN (from all-apps view) -> back to Favorites

    private fun setupGestures() {
        gestureDetector = GestureDetector(this, object : GestureDetector.SimpleOnGestureListener() {
            override fun onFling(
                e1: MotionEvent?,
                e2: MotionEvent,
                velocityX: Float,
                velocityY: Float
            ): Boolean {
                if (e1 == null) return false
                val diffX = e2.x - e1.x
                val diffY = e2.y - e1.y

                if (abs(diffX) > abs(diffY)) {
                    // Horizontal swipe
                    if (abs(diffX) > SWIPE_THRESHOLD && abs(velocityX) > SWIPE_VELOCITY_THRESHOLD) {
                        if (diffX < 0) {
                            onSwipeLeft()
                        } else {
                            onSwipeRight()
                        }
                        return true
                    }
                } else {
                    // Vertical swipe
                    if (abs(diffY) > SWIPE_THRESHOLD && abs(velocityY) > SWIPE_VELOCITY_THRESHOLD) {
                        if (diffY < 0) {
                            onSwipeUp()
                        } else {
                            onSwipeDown()
                        }
                        return true
                    }
                }
                return false
            }
        })

        val root = findViewById<View>(R.id.root_layout)
        root.setOnTouchListener { _, event -> gestureDetector.onTouchEvent(event) }
        // Also attach to the list so swipes register even when starting on a row
        listView.setOnTouchListener { _, event -> gestureDetector.onTouchEvent(event) }
    }

    private fun onSwipeLeft() {
        showingAllApps = true
        refreshList()
    }

    private fun onSwipeDown() {
        if (showingAllApps) {
            showingAllApps = false
            refreshList()
        }
    }

    private fun onSwipeUp() {
        openChromeSearch()
    }

    private fun onSwipeRight() {
        openGoogleNews()
    }

    private fun openChromeSearch() {
        val uri = Uri.parse("https://www.google.com/search?q=")
        val intent = Intent(Intent.ACTION_VIEW, uri)
        intent.setPackage("com.android.chrome")
        try {
            startActivity(intent)
        } catch (e: Exception) {
            // Chrome not installed — fall back to whatever the default browser is
            startActivity(Intent(Intent.ACTION_VIEW, uri))
        }
    }

    private fun openGoogleNews() {
        val uri = Uri.parse("https://news.google.com")
        val intent = Intent(Intent.ACTION_VIEW, uri)
        intent.setPackage("com.android.chrome")
        try {
            startActivity(intent)
        } catch (e: Exception) {
            startActivity(Intent(Intent.ACTION_VIEW, uri))
        }
    }

    // Intercept the physical/gesture Back action so leaving "All Apps" goes back
    // to Favorites instead of doing nothing (launchers can't really be "backed out of").
    override fun onBackPressed() {
        if (showingAllApps) {
            showingAllApps = false
            refreshList()
        }
        // Deliberately do NOT call super.onBackPressed() — this is Home, there's nowhere to go
    }
}
