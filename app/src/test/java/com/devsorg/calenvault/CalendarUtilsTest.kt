package com.devsorg.calenvault

import com.devsorg.calenvault.data.model.EventCategory
import com.devsorg.calenvault.ui.calendar.CalendarUtils
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar

class CalendarUtilsTest {

    @Test
    fun testDateFormatting() {
        val cal = Calendar.getInstance().apply {
            set(Calendar.YEAR, 2026)
            set(Calendar.MONTH, Calendar.NOVEMBER)
            set(Calendar.DAY_OF_MONTH, 18)
        }
        val formatted = CalendarUtils.formatDate(cal)
        assertEquals("2026-11-18", formatted)
    }

    @Test
    fun testGenerateDaysForNovember2026() {
        val cal = Calendar.getInstance().apply {
            set(Calendar.YEAR, 2026)
            set(Calendar.MONTH, Calendar.NOVEMBER)
            set(Calendar.DAY_OF_MONTH, 1)
        }
        val days = CalendarUtils.generateDaysForMonth(
            monthCalendar = cal,
            selectedDateString = "2026-11-18",
            eventsByDate = emptyMap()
        )

        // November 2026 starts on Sunday (1st), has 30 days -> exactly 35 cells
        assertEquals(35, days.size)

        // The 18th should be selected
        val selectedDay = days.find { it.dateString == "2026-11-18" }
        assertTrue("Day 18 must be found and selected", selectedDay != null && selectedDay.isSelected)

        // Day 1 should be current month
        val firstDay = days.find { it.dateString == "2026-11-01" }
        assertTrue("Day 1 should be current month", firstDay != null && firstDay.isCurrentMonth)
    }

    @Test
    fun testGenerateDaysWithMondayStart() {
        val cal = Calendar.getInstance().apply {
            set(Calendar.YEAR, 2026)
            set(Calendar.NOVEMBER, 1)
            set(Calendar.DAY_OF_MONTH, 1)
        }
        val days = CalendarUtils.generateDaysForMonth(
            monthCalendar = cal,
            selectedDateString = "2026-11-18",
            eventsByDate = emptyMap(),
            startDayOfWeek = Calendar.MONDAY
        )
        // Total cells should be multiple of 7 (35 or 42)
        assertTrue(days.size % 7 == 0)
        // Day 18 must be found
        val day18 = days.find { it.dateString == "2026-11-18" }
        assertTrue(day18 != null && day18.isSelected)
    }

    @Test
    fun testCategoryFromId() {
        assertEquals(EventCategory.WORK, EventCategory.fromId("WORK"))
        assertEquals(EventCategory.MEETING, EventCategory.fromId("meeting"))
        assertEquals(EventCategory.HEALTH, EventCategory.fromId("HeaLTH"))
        assertEquals(EventCategory.OTHER, EventCategory.fromId("UNKNOWN_CATEGORY"))
        assertEquals(EventCategory.OTHER, EventCategory.fromId(null))
    }

    @Test
    fun testFormatDisplayTime() {
        val displayTime = CalendarUtils.formatDisplayTime("14:30")
        assertTrue("Display time should contain 2:30", displayTime.contains("2:30"))
    }
}

