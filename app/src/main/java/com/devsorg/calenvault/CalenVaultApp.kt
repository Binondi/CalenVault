package com.devsorg.calenvault

import android.app.Activity
import android.app.Application
import android.os.Bundle
import com.devsorg.calenvault.data.preferences.PreferencesManager
import com.google.android.material.color.DynamicColors
import com.google.android.material.color.DynamicColorsOptions

class CalenVaultApp : Application(), Application.ActivityLifecycleCallbacks {

    private var activityCount = 0

    var isVaultSessionActive = false
    var isWaitingForResult = false

    override fun onCreate() {
        super.onCreate()
        registerActivityLifecycleCallbacks(this)

        val prefs = PreferencesManager.getInstance(this)
        // Apply saved theme mode (System, Light, Dark)
        prefs.applyTheme()

        // Apply dynamic colors if enabled by user
        if (prefs.isDynamicColorsSupported()) {
            val options = DynamicColorsOptions.Builder()
                .setPrecondition { _, _ -> prefs.isDynamicColorsEnabled }
                .build()
            DynamicColors.applyToActivitiesIfAvailable(this, options)
        }
    }

    override fun onActivityStarted(activity: Activity) {
        activityCount++
        isWaitingForResult = false
    }

    override fun onActivityStopped(activity: Activity) {
        activityCount--

        if (activityCount <= 0 && !isWaitingForResult && !activity.isChangingConfigurations) {
            isVaultSessionActive = false
        }
    }

    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {}
    override fun onActivityResumed(activity: Activity) {}
    override fun onActivityPaused(activity: Activity) {}
    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}
    override fun onActivityDestroyed(activity: Activity) {}
}
