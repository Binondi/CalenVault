package com.devsorg.calenvault.data.db

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.devsorg.calenvault.data.model.CalendarEvent
import com.devsorg.calenvault.data.model.EventCategory

class EventDatabaseHelper(context: Context) :
    SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        const val DATABASE_NAME = "calenvault_events.db"
        const val DATABASE_VERSION = 2

        const val TABLE_EVENTS = "events"
        const val COLUMN_ID = "_id"
        const val COLUMN_TITLE = "title"
        const val COLUMN_DESCRIPTION = "description"
        const val COLUMN_LOCATION = "location"
        const val COLUMN_DATE = "event_date"
        const val COLUMN_START_TIME = "start_time"
        const val COLUMN_END_TIME = "end_time"
        const val COLUMN_IS_ALL_DAY = "is_all_day"
        const val COLUMN_CATEGORY = "category"
        const val COLUMN_IS_COMPLETED = "is_completed"
        const val COLUMN_CREATED_AT = "created_at"

        @Volatile
        private var instance: EventDatabaseHelper? = null

        fun getInstance(context: Context): EventDatabaseHelper {
            return instance ?: synchronized(this) {
                instance ?: EventDatabaseHelper(context.applicationContext).also { instance = it }
            }
        }
    }

    override fun onCreate(db: SQLiteDatabase) {
        val createTableQuery = """
            CREATE TABLE $TABLE_EVENTS (
                $COLUMN_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COLUMN_TITLE TEXT NOT NULL,
                $COLUMN_DESCRIPTION TEXT,
                $COLUMN_LOCATION TEXT,
                $COLUMN_DATE TEXT NOT NULL,
                $COLUMN_START_TIME TEXT,
                $COLUMN_END_TIME TEXT,
                $COLUMN_IS_ALL_DAY INTEGER DEFAULT 0,
                $COLUMN_CATEGORY TEXT NOT NULL,
                $COLUMN_IS_COMPLETED INTEGER DEFAULT 0,
                $COLUMN_CREATED_AT INTEGER
            )
        """.trimIndent()
        db.execSQL(createTableQuery)

        // Fast index on event_date and category
        db.execSQL("CREATE INDEX idx_events_date ON $TABLE_EVENTS ($COLUMN_DATE)")
        db.execSQL("CREATE INDEX idx_events_category ON $TABLE_EVENTS ($COLUMN_CATEGORY)")
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS $TABLE_EVENTS")
        onCreate(db)
    }

    fun insertEvent(event: CalendarEvent): Long {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(COLUMN_TITLE, event.title)
            put(COLUMN_DESCRIPTION, event.description)
            put(COLUMN_LOCATION, event.location)
            put(COLUMN_DATE, event.date)
            put(COLUMN_START_TIME, event.startTime)
            put(COLUMN_END_TIME, event.endTime)
            put(COLUMN_IS_ALL_DAY, if (event.isAllDay) 1 else 0)
            put(COLUMN_CATEGORY, event.category.id)
            put(COLUMN_IS_COMPLETED, if (event.isCompleted) 1 else 0)
            put(COLUMN_CREATED_AT, event.createdAt)
        }
        return db.insert(TABLE_EVENTS, null, values)
    }

    fun updateEvent(event: CalendarEvent): Int {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(COLUMN_TITLE, event.title)
            put(COLUMN_DESCRIPTION, event.description)
            put(COLUMN_LOCATION, event.location)
            put(COLUMN_DATE, event.date)
            put(COLUMN_START_TIME, event.startTime)
            put(COLUMN_END_TIME, event.endTime)
            put(COLUMN_IS_ALL_DAY, if (event.isAllDay) 1 else 0)
            put(COLUMN_CATEGORY, event.category.id)
            put(COLUMN_IS_COMPLETED, if (event.isCompleted) 1 else 0)
        }
        return db.update(TABLE_EVENTS, values, "$COLUMN_ID = ?", arrayOf(event.id.toString()))
    }

    fun toggleCompleted(id: Long, isCompleted: Boolean): Int {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(COLUMN_IS_COMPLETED, if (isCompleted) 1 else 0)
        }
        return db.update(TABLE_EVENTS, values, "$COLUMN_ID = ?", arrayOf(id.toString()))
    }

    fun deleteEvent(id: Long): Int {
        val db = writableDatabase
        return db.delete(TABLE_EVENTS, "$COLUMN_ID = ?", arrayOf(id.toString()))
    }

    fun clearAllEvents(): Int {
        val db = writableDatabase
        return db.delete(TABLE_EVENTS, null, null)
    }

    fun getEventById(id: Long): CalendarEvent? {
        val db = readableDatabase
        val cursor = db.query(
            TABLE_EVENTS,
            null,
            "$COLUMN_ID = ?",
            arrayOf(id.toString()),
            null,
            null,
            null
        )
        return cursor.use {
            if (it.moveToFirst()) cursorToEvent(it) else null
        }
    }

    fun getEventsForDate(date: String): List<CalendarEvent> {
        val events = mutableListOf<CalendarEvent>()
        val db = readableDatabase
        val cursor = db.query(
            TABLE_EVENTS,
            null,
            "$COLUMN_DATE = ?",
            arrayOf(date),
            null,
            null,
            "$COLUMN_IS_ALL_DAY DESC, $COLUMN_START_TIME ASC, $COLUMN_TITLE ASC"
        )
        cursor.use {
            while (it.moveToNext()) {
                events.add(cursorToEvent(it))
            }
        }
        return events
    }

    fun getEventsForMonth(yearMonthPrefix: String): List<CalendarEvent> {
        val events = mutableListOf<CalendarEvent>()
        val db = readableDatabase
        val cursor = db.query(
            TABLE_EVENTS,
            null,
            "$COLUMN_DATE LIKE ?",
            arrayOf("$yearMonthPrefix%"),
            null,
            null,
            "$COLUMN_DATE ASC, $COLUMN_START_TIME ASC"
        )
        cursor.use {
            while (it.moveToNext()) {
                events.add(cursorToEvent(it))
            }
        }
        return events
    }

    fun searchEvents(query: String, category: EventCategory?): List<CalendarEvent> {
        val events = mutableListOf<CalendarEvent>()
        val db = readableDatabase
        val selection = StringBuilder()
        val args = mutableListOf<String>()

        if (query.isNotBlank()) {
            selection.append("($COLUMN_TITLE LIKE ? OR $COLUMN_DESCRIPTION LIKE ? OR $COLUMN_LOCATION LIKE ?)")
            val pattern = "%$query%"
            args.add(pattern)
            args.add(pattern)
            args.add(pattern)
        }

        if (category != null) {
            if (selection.isNotEmpty()) {
                selection.append(" AND ")
            }
            selection.append("$COLUMN_CATEGORY = ?")
            args.add(category.id)
        }

        val cursor = db.query(
            TABLE_EVENTS,
            null,
            if (selection.isNotEmpty()) selection.toString() else null,
            if (args.isNotEmpty()) args.toTypedArray() else null,
            null,
            null,
            "$COLUMN_DATE DESC, $COLUMN_START_TIME ASC"
        )
        cursor.use {
            while (it.moveToNext()) {
                events.add(cursorToEvent(it))
            }
        }
        return events
    }

    private fun cursorToEvent(cursor: Cursor): CalendarEvent {
        return CalendarEvent(
            id = cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_ID)),
            title = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_TITLE)),
            description = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_DESCRIPTION)) ?: "",
            location = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_LOCATION)) ?: "",
            date = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_DATE)),
            startTime = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_START_TIME)) ?: "09:00",
            endTime = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_END_TIME)) ?: "10:00",
            isAllDay = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_IS_ALL_DAY)) == 1,
            category = EventCategory.fromId(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_CATEGORY))),
            isCompleted = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_IS_COMPLETED)) == 1,
            createdAt = cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_CREATED_AT))
        )
    }
}

