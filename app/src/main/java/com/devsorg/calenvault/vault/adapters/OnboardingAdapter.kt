package com.devsorg.calenvault.vault.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.recyclerview.widget.RecyclerView
import com.devsorg.calenvault.R
import com.devsorg.calenvault.databinding.ItemOnboardingPageBinding
import com.devsorg.calenvault.vault.utils.PrefsUtil

enum class TriggerType {
    SEARCH,
    LONG_PRESS,
    MULTI_TAP
}

data class OnboardingPage(
    @param:DrawableRes val iconRes: Int,
    @param:StringRes val tagRes: Int,
    @param:StringRes val titleRes: Int,
    @param:StringRes val descRes: Int,
    @param:StringRes val highlight1Res: Int = 0,
    @param:StringRes val highlight2Res: Int = 0,
    @param:StringRes val highlight3Res: Int = 0,
    val triggerType: TriggerType? = null
)

class OnboardingAdapter(
    private val pages: List<OnboardingPage>,
    private val prefs: PrefsUtil
) : RecyclerView.Adapter<OnboardingAdapter.OnboardingViewHolder>() {

    inner class OnboardingViewHolder(val binding: ItemOnboardingPageBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(page: OnboardingPage) {
            binding.ivHeroIcon.setImageResource(page.iconRes)
            binding.tvTag.setText(page.tagRes)
            binding.tvTitle.setText(page.titleRes)
            binding.tvDescription.setText(page.descRes)

            if (page.triggerType == null) {
                // Standard page
                binding.cardHighlights.visibility = View.VISIBLE
                binding.cardVisualMock.visibility = View.GONE
                binding.cardSwitch.visibility = View.GONE

                binding.tvHighlight1.setText(page.highlight1Res)
                binding.tvHighlight2.setText(page.highlight2Res)
                binding.tvHighlight3.setText(page.highlight3Res)
            } else {
                // Trigger page with visual mock and switch
                binding.cardHighlights.visibility = View.GONE
                binding.cardVisualMock.visibility = View.VISIBLE
                binding.cardSwitch.visibility = View.VISIBLE

                binding.layoutVisualSearch.visibility = if (page.triggerType == TriggerType.SEARCH) View.VISIBLE else View.GONE
                binding.layoutVisualLongPress.visibility = if (page.triggerType == TriggerType.LONG_PRESS) View.VISIBLE else View.GONE
                binding.layoutVisualMultiTap.visibility = if (page.triggerType == TriggerType.MULTI_TAP) View.VISIBLE else View.GONE

                // Configure switch
                binding.switchTrigger.setOnCheckedChangeListener(null)
                val isCurrentlyEnabled = when (page.triggerType) {
                    TriggerType.SEARCH -> prefs.isTriggerSearchEnabled()
                    TriggerType.LONG_PRESS -> prefs.isTriggerTitleLongPressEnabled()
                    TriggerType.MULTI_TAP -> prefs.isTriggerTodayMultiTapEnabled()
                }
                binding.switchTrigger.isChecked = isCurrentlyEnabled
                updateSwitchLabel(page.triggerType, isCurrentlyEnabled)

                binding.switchTrigger.setOnCheckedChangeListener { _, isChecked ->
                    val other1: Boolean
                    val other2: Boolean
                    when (page.triggerType) {
                        TriggerType.SEARCH -> {
                            other1 = prefs.isTriggerTitleLongPressEnabled()
                            other2 = prefs.isTriggerTodayMultiTapEnabled()
                        }
                        TriggerType.LONG_PRESS -> {
                            other1 = prefs.isTriggerSearchEnabled()
                            other2 = prefs.isTriggerTodayMultiTapEnabled()
                        }
                        TriggerType.MULTI_TAP -> {
                            other1 = prefs.isTriggerSearchEnabled()
                            other2 = prefs.isTriggerTitleLongPressEnabled()
                        }
                    }

                    // Enforce safety rule: user can only disable 2 of them, not all 3
                    if (!isChecked && !other1 && !other2) {
                        binding.switchTrigger.isChecked = true
                        Toast.makeText(
                            binding.root.context,
                            binding.root.context.getString(R.string.at_least_one_trigger_required),
                            Toast.LENGTH_SHORT
                        ).show()
                    } else {
                        when (page.triggerType) {
                            TriggerType.SEARCH -> prefs.setTriggerSearchEnabled(isChecked)
                            TriggerType.LONG_PRESS -> prefs.setTriggerTitleLongPressEnabled(isChecked)
                            TriggerType.MULTI_TAP -> prefs.setTriggerTodayMultiTapEnabled(isChecked)
                        }
                        updateSwitchLabel(page.triggerType, isChecked)
                    }
                }
            }
        }

        private fun updateSwitchLabel(type: TriggerType, enabled: Boolean) {
            when (type) {
                TriggerType.SEARCH -> {
                    binding.tvSwitchTitle.text = "Enable Search Bar Trigger"
                    binding.tvSwitchSubtitle.text = if (enabled) "Active — type PIN in search bar" else "Disabled — search bar unlock inactive"
                }
                TriggerType.LONG_PRESS -> {
                    binding.tvSwitchTitle.text = "Enable Title Long-Press"
                    binding.tvSwitchSubtitle.text = if (enabled) "Active — hold header title for 2.5s" else "Disabled — header long-press inactive"
                }
                TriggerType.MULTI_TAP -> {
                    binding.tvSwitchTitle.text = "Enable Multi-Tap Today"
                    binding.tvSwitchSubtitle.text = if (enabled) "Active — tap Today button 4 times" else "Disabled — today multi-tap inactive"
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): OnboardingViewHolder {
        val binding = ItemOnboardingPageBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return OnboardingViewHolder(binding)
    }

    override fun onBindViewHolder(holder: OnboardingViewHolder, position: Int) {
        holder.bind(pages[position])
    }

    override fun getItemCount(): Int = pages.size
}
