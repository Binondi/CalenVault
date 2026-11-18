package com.devsorg.calenvault.ui.adapter

import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.devsorg.calenvault.R
import com.devsorg.calenvault.data.model.CalendarEvent
import com.devsorg.calenvault.data.preferences.TimeFormatPref
import com.devsorg.calenvault.databinding.ItemCalendarEventBinding
import com.devsorg.calenvault.ui.calendar.CalendarUtils

class EventAdapter(
    private val onEventClick: (CalendarEvent) -> Unit
) : ListAdapter<CalendarEvent, EventAdapter.EventViewHolder>(EventDiffCallback()) {

    var timeFormatPref: TimeFormatPref = TimeFormatPref.SYSTEM

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EventViewHolder {
        val binding = ItemCalendarEventBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return EventViewHolder(binding, onEventClick)
    }

    override fun onBindViewHolder(holder: EventViewHolder, position: Int) {
        holder.bind(getItem(position), timeFormatPref)
    }

    class EventViewHolder(
        private val binding: ItemCalendarEventBinding,
        private val onEventClick: (CalendarEvent) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(event: CalendarEvent, timeFormatPref: TimeFormatPref) {
            val context = itemView.context
            val category = event.category

            val categoryColor = ContextCompat.getColor(context, category.colorRes)
            val categoryLightColor = ContextCompat.getColor(context, category.lightColorRes)

            // Category stripe & chip
            binding.viewCategoryStripe.setBackgroundColor(categoryColor)
            binding.chipCategory.text = context.getString(category.displayNameRes).uppercase()
            binding.chipCategory.setTextColor(categoryColor)
            binding.chipCategory.backgroundTintList = ColorStateList.valueOf(categoryLightColor)

            // Title
            binding.tvTitle.text = event.title

            // Time
            if (event.isAllDay) {
                binding.tvTime.text = context.getString(R.string.all_day)
            } else {
                val startFormatted = CalendarUtils.formatDisplayTime(event.startTime, timeFormatPref, context)
                val endFormatted = CalendarUtils.formatDisplayTime(event.endTime, timeFormatPref, context)
                binding.tvTime.text = "$startFormatted – $endFormatted"
            }

            // Location
            if (event.location.isNotBlank()) {
                binding.layoutLocation.visibility = View.VISIBLE
                binding.tvLocation.text = event.location
            } else {
                binding.layoutLocation.visibility = View.GONE
            }

            // Description
            if (event.description.isNotBlank()) {
                binding.tvDescription.visibility = View.VISIBLE
                binding.tvDescription.text = event.description
            } else {
                binding.tvDescription.visibility = View.GONE
            }

            itemView.setOnClickListener {
                onEventClick(event)
            }
        }
    }

    private class EventDiffCallback : DiffUtil.ItemCallback<CalendarEvent>() {
        override fun areItemsTheSame(oldItem: CalendarEvent, newItem: CalendarEvent): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: CalendarEvent, newItem: CalendarEvent): Boolean {
            return oldItem == newItem
        }
    }
}
