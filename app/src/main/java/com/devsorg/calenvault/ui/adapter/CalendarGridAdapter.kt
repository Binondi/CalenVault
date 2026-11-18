package com.devsorg.calenvault.ui.adapter

import android.content.res.ColorStateList
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.devsorg.calenvault.R
import com.devsorg.calenvault.data.model.CalendarDay
import com.devsorg.calenvault.databinding.ItemCalendarDayBinding

class CalendarGridAdapter(
    private val onDayClick: (CalendarDay) -> Unit
) : ListAdapter<CalendarDay, CalendarGridAdapter.DayViewHolder>(DayDiffCallback()) {

    var highlightWeekends: Boolean = true
    var showEventDots: Boolean = true

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DayViewHolder {
        val binding = ItemCalendarDayBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return DayViewHolder(binding, onDayClick)
    }

    override fun onBindViewHolder(holder: DayViewHolder, position: Int) {
        holder.bind(getItem(position), highlightWeekends, showEventDots)
    }

    class DayViewHolder(
        private val binding: ItemCalendarDayBinding,
        private val onDayClick: (CalendarDay) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(day: CalendarDay, highlightWeekends: Boolean, showEventDots: Boolean) {
            val context = itemView.context
            binding.tvDayNumber.text = day.dayNumber.toString()

            // Resolve theme colors using MaterialColors
            val colorOnSurface = com.google.android.material.color.MaterialColors.getColor(
                binding.root,
                com.google.android.material.R.attr.colorOnSurface
            )
            val colorOnPrimary = com.google.android.material.color.MaterialColors.getColor(
                binding.root,
                com.google.android.material.R.attr.colorOnPrimary
            )
            val colorPrimary = com.google.android.material.color.MaterialColors.getColor(
                binding.root,
                androidx.appcompat.R.attr.colorPrimary
            )

            when {
                day.isSelected -> {
                    binding.dayContainer.setBackgroundResource(R.drawable.bg_calendar_day_selected)
                    binding.tvDayNumber.setTextColor(colorOnPrimary)
                }
                day.isToday -> {
                    binding.dayContainer.setBackgroundResource(R.drawable.bg_calendar_day_today)
                    binding.tvDayNumber.setTextColor(colorPrimary)
                }
                else -> {
                    binding.dayContainer.setBackgroundResource(0)
                    if (day.isCurrentMonth) {
                        if (day.isWeekend && highlightWeekends) {
                            binding.tvDayNumber.setTextColor(
                                ContextCompat.getColor(context, R.color.calendar_day_weekend)
                            )
                        } else {
                            binding.tvDayNumber.setTextColor(colorOnSurface)
                        }
                    } else {
                        binding.tvDayNumber.setTextColor(
                            ContextCompat.getColor(context, R.color.calendar_day_other_month)
                        )
                    }
                }
            }

            // Event indicators
            val events = day.events
            if (events.isNotEmpty() && showEventDots) {
                binding.layoutDots.visibility = View.VISIBLE
                val dots = listOf(binding.dot1, binding.dot2, binding.dot3)

                dots.forEachIndexed { index, dotView ->
                    if (index < events.size) {
                        dotView.visibility = View.VISIBLE
                        val dotColor = if (day.isSelected) {
                            colorOnPrimary
                        } else {
                            ContextCompat.getColor(context, events[index].category.colorRes)
                        }
                        dotView.backgroundTintList = ColorStateList.valueOf(dotColor)
                    } else {
                        dotView.visibility = View.GONE
                    }
                }
            } else {
                binding.layoutDots.visibility = View.GONE
            }

            itemView.setOnClickListener {
                onDayClick(day)
            }
        }
    }

    private class DayDiffCallback : DiffUtil.ItemCallback<CalendarDay>() {
        override fun areItemsTheSame(oldItem: CalendarDay, newItem: CalendarDay): Boolean {
            return oldItem.dateString == newItem.dateString
        }

        override fun areContentsTheSame(oldItem: CalendarDay, newItem: CalendarDay): Boolean {
            return oldItem == newItem
        }
    }
}
