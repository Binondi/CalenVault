package com.devsorg.calenvault

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.devsorg.calenvault.data.db.EventDatabaseHelper
import com.devsorg.calenvault.data.model.EventCategory
import com.devsorg.calenvault.data.preferences.PreferencesManager
import com.devsorg.calenvault.data.preferences.ThemeMode
import com.devsorg.calenvault.data.preferences.TimeFormatPref
import com.devsorg.calenvault.data.repository.EventRepository
import com.devsorg.calenvault.databinding.ActivitySettingsBinding
import com.devsorg.calenvault.vault.activities.AboutActivity
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import java.util.Calendar

class SettingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySettingsBinding
    private lateinit var prefs: PreferencesManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        prefs = PreferencesManager.getInstance(this)

        setupWindowInsets()
        setupToolbar()
        setupAppearanceSettings()
        setupCalendarSettings()
        setupPrivacySettings()
        setupAboutSection()
    }

    private fun setupWindowInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.settingsRoot) { _, insets ->
            val statusAndCutout = insets.getInsets(
                WindowInsetsCompat.Type.statusBars() or WindowInsetsCompat.Type.displayCutout()
            )
            val navBars = insets.getInsets(WindowInsetsCompat.Type.navigationBars())

            binding.appBarSettings.setPadding(
                0,
                statusAndCutout.top,
                0,
                0
            )

            binding.scrollSettings.setPadding(
                0,
                0,
                0,
                navBars.bottom + (32 * resources.displayMetrics.density).toInt()
            )

            insets
        }
    }

    private fun setupToolbar() {
        binding.toolbarSettings.setNavigationOnClickListener {
            finish()
        }
    }

    private fun setupAppearanceSettings() {
        // Theme selection
        updateThemeSummary()
        binding.rowTheme.setOnClickListener {
            showThemeSelectionDialog()
        }

        // Dynamic colors
        val isSupported = prefs.isDynamicColorsSupported()
        if (isSupported) {
            binding.switchDynamicColors.isChecked = prefs.isDynamicColorsEnabled
            binding.switchDynamicColors.setOnCheckedChangeListener { _, isChecked ->
                prefs.isDynamicColorsEnabled = isChecked
                recreate()
            }
        } else {
            binding.switchDynamicColors.isEnabled = false
            binding.switchDynamicColors.isChecked = false
            binding.tvDynamicColorsDesc.text = getString(R.string.dynamic_colors_unsupported)
        }
    }

    private fun updateThemeSummary() {
        val summary = when (prefs.themeMode) {
            ThemeMode.SYSTEM -> getString(R.string.theme_system)
            ThemeMode.LIGHT -> getString(R.string.theme_light)
            ThemeMode.DARK -> getString(R.string.theme_dark)
        }
        binding.tvThemeSummary.text = summary
    }

    private fun showThemeSelectionDialog() {
        val themes = arrayOf(
            getString(R.string.theme_system),
            getString(R.string.theme_light),
            getString(R.string.theme_dark)
        )
        val currentIndex = when (prefs.themeMode) {
            ThemeMode.SYSTEM -> 0
            ThemeMode.LIGHT -> 1
            ThemeMode.DARK -> 2
        }

        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.choose_theme)
            .setSingleChoiceItems(themes, currentIndex) { dialog, which ->
                val selectedMode = when (which) {
                    0 -> ThemeMode.SYSTEM
                    1 -> ThemeMode.LIGHT
                    else -> ThemeMode.DARK
                }
                prefs.themeMode = selectedMode
                updateThemeSummary()
                dialog.dismiss()
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun setupCalendarSettings() {
        // First Day of Week
        updateFirstDaySummary()
        binding.rowFirstDay.setOnClickListener {
            showFirstDayDialog()
        }

        // Time Format
        updateTimeFormatSummary()
        binding.rowTimeFormat.setOnClickListener {
            showTimeFormatDialog()
        }

        // Default Duration
        updateDefaultDurationSummary()
        binding.rowDefaultDuration.setOnClickListener {
            showDefaultDurationDialog()
        }

        // Default Category
        updateDefaultCategorySummary()
        binding.rowDefaultCategory.setOnClickListener {
            showDefaultCategoryDialog()
        }

        // Highlight Weekends Switch
        binding.switchHighlightWeekends.isChecked = prefs.highlightWeekends
        binding.switchHighlightWeekends.setOnCheckedChangeListener { _, isChecked ->
            prefs.highlightWeekends = isChecked
        }

        // Show Event Indicators (Dots) Switch
        binding.switchShowDots.isChecked = prefs.showEventDots
        binding.switchShowDots.setOnCheckedChangeListener { _, isChecked ->
            prefs.showEventDots = isChecked
        }
    }

    private fun updateFirstDaySummary() {
        val summary = if (prefs.firstDayOfWeek == Calendar.MONDAY) {
            getString(R.string.monday)
        } else {
            getString(R.string.sunday)
        }
        binding.tvFirstDaySummary.text = summary
    }

    private fun showFirstDayDialog() {
        val days = arrayOf(
            getString(R.string.sunday),
            getString(R.string.monday)
        )
        val currentIndex = if (prefs.firstDayOfWeek == Calendar.MONDAY) 1 else 0

        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.choose_first_day)
            .setSingleChoiceItems(days, currentIndex) { dialog, which ->
                prefs.firstDayOfWeek = if (which == 1) Calendar.MONDAY else Calendar.SUNDAY
                updateFirstDaySummary()
                dialog.dismiss()
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun updateTimeFormatSummary() {
        val summary = when (prefs.timeFormat) {
            TimeFormatPref.SYSTEM -> getString(R.string.time_format_system)
            TimeFormatPref.FORMAT_12H -> getString(R.string.time_format_12h)
            TimeFormatPref.FORMAT_24H -> getString(R.string.time_format_24h)
        }
        binding.tvTimeFormatSummary.text = summary
    }

    private fun showTimeFormatDialog() {
        val formats = arrayOf(
            getString(R.string.time_format_system),
            getString(R.string.time_format_12h),
            getString(R.string.time_format_24h)
        )
        val currentIndex = when (prefs.timeFormat) {
            TimeFormatPref.SYSTEM -> 0
            TimeFormatPref.FORMAT_12H -> 1
            TimeFormatPref.FORMAT_24H -> 2
        }

        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.choose_time_format)
            .setSingleChoiceItems(formats, currentIndex) { dialog, which ->
                val selected = when (which) {
                    1 -> TimeFormatPref.FORMAT_12H
                    2 -> TimeFormatPref.FORMAT_24H
                    else -> TimeFormatPref.SYSTEM
                }
                prefs.timeFormat = selected
                updateTimeFormatSummary()
                dialog.dismiss()
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun updateDefaultDurationSummary() {
        val summary = when (prefs.defaultDurationMinutes) {
            15 -> getString(R.string.duration_15_min)
            30 -> getString(R.string.duration_30_min)
            45 -> getString(R.string.duration_45_min)
            60 -> getString(R.string.duration_60_min)
            90 -> getString(R.string.duration_90_min)
            120 -> getString(R.string.duration_120_min)
            else -> "${prefs.defaultDurationMinutes} min"
        }
        binding.tvDefaultDurationSummary.text = summary
    }

    private fun showDefaultDurationDialog() {
        val durationLabels = arrayOf(
            getString(R.string.duration_15_min),
            getString(R.string.duration_30_min),
            getString(R.string.duration_45_min),
            getString(R.string.duration_60_min),
            getString(R.string.duration_90_min),
            getString(R.string.duration_120_min)
        )
        val durationValues = intArrayOf(15, 30, 45, 60, 90, 120)
        val currentIndex = durationValues.indexOf(prefs.defaultDurationMinutes).let { if (it >= 0) it else 3 }

        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.choose_duration)
            .setSingleChoiceItems(durationLabels, currentIndex) { dialog, which ->
                prefs.defaultDurationMinutes = durationValues[which]
                updateDefaultDurationSummary()
                dialog.dismiss()
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun updateDefaultCategorySummary() {
        val category = prefs.defaultCategory
        binding.tvDefaultCategorySummary.text = getString(category.displayNameRes)
    }

    private fun showDefaultCategoryDialog() {
        val categories = EventCategory.entries
        val labels = categories.map { getString(it.displayNameRes) }.toTypedArray()
        val currentIndex = categories.indexOf(prefs.defaultCategory).let { if (it >= 0) it else 0 }

        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.choose_default_category)
            .setSingleChoiceItems(labels, currentIndex) { dialog, which ->
                prefs.defaultCategory = categories[which]
                updateDefaultCategorySummary()
                dialog.dismiss()
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun setupPrivacySettings() {
        binding.rowClearData.setOnClickListener {
            MaterialAlertDialogBuilder(this)
                .setTitle(R.string.clear_data_title)
                .setMessage(R.string.clear_data_confirm)
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.delete) { _, _ ->
                    val repo = EventRepository(EventDatabaseHelper.getInstance(this))
                    repo.clearAllEvents()
                    Toast.makeText(this, R.string.all_events_cleared, Toast.LENGTH_SHORT).show()
                }
                .show()
        }
    }

    private fun setupAboutSection() {
        binding.rowAbout.setOnClickListener {
            startActivity(Intent(this, AboutActivity::class.java))
        }
    }
}
