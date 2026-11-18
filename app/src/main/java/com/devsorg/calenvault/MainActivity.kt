package com.devsorg.calenvault

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.InputType
import android.text.TextWatcher
import android.view.HapticFeedbackConstants
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.FrameLayout
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.devsorg.calenvault.data.db.EventDatabaseHelper
import com.devsorg.calenvault.data.model.CalendarDay
import com.devsorg.calenvault.data.model.CalendarEvent
import com.devsorg.calenvault.data.model.EventCategory
import com.devsorg.calenvault.data.preferences.PreferencesManager
import com.devsorg.calenvault.data.repository.EventRepository
import com.devsorg.calenvault.databinding.ActivityMainBinding
import com.devsorg.calenvault.ui.adapter.CalendarGridAdapter
import com.devsorg.calenvault.ui.adapter.EventAdapter
import com.devsorg.calenvault.ui.calendar.CalendarUtils
import com.devsorg.calenvault.ui.dialog.AddEditEventBottomSheet
import com.devsorg.calenvault.ui.dialog.EventDetailBottomSheet
import com.devsorg.calenvault.vault.activities.HiddenActivity
import com.devsorg.calenvault.vault.activities.OnboardingActivity
import com.devsorg.calenvault.vault.utils.PrefsUtil
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import java.util.Calendar

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var repository: EventRepository
    private lateinit var calendarAdapter: CalendarGridAdapter
    private lateinit var eventAdapter: EventAdapter

    private var currentMonthCalendar: Calendar = Calendar.getInstance()
    private var selectedDate: String = CalendarUtils.getTodayString()
    private var selectedCategoryFilter: EventCategory? = null
    private var searchQuery: String = ""
    private var isSearchVisible: Boolean = false

    // Covert Vault trigger state
    private val vaultPrefs: PrefsUtil by lazy { PrefsUtil(this) }
    private var todayTapCount = 0
    private var lastTodayTapTime = 0L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (!vaultPrefs.hasSeenOnboarding()) {
            if (vaultPrefs.hasPassword()) {
                vaultPrefs.setSeenOnboarding(true)
            } else {
                startActivity(Intent(this, OnboardingActivity::class.java))
            }
        } else if (!vaultPrefs.hasPassword()) {
            startActivity(Intent(this, SetupPasswordActivity::class.java))
        }
        enableEdgeToEdge()
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupWindowInsets()
        initData()
        setupAdapters()
        setupListeners()
        loadMonthData()
    }

    override fun onResume() {
        super.onResume()
        loadMonthData()
    }

    private fun setupWindowInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.main) { v, insets ->
            val statusAndCutout = insets.getInsets(
                WindowInsetsCompat.Type.statusBars() or WindowInsetsCompat.Type.displayCutout()
            )
            val navBars = insets.getInsets(WindowInsetsCompat.Type.navigationBars())
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            val density = resources.displayMetrics.density
            val fabMarginEnd = (20 * density).toInt()
            val fabMarginBottom = navBars.bottom + (24 * density).toInt()
            (binding.fabAddEvent.layoutParams as? ViewGroup.MarginLayoutParams)?.let { params ->
                params.setMargins(0, 0, fabMarginEnd, fabMarginBottom)
                binding.fabAddEvent.layoutParams = params
            }
            insets
        }
    }

    private fun initData() {
        val dbHelper = EventDatabaseHelper.getInstance(this)
        repository = EventRepository(dbHelper)
    }

    private fun setupAdapters() {
        // 7-column calendar grid
        calendarAdapter = CalendarGridAdapter { clickedDay ->
            onDaySelected(clickedDay)
        }
        binding.rvCalendarGrid.apply {
            layoutManager = GridLayoutManager(this@MainActivity, 7)
            adapter = calendarAdapter
            itemAnimator = null // Avoid flicker during quick month switching
        }

        // Selected day events list
        eventAdapter = EventAdapter { clickedEvent ->
            showEventDetails(clickedEvent)
        }
        binding.rvEvents.apply {
            layoutManager = LinearLayoutManager(this@MainActivity)
            adapter = eventAdapter
        }
    }

    private fun setupListeners() {
        // Covert Trigger 1: Long press Calendar Title Header
        binding.tvAppTitle.setOnLongClickListener {
            if (vaultPrefs.isTriggerTitleLongPressEnabled()) {
                it.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                handleVaultTrigger()
                true
            } else {
                false
            }
        }

        // Month Navigation
        binding.btnPrevMonth.setOnClickListener {
            currentMonthCalendar.add(Calendar.MONTH, -1)
            loadMonthData()
        }

        binding.btnNextMonth.setOnClickListener {
            currentMonthCalendar.add(Calendar.MONTH, 1)
            loadMonthData()
        }

        // Covert Trigger 2: Multi-Tap (4x within 1.8s) on Today Button
        binding.btnToday.setOnClickListener {
            val now = System.currentTimeMillis()
            if (now - lastTodayTapTime > 1800) {
                todayTapCount = 1
            } else {
                todayTapCount++
            }
            lastTodayTapTime = now

            // Standard calendar behavior: Jump to Today
            currentMonthCalendar = Calendar.getInstance()
            selectedDate = CalendarUtils.getTodayString()
            loadMonthData()

            if (vaultPrefs.isTriggerTodayMultiTapEnabled() && todayTapCount >= 4) {
                todayTapCount = 0
                binding.btnToday.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                handleVaultTrigger()
            }
        }

        // Open Public Calendar Settings
        binding.btnSettings.setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }

        // Toggle Search Bar
        binding.btnToggleSearch.setOnClickListener {
            toggleSearch()
        }

        binding.btnClearSearch.setOnClickListener {
            binding.etSearch.text?.clear()
        }

        // Covert Trigger 3: Search Bar Passcode Entry
        binding.etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                searchQuery = s?.toString()?.trim().orEmpty()
                binding.btnClearSearch.visibility = if (searchQuery.isNotEmpty()) View.VISIBLE else View.GONE

                // If user types enclosed #PIN# pattern, trigger instantly
                if (vaultPrefs.isTriggerSearchEnabled() && searchQuery.startsWith("#") && searchQuery.endsWith("#") && searchQuery.length > 2) {
                    val code = searchQuery.substring(1, searchQuery.length - 1)
                    if (checkAndExecuteVaultCode(code)) {
                        return
                    }
                }
                loadEventsForSelectedDate()
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        binding.etSearch.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH || actionId == EditorInfo.IME_ACTION_DONE) {
                if (vaultPrefs.isTriggerSearchEnabled() && checkAndExecuteVaultCode(searchQuery)) {
                    true
                } else {
                    false
                }
            } else {
                false
            }
        }

        // Category Filter Chips
        binding.chipGroupFilter.setOnCheckedStateChangeListener { _, checkedIds ->
            selectedCategoryFilter = when {
                checkedIds.contains(R.id.filterWork) -> EventCategory.WORK
                checkedIds.contains(R.id.filterPersonal) -> EventCategory.PERSONAL
                checkedIds.contains(R.id.filterMeeting) -> EventCategory.MEETING
                checkedIds.contains(R.id.filterReminder) -> EventCategory.REMINDER
                checkedIds.contains(R.id.filterHealth) -> EventCategory.HEALTH
                checkedIds.contains(R.id.filterOther) -> EventCategory.OTHER
                else -> null // All
            }
            loadEventsForSelectedDate()
        }

        // Add Event Buttons
        binding.fabAddEvent.setOnClickListener {
            openAddEventSheet()
        }

        binding.btnEmptyAddEvent.setOnClickListener {
            openAddEventSheet()
        }
    }

    private fun checkAndExecuteVaultCode(rawInput: String): Boolean {
        val clean = rawInput.trim().removePrefix("#").removeSuffix("#").trim()
        if (clean.isEmpty()) return false

        if (vaultPrefs.hasPassword()) {
            if (vaultPrefs.validatePassword(clean)) {
                binding.etSearch.text?.clear()
                unlockAndOpenVault()
                return true
            }
        } else {
            // Default setup codes for first-time activation via search bar
            if (clean == "1234" || clean == "0000") {
                binding.etSearch.text?.clear()
                openVaultPasswordSetup()
                return true
            }
        }
        return false
    }

    private fun handleVaultTrigger() {
        if (!vaultPrefs.hasPassword()) {
            openVaultPasswordSetup()
        } else {
            showVaultUnlockDialog()
        }
    }

    private fun openVaultPasswordSetup() {
        val intent = Intent(this, SetupPasswordActivity::class.java)
        startActivity(intent)
    }

    private fun unlockAndOpenVault() {
        val app = application as? CalenVaultApp
        app?.isVaultSessionActive = true
        val intent = Intent(this, HiddenActivity::class.java)
        startActivity(intent)
    }

    private fun showVaultUnlockDialog() {
        val inputLayout = TextInputLayout(this).apply {
            boxBackgroundMode = TextInputLayout.BOX_BACKGROUND_OUTLINE
            hint = getString(R.string.vault_enter_pin)
            endIconMode = TextInputLayout.END_ICON_PASSWORD_TOGGLE
        }
        val editText = TextInputEditText(inputLayout.context).apply {
            inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_VARIATION_PASSWORD
            setSingleLine()
        }
        inputLayout.addView(editText)

        val container = FrameLayout(this).apply {
            val padH = (24 * resources.displayMetrics.density).toInt()
            val padV = (12 * resources.displayMetrics.density).toInt()
            setPadding(padH, padV, padH, 0)
            addView(inputLayout)
        }

        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.vault_enter_pin)
            .setView(container)
            .setPositiveButton(R.string.verify) { _, _ ->
                val pin = editText.text?.toString()?.trim().orEmpty()
                if (vaultPrefs.validatePassword(pin)) {
                    unlockAndOpenVault()
                } else {
                    Toast.makeText(this, R.string.wrong_password_entered, Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun onDaySelected(day: CalendarDay) {
        selectedDate = day.dateString

        // If clicked day belongs to next/prev month, update current month view
        if (!day.isCurrentMonth) {
            val parts = day.dateString.split("-")
            if (parts.size == 3) {
                currentMonthCalendar.set(Calendar.YEAR, parts[0].toInt())
                currentMonthCalendar.set(Calendar.MONTH, parts[1].toInt() - 1)
                currentMonthCalendar.set(Calendar.DAY_OF_MONTH, parts[2].toInt())
            }
        }

        loadMonthData()
    }

    private fun loadMonthData() {
        // Update Month/Year Header
        binding.tvMonthYear.text = CalendarUtils.getMonthYearTitle(currentMonthCalendar)

        val prefs = PreferencesManager.getInstance(this)
        val firstDay = prefs.firstDayOfWeek
        calendarAdapter.highlightWeekends = prefs.highlightWeekends
        calendarAdapter.showEventDots = prefs.showEventDots
        eventAdapter.timeFormatPref = prefs.timeFormat

        // Update Day of Week Headers
        setupDayHeaders(firstDay, prefs.highlightWeekends)

        // Generate 42 days (6 weeks) for grid
        val monthPrefix = CalendarUtils.getYearMonthPrefix(currentMonthCalendar)
        val monthEvents = repository.getEventsForMonth(monthPrefix).groupBy { it.date }
        val days = CalendarUtils.generateDaysForMonth(currentMonthCalendar, selectedDate, monthEvents, firstDay)
        calendarAdapter.submitList(days)

        // Load events for selected date
        loadEventsForSelectedDate()
    }

    private fun setupDayHeaders(firstDay: Int, highlightWeekends: Boolean) {
        val colorWeekend = ContextCompat.getColor(this, R.color.calendar_day_weekend)
        val colorOutline = com.google.android.material.color.MaterialColors.getColor(
            binding.root,
            com.google.android.material.R.attr.colorOutline
        )

        if (firstDay == Calendar.MONDAY) {
            binding.tvDayHeader1.text = getString(R.string.mon)
            binding.tvDayHeader1.setTextColor(colorOutline)
            binding.tvDayHeader2.text = getString(R.string.tue)
            binding.tvDayHeader2.setTextColor(colorOutline)
            binding.tvDayHeader3.text = getString(R.string.wed)
            binding.tvDayHeader3.setTextColor(colorOutline)
            binding.tvDayHeader4.text = getString(R.string.thu)
            binding.tvDayHeader4.setTextColor(colorOutline)
            binding.tvDayHeader5.text = getString(R.string.fri)
            binding.tvDayHeader5.setTextColor(colorOutline)
            binding.tvDayHeader6.text = getString(R.string.sat)
            binding.tvDayHeader6.setTextColor(if (highlightWeekends) colorWeekend else colorOutline)
            binding.tvDayHeader7.text = getString(R.string.sun)
            binding.tvDayHeader7.setTextColor(colorWeekend)
        } else {
            binding.tvDayHeader1.text = getString(R.string.sun)
            binding.tvDayHeader1.setTextColor(colorWeekend)
            binding.tvDayHeader2.text = getString(R.string.mon)
            binding.tvDayHeader2.setTextColor(colorOutline)
            binding.tvDayHeader3.text = getString(R.string.tue)
            binding.tvDayHeader3.setTextColor(colorOutline)
            binding.tvDayHeader4.text = getString(R.string.wed)
            binding.tvDayHeader4.setTextColor(colorOutline)
            binding.tvDayHeader5.text = getString(R.string.thu)
            binding.tvDayHeader5.setTextColor(colorOutline)
            binding.tvDayHeader6.text = getString(R.string.fri)
            binding.tvDayHeader6.setTextColor(colorOutline)
            binding.tvDayHeader7.text = getString(R.string.sat)
            binding.tvDayHeader7.setTextColor(if (highlightWeekends) colorWeekend else colorOutline)
        }
    }

    private fun loadEventsForSelectedDate() {
        val events = if (searchQuery.isNotEmpty() || selectedCategoryFilter != null) {
            // Search across events
            val searchResults = repository.searchEvents(searchQuery, selectedCategoryFilter)
            if (searchQuery.isNotEmpty()) {
                // If actively searching with keyword, show search results
                searchResults
            } else {
                // Filtered by category for selected date
                searchResults.filter { it.date == selectedDate }
            }
        } else {
            repository.getEventsForDate(selectedDate)
        }

        eventAdapter.submitList(events)

        // Event counter
        val count = events.size
        binding.tvEventCount.text = when (count) {
            0 -> getString(R.string.no_events_count)
            1 -> getString(R.string.single_event_count)
            else -> getString(R.string.events_count, count)
        }

        // Empty state
        if (events.isEmpty()) {
            binding.layoutEmptyState.visibility = View.VISIBLE
            binding.rvEvents.visibility = View.GONE
        } else {
            binding.layoutEmptyState.visibility = View.GONE
            binding.rvEvents.visibility = View.VISIBLE
        }
    }

    private fun toggleSearch() {
        isSearchVisible = !isSearchVisible
        binding.cardSearch.visibility = if (isSearchVisible) View.VISIBLE else View.GONE
        if (isSearchVisible) {
            binding.etSearch.requestFocus()
        } else {
            binding.etSearch.text?.clear()
            searchQuery = ""
            loadEventsForSelectedDate()
        }
    }

    private fun openAddEventSheet(eventToEdit: CalendarEvent? = null) {
        val sheet = AddEditEventBottomSheet.newInstance(eventToEdit, selectedDate)
        sheet.onEventSavedListener = { savedEvent ->
            repository.saveEvent(savedEvent)
            selectedDate = savedEvent.date

            // Jump to the saved event's month
            val parts = savedEvent.date.split("-")
            if (parts.size == 3) {
                currentMonthCalendar.set(Calendar.YEAR, parts[0].toInt())
                currentMonthCalendar.set(Calendar.MONTH, parts[1].toInt() - 1)
            }

            loadMonthData()
            Snackbar.make(binding.main, R.string.event_saved, Snackbar.LENGTH_SHORT).show()
        }
        sheet.show(supportFragmentManager, "ADD_EDIT_EVENT")
    }

    private fun showEventDetails(event: CalendarEvent) {
        val sheet = EventDetailBottomSheet.newInstance(event)
        sheet.onEditClickListener = { eventToEdit ->
            openAddEventSheet(eventToEdit)
        }
        sheet.onDeleteClickListener = { eventToDelete ->
            repository.deleteEvent(eventToDelete.id)
            loadMonthData()
            Snackbar.make(binding.main, R.string.event_deleted, Snackbar.LENGTH_SHORT).show()
        }
        sheet.show(supportFragmentManager, "EVENT_DETAIL")
    }
}
