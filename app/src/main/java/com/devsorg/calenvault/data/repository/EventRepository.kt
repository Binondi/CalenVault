package com.devsorg.calenvault.data.repository

import com.devsorg.calenvault.data.db.EventDatabaseHelper
import com.devsorg.calenvault.data.model.CalendarEvent
import com.devsorg.calenvault.data.model.EventCategory

class EventRepository(private val dbHelper: EventDatabaseHelper) {

    fun saveEvent(event: CalendarEvent): Long {
        return if (event.id > 0) {
            dbHelper.updateEvent(event)
            event.id
        } else {
            dbHelper.insertEvent(event)
        }
    }

    fun deleteEvent(id: Long): Boolean {
        return dbHelper.deleteEvent(id) > 0
    }

    fun clearAllEvents(): Boolean {
        return dbHelper.clearAllEvents() >= 0
    }

    fun toggleCompleted(id: Long, isCompleted: Boolean): Boolean {
        return dbHelper.toggleCompleted(id, isCompleted) > 0
    }

    fun getEventById(id: Long): CalendarEvent? {
        return dbHelper.getEventById(id)
    }

    fun getEventsForDate(date: String): List<CalendarEvent> {
        return dbHelper.getEventsForDate(date)
    }

    fun getEventsForMonth(yearMonthPrefix: String): List<CalendarEvent> {
        return dbHelper.getEventsForMonth(yearMonthPrefix)
    }

    fun searchEvents(query: String, category: EventCategory?): List<CalendarEvent> {
        return dbHelper.searchEvents(query, category)
    }
}

