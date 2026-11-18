package com.devsorg.calenvault.ui.dialog

import android.content.res.ColorStateList
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import com.devsorg.calenvault.R
import com.devsorg.calenvault.data.model.CalendarEvent
import com.devsorg.calenvault.data.preferences.PreferencesManager
import com.devsorg.calenvault.databinding.DialogEventDetailBinding
import com.devsorg.calenvault.ui.calendar.CalendarUtils
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class EventDetailBottomSheet : BottomSheetDialogFragment() {

    private var _binding: DialogEventDetailBinding? = null
    private val binding get() = _binding!!

    private var event: CalendarEvent? = null

    var onEditClickListener: ((CalendarEvent) -> Unit)? = null
    var onDeleteClickListener: ((CalendarEvent) -> Unit)? = null

    companion object {
        private const val ARG_EVENT = "arg_event"

        fun newInstance(event: CalendarEvent): EventDetailBottomSheet {
            return EventDetailBottomSheet().apply {
                arguments = Bundle().apply {
                    putSerializable(ARG_EVENT, event)
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        @Suppress("DEPRECATION")
        event = arguments?.getSerializable(ARG_EVENT) as? CalendarEvent
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DialogEventDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val currentEvent = event ?: run {
            dismiss()
            return
        }

        bindEventData(currentEvent)
        setupActions(currentEvent)
    }

    private fun bindEventData(item: CalendarEvent) {
        val context = requireContext()
        val category = item.category
        val categoryColor = ContextCompat.getColor(context, category.colorRes)
        val categoryLightColor = ContextCompat.getColor(context, category.lightColorRes)

        binding.tvDetailCategory.text = context.getString(category.displayNameRes).uppercase()
        binding.tvDetailCategory.setTextColor(categoryColor)
        binding.tvDetailCategory.backgroundTintList = ColorStateList.valueOf(categoryLightColor)

        binding.tvDetailTitle.text = item.title
        binding.tvDetailDate.text = CalendarUtils.getDisplayDate(item.date)

        if (item.isAllDay) {
            binding.tvDetailTime.text = getString(R.string.all_day)
        } else {
            val timeFormat = PreferencesManager.getInstance(context).timeFormat
            val startFormatted = CalendarUtils.formatDisplayTime(item.startTime, timeFormat, context)
            val endFormatted = CalendarUtils.formatDisplayTime(item.endTime, timeFormat, context)
            binding.tvDetailTime.text = "$startFormatted – $endFormatted"
        }

        if (item.location.isNotBlank()) {
            binding.layoutDetailLocation.visibility = View.VISIBLE
            binding.tvDetailLocation.text = item.location
        } else {
            binding.layoutDetailLocation.visibility = View.GONE
        }

        if (item.description.isNotBlank()) {
            binding.layoutDetailDescription.visibility = View.VISIBLE
            binding.tvDetailDescription.text = item.description
        } else {
            binding.layoutDetailDescription.visibility = View.GONE
        }
    }

    private fun setupActions(item: CalendarEvent) {
        binding.btnDetailClose.setOnClickListener {
            dismiss()
        }

        binding.btnDetailEdit.setOnClickListener {
            dismiss()
            onEditClickListener?.invoke(item)
        }

        binding.btnDetailDelete.setOnClickListener {
            MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.delete_confirm_title)
                .setMessage(R.string.delete_event_confirm)
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.delete) { _, _ ->
                    dismiss()
                    onDeleteClickListener?.invoke(item)
                }
                .show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

