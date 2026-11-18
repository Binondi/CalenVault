package com.devsorg.calenvault.data.model

data class CalendarDay(
    val dayNumber: Int,
    val dateString: String,      // "yyyy-MM-dd"
    val isCurrentMonth: Boolean,
    val isToday: Boolean,
    val isWeekend: Boolean = false,
    var isSelected: Boolean = false,
    val events: List<CalendarEvent> = emptyList()
)
