package com.devsorg.calenvault.data.model

import java.io.Serializable

data class CalendarEvent(
    val id: Long = 0,
    val title: String,
    val description: String = "",
    val location: String = "",
    val date: String,            // "yyyy-MM-dd"
    val startTime: String = "09:00", // "HH:mm"
    val endTime: String = "10:00",   // "HH:mm"
    val isAllDay: Boolean = false,
    val category: EventCategory = EventCategory.WORK,
    val isCompleted: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
) : Serializable

