package com.devsorg.calenvault.ui.calendar

import android.content.Context
import com.devsorg.calenvault.data.model.CalendarDay
import com.devsorg.calenvault.data.model.CalendarEvent
import com.devsorg.calenvault.data.preferences.TimeFormatPref
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

object CalendarUtils {

    private val DATE_FORMAT = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    private val MONTH_YEAR_FORMAT = SimpleDateFormat("MMMM yyyy", Locale.getDefault())
    private val YEAR_MONTH_FORMAT = SimpleDateFormat("yyyy-MM", Locale.US)
    private val DISPLAY_DATE_FORMAT = SimpleDateFormat("EEEE, MMM d, yyyy", Locale.getDefault())
    private val TIME_24_FORMAT = SimpleDateFormat("HH:mm", Locale.US)
    private val TIME_12_FORMAT = SimpleDateFormat("h:mm a", Locale.getDefault())

    fun formatDate(calendar: Calendar): String {
        return DATE_FORMAT.format(calendar.time)
    }

    fun formatDate(year: Int, monthZeroIndexed: Int, day: Int): String {
        val cal = Calendar.getInstance().apply {
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, monthZeroIndexed)
            set(Calendar.DAY_OF_MONTH, day)
        }
        return DATE_FORMAT.format(cal.time)
    }

    fun getTodayString(): String {
        return DATE_FORMAT.format(Calendar.getInstance().time)
    }

    fun getMonthYearTitle(calendar: Calendar): String {
        return MONTH_YEAR_FORMAT.format(calendar.time)
    }

    fun getYearMonthPrefix(calendar: Calendar): String {
        return YEAR_MONTH_FORMAT.format(calendar.time)
    }

    fun getDisplayDate(dateString: String): String {
        return try {
            val date = DATE_FORMAT.parse(dateString)
            if (date != null) DISPLAY_DATE_FORMAT.format(date) else dateString
        } catch (_: Exception) {
            dateString
        }
    }

    fun formatDisplayTime(
        time24: String,
        pref: TimeFormatPref = TimeFormatPref.SYSTEM,
        context: Context? = null
    ): String {
        return try {
            val date = TIME_24_FORMAT.parse(time24) ?: return time24
            when (pref) {
                TimeFormatPref.FORMAT_12H -> TIME_12_FORMAT.format(date)
                TimeFormatPref.FORMAT_24H -> TIME_24_FORMAT.format(date)
                TimeFormatPref.SYSTEM -> {
                    if (context != null && android.text.format.DateFormat.is24HourFormat(context)) {
                        TIME_24_FORMAT.format(date)
                    } else {
                        TIME_12_FORMAT.format(date)
                    }
                }
            }
        } catch (_: Exception) {
            time24
        }
    }

    fun generateDaysForMonth(
        monthCalendar: Calendar,
        selectedDateString: String,
        eventsByDate: Map<String, List<CalendarEvent>>,
        startDayOfWeek: Int = Calendar.SUNDAY
    ): List<CalendarDay> {
        val days = mutableListOf<CalendarDay>()

        val cal = (monthCalendar.clone() as Calendar).apply {
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        val targetMonth = cal.get(Calendar.MONTH)
        val todayString = getTodayString()

        // Calculate days to prepend based on preferred startDayOfWeek (e.g. Sunday or Monday)
        val firstDayOfWeek = cal.get(Calendar.DAY_OF_WEEK)
        val daysBefore = (firstDayOfWeek - startDayOfWeek + 7) % 7

        if (daysBefore > 0) {
            val prevMonthCal = (cal.clone() as Calendar).apply {
                add(Calendar.DAY_OF_MONTH, -daysBefore)
            }
            for (i in 0 until daysBefore) {
                val dateStr = formatDate(prevMonthCal)
                val dayOfWeek = prevMonthCal.get(Calendar.DAY_OF_WEEK)
                val isWeekend = (dayOfWeek == Calendar.SUNDAY || dayOfWeek == Calendar.SATURDAY)
                days.add(
                    CalendarDay(
                        dayNumber = prevMonthCal.get(Calendar.DAY_OF_MONTH),
                        dateString = dateStr,
                        isCurrentMonth = false,
                        isToday = (dateStr == todayString),
                        isWeekend = isWeekend,
                        isSelected = (dateStr == selectedDateString),
                        events = eventsByDate[dateStr] ?: emptyList()
                    )
                )
                prevMonthCal.add(Calendar.DAY_OF_MONTH, 1)
            }
        }

        // Current month days
        val maxDaysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
        for (dayNum in 1..maxDaysInMonth) {
            cal.set(Calendar.DAY_OF_MONTH, dayNum)
            val dateStr = formatDate(cal)
            val dayOfWeek = cal.get(Calendar.DAY_OF_WEEK)
            val isWeekend = (dayOfWeek == Calendar.SUNDAY || dayOfWeek == Calendar.SATURDAY)
            days.add(
                CalendarDay(
                    dayNumber = dayNum,
                    dateString = dateStr,
                    isCurrentMonth = true,
                    isToday = (dateStr == todayString),
                    isWeekend = isWeekend,
                    isSelected = (dateStr == selectedDateString),
                    events = eventsByDate[dateStr] ?: emptyList()
                )
            )
        }

        // Remaining days to complete grid (multiples of 7, 35 or 42)
        val totalCells = if (days.size <= 35) 35 else 42
        val daysAfter = totalCells - days.size
        if (daysAfter > 0) {
            val nextMonthCal = (cal.clone() as Calendar).apply {
                set(Calendar.DAY_OF_MONTH, maxDaysInMonth)
                add(Calendar.DAY_OF_MONTH, 1)
            }
            for (i in 0 until daysAfter) {
                val dateStr = formatDate(nextMonthCal)
                val dayOfWeek = nextMonthCal.get(Calendar.DAY_OF_WEEK)
                val isWeekend = (dayOfWeek == Calendar.SUNDAY || dayOfWeek == Calendar.SATURDAY)
                days.add(
                    CalendarDay(
                        dayNumber = nextMonthCal.get(Calendar.DAY_OF_MONTH),
                        dateString = dateStr,
                        isCurrentMonth = false,
                        isToday = (dateStr == todayString),
                        isWeekend = isWeekend,
                        isSelected = (dateStr == selectedDateString),
                        events = eventsByDate[dateStr] ?: emptyList()
                    )
                )
                nextMonthCal.add(Calendar.DAY_OF_MONTH, 1)
            }
        }

        return days
    }
}
