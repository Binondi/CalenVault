package com.devsorg.calenvault.ui.dialog

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import com.devsorg.calenvault.R
import com.devsorg.calenvault.data.model.CalendarEvent
import com.devsorg.calenvault.data.model.EventCategory
import com.devsorg.calenvault.data.preferences.PreferencesManager
import com.devsorg.calenvault.data.preferences.TimeFormatPref
import com.devsorg.calenvault.databinding.DialogAddEditEventBinding
import com.devsorg.calenvault.ui.calendar.CalendarUtils
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.timepicker.MaterialTimePicker
import com.google.android.material.timepicker.TimeFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

class AddEditEventBottomSheet : BottomSheetDialogFragment() {

    private var _binding: DialogAddEditEventBinding? = null
    private val binding get() = _binding!!

    private var existingEvent: CalendarEvent? = null
    private var selectedDate: String = CalendarUtils.getTodayString()
    private var startTime: String = "09:00"
    private var endTime: String = "10:00"
    private var selectedCategory: EventCategory = EventCategory.WORK

    var onEventSavedListener: ((CalendarEvent) -> Unit)? = null

    companion object {
        private const val ARG_EVENT = "arg_event"
        private const val ARG_INITIAL_DATE = "arg_initial_date"

        fun newInstance(event: CalendarEvent? = null, initialDate: String? = null): AddEditEventBottomSheet {
            return AddEditEventBottomSheet().apply {
                arguments = Bundle().apply {
                    putSerializable(ARG_EVENT, event)
                    putString(ARG_INITIAL_DATE, initialDate)
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        @Suppress("DEPRECATION")
        existingEvent = arguments?.getSerializable(ARG_EVENT) as? CalendarEvent
        val initDate = arguments?.getString(ARG_INITIAL_DATE)
        if (initDate != null) {
            selectedDate = initDate
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DialogAddEditEventBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupInitialData()
        setupListeners()
    }

    private fun setupInitialData() {
        val prefs = PreferencesManager.getInstance(requireContext())
        val timeFormat = prefs.timeFormat

        val event = existingEvent
        if (event != null) {
            binding.tvSheetTitle.text = getString(R.string.edit_event)
            binding.etTitle.setText(event.title)
            binding.etDescription.setText(event.description)
            binding.etLocation.setText(event.location)
            selectedDate = event.date
            startTime = event.startTime
            endTime = event.endTime
            selectedCategory = event.category
            binding.switchAllDay.isChecked = event.isAllDay
        } else {
            binding.tvSheetTitle.text = getString(R.string.add_event)
            selectedCategory = prefs.defaultCategory
            val defaultDuration = prefs.defaultDurationMinutes
            val startMinutes = 9 * 60
            val endMinutes = startMinutes + defaultDuration
            val endHour = (endMinutes / 60) % 24
            val endMin = endMinutes % 60
            startTime = "09:00"
            endTime = String.format(Locale.US, "%02d:%02d", endHour, endMin)
        }

        binding.tvSelectedDate.text = CalendarUtils.getDisplayDate(selectedDate)
        binding.tvStartTime.text = CalendarUtils.formatDisplayTime(startTime, timeFormat, requireContext())
        binding.tvEndTime.text = CalendarUtils.formatDisplayTime(endTime, timeFormat, requireContext())

        // Select proper category chip
        when (selectedCategory) {
            EventCategory.WORK -> binding.chipWork.isChecked = true
            EventCategory.PERSONAL -> binding.chipPersonal.isChecked = true
            EventCategory.MEETING -> binding.chipMeeting.isChecked = true
            EventCategory.REMINDER -> binding.chipReminder.isChecked = true
            EventCategory.HEALTH -> binding.chipHealth.isChecked = true
            EventCategory.OTHER -> binding.chipOther.isChecked = true
        }

        updateTimeVisibility(binding.switchAllDay.isChecked)
    }

    private fun setupListeners() {
        binding.btnCloseSheet.setOnClickListener {
            dismiss()
        }

        binding.chipGroupCategory.setOnCheckedStateChangeListener { _, checkedIds ->
            selectedCategory = when {
                checkedIds.contains(R.id.chipWork) -> EventCategory.WORK
                checkedIds.contains(R.id.chipPersonal) -> EventCategory.PERSONAL
                checkedIds.contains(R.id.chipMeeting) -> EventCategory.MEETING
                checkedIds.contains(R.id.chipReminder) -> EventCategory.REMINDER
                checkedIds.contains(R.id.chipHealth) -> EventCategory.HEALTH
                else -> EventCategory.OTHER
            }
        }

        binding.switchAllDay.setOnCheckedChangeListener { _, isChecked ->
            updateTimeVisibility(isChecked)
        }

        binding.cardDatePicker.setOnClickListener {
            showDatePicker()
        }

        binding.cardStartTime.setOnClickListener {
            showTimePicker(true)
        }

        binding.cardEndTime.setOnClickListener {
            showTimePicker(false)
        }

        binding.btnSaveEvent.setOnClickListener {
            saveEvent()
        }
    }

    private fun updateTimeVisibility(isAllDay: Boolean) {
        binding.layoutTimePickers.visibility = if (isAllDay) View.GONE else View.VISIBLE
    }

    private fun showDatePicker() {
        val picker = MaterialDatePicker.Builder.datePicker()
            .setTitleText(R.string.select_date)
            .build()

        picker.addOnPositiveButtonClickListener { selectionMillis ->
            val utcCalendar = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
                timeInMillis = selectionMillis
            }
            val localCalendar = Calendar.getInstance().apply {
                set(Calendar.YEAR, utcCalendar.get(Calendar.YEAR))
                set(Calendar.MONTH, utcCalendar.get(Calendar.MONTH))
                set(Calendar.DAY_OF_MONTH, utcCalendar.get(Calendar.DAY_OF_MONTH))
            }
            selectedDate = CalendarUtils.formatDate(localCalendar)
            binding.tvSelectedDate.text = CalendarUtils.getDisplayDate(selectedDate)
        }

        picker.show(childFragmentManager, "DATE_PICKER")
    }

    private fun showTimePicker(isStartTime: Boolean) {
        val prefs = PreferencesManager.getInstance(requireContext())
        val timeFormat = prefs.timeFormat

        val current = if (isStartTime) startTime else endTime
        val parts = current.split(":")
        val hour = parts.getOrNull(0)?.toIntOrNull() ?: 9
        val minute = parts.getOrNull(1)?.toIntOrNull() ?: 0

        val clockFormat = when (timeFormat) {
            TimeFormatPref.FORMAT_24H -> TimeFormat.CLOCK_24H
            TimeFormatPref.FORMAT_12H -> TimeFormat.CLOCK_12H
            TimeFormatPref.SYSTEM -> {
                if (android.text.format.DateFormat.is24HourFormat(requireContext())) {
                    TimeFormat.CLOCK_24H
                } else {
                    TimeFormat.CLOCK_12H
                }
            }
        }

        val timePicker = MaterialTimePicker.Builder()
            .setTimeFormat(clockFormat)
            .setHour(hour)
            .setMinute(minute)
            .setTitleText(if (isStartTime) R.string.start_time else R.string.end_time)
            .build()

        timePicker.addOnPositiveButtonClickListener {
            val formattedTime = String.format(Locale.US, "%02d:%02d", timePicker.hour, timePicker.minute)
            if (isStartTime) {
                startTime = formattedTime
                binding.tvStartTime.text = CalendarUtils.formatDisplayTime(startTime, timeFormat, requireContext())
            } else {
                endTime = formattedTime
                binding.tvEndTime.text = CalendarUtils.formatDisplayTime(endTime, timeFormat, requireContext())
            }
        }

        timePicker.show(childFragmentManager, "TIME_PICKER")
    }

    private fun saveEvent() {
        val title = binding.etTitle.text?.toString()?.trim().orEmpty()
        if (title.isBlank()) {
            binding.tilTitle.error = getString(R.string.title_required)
            binding.etTitle.requestFocus()
            return
        }
        binding.tilTitle.error = null

        val description = binding.etDescription.text?.toString()?.trim().orEmpty()
        val location = binding.etLocation.text?.toString()?.trim().orEmpty()
        val isAllDay = binding.switchAllDay.isChecked

        val eventToSave = CalendarEvent(
            id = existingEvent?.id ?: 0L,
            title = title,
            description = description,
            location = location,
            date = selectedDate,
            startTime = startTime,
            endTime = endTime,
            isAllDay = isAllDay,
            category = selectedCategory,
            isCompleted = existingEvent?.isCompleted ?: false,
            createdAt = existingEvent?.createdAt ?: System.currentTimeMillis()
        )

        onEventSavedListener?.invoke(eventToSave)
        dismiss()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

