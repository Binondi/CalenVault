package com.devsorg.calenvault.data.model

import androidx.annotation.ColorRes
import androidx.annotation.StringRes
import com.devsorg.calenvault.R

enum class EventCategory(
    val id: String,
    @param:StringRes @get:StringRes val displayNameRes: Int,
    @param:ColorRes @get:ColorRes val colorRes: Int,
    @param:ColorRes @get:ColorRes val lightColorRes: Int
) {
    WORK("WORK", R.string.category_work, R.color.category_work, R.color.category_work_light),
    PERSONAL("PERSONAL", R.string.category_personal, R.color.category_personal, R.color.category_personal_light),
    MEETING("MEETING", R.string.category_meeting, R.color.category_meeting, R.color.category_meeting_light),
    REMINDER("REMINDER", R.string.category_reminder, R.color.category_reminder, R.color.category_reminder_light),
    HEALTH("HEALTH", R.string.category_health, R.color.category_health, R.color.category_health_light),
    OTHER("OTHER", R.string.category_other, R.color.category_other, R.color.category_other_light);

    companion object {
        fun fromId(id: String?): EventCategory {
            return entries.firstOrNull { it.id.equals(id, ignoreCase = true) } ?: OTHER
        }
    }
}
