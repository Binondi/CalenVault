package com.devsorg.calenvault.vault.utils

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import java.security.MessageDigest

class PrefsUtil(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("CalenVaultSecret", Context.MODE_PRIVATE)

    fun hasPassword(): Boolean {
        return prefs.getString("password", "")?.isNotEmpty() ?: false
    }

    fun savePassword(password: String) {
        val hashedPassword = hashPassword(password)
        prefs.edit {
            putString("password", hashedPassword)
        }
    }

    fun setBoolean(key: String, value: Boolean) {
        prefs.edit { putBoolean(key, value) }
    }

    fun setInt(key: String, value: Int) {
        prefs.edit { putInt(key, value) }
    }

    fun getBoolean(key: String, defValue: Boolean = false): Boolean {
        return prefs.getBoolean(key, defValue)
    }

    fun getInt(key: String, defValue: Int): Int {
        return prefs.getInt(key, defValue)
    }

    fun resetPassword() {
        prefs.edit {
            remove("password")
                .remove("security_question")
                .remove("security_answer")
        }
    }

    fun validatePassword(input: String): Boolean {
        val stored = prefs.getString("password", "") ?: ""
        return stored.isNotEmpty() && stored == hashPassword(input)
    }

    fun saveSecurityQA(question: String, answer: String) {
        prefs.edit {
            putString("security_question", question)
                .putString("security_answer", hashPassword(answer))
        }
    }

    fun validateSecurityAnswer(answer: String): Boolean {
        val stored = prefs.getString("security_answer", "") ?: ""
        return stored == hashPassword(answer)
    }

    fun getSecurityQuestion(): String? {
        return prefs.getString("security_question", null)
    }

    private fun hashPassword(password: String): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest(password.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }

    fun shouldShowDonateOnVaultOpen(): Boolean {
        return !getBoolean(PREF_DONATE_NEVER_SHOW_AGAIN, false)
    }

    fun setDonateShown() {
        setBoolean(PREF_DONATE_SHOWN_FIRST_TIME, true)
    }

    fun setDonateNeverShowAgain(neverShow: Boolean) {
        setBoolean(PREF_DONATE_NEVER_SHOW_AGAIN, neverShow)
    }

    fun hasSeenOnboarding(): Boolean {
        return getBoolean(PREF_HAS_SEEN_ONBOARDING, false)
    }

    fun setSeenOnboarding(seen: Boolean) {
        setBoolean(PREF_HAS_SEEN_ONBOARDING, seen)
    }

    fun isShakeToCloseEnabled(): Boolean {
        return getBoolean(PREF_SHAKE_TO_CLOSE, false)
    }

    fun setShakeToCloseEnabled(enabled: Boolean) {
        setBoolean(PREF_SHAKE_TO_CLOSE, enabled)
    }

    fun getShakeSensitivity(): Int {
        return getInt(PREF_SHAKE_SENSITIVITY, 3)
    }

    fun setShakeSensitivity(sensitivity: Int) {
        setInt(PREF_SHAKE_SENSITIVITY, sensitivity.coerceIn(1, 5))
    }

    fun isShakeVibrateEnabled(): Boolean {
        return getBoolean(PREF_SHAKE_VIBRATE, true)
    }

    fun setShakeVibrateEnabled(enabled: Boolean) {
        setBoolean(PREF_SHAKE_VIBRATE, enabled)
    }

    fun hasSeenShakeAnnouncement(): Boolean {
        return getBoolean(PREF_SEEN_SHAKE_ANNOUNCEMENT, false)
    }

    fun setSeenShakeAnnouncement(seen: Boolean) {
        setBoolean(PREF_SEEN_SHAKE_ANNOUNCEMENT, seen)
    }

    // Covert Vault Trigger Settings (Configured inside secret vault settings)
    fun isTriggerSearchEnabled(): Boolean {
        return getBoolean(PREF_TRIGGER_SEARCH, true)
    }

    fun setTriggerSearchEnabled(enabled: Boolean) {
        setBoolean(PREF_TRIGGER_SEARCH, enabled)
    }

    fun isTriggerTitleLongPressEnabled(): Boolean {
        return getBoolean(PREF_TRIGGER_LONG_PRESS, true)
    }

    fun setTriggerTitleLongPressEnabled(enabled: Boolean) {
        setBoolean(PREF_TRIGGER_LONG_PRESS, enabled)
    }

    fun isTriggerTodayMultiTapEnabled(): Boolean {
        return getBoolean(PREF_TRIGGER_MULTI_TAP, true)
    }

    fun setTriggerTodayMultiTapEnabled(enabled: Boolean) {
        setBoolean(PREF_TRIGGER_MULTI_TAP, enabled)
    }

    companion object {
        const val PREF_HAS_SEEN_ONBOARDING = "has_seen_onboarding"
        const val PREF_DONATE_NEVER_SHOW_AGAIN = "donate_never_show_again"
        const val PREF_DONATE_SHOWN_FIRST_TIME = "donate_shown_first_time"
        const val PREF_DONATE_VAULT_OPEN_COUNT = "donate_vault_open_count"
        const val PREF_SHAKE_TO_CLOSE = "shake_to_close"
        const val PREF_SHAKE_SENSITIVITY = "shake_sensitivity"
        const val PREF_SHAKE_VIBRATE = "shake_vibrate"
        const val PREF_SEEN_SHAKE_ANNOUNCEMENT = "seen_shake_announcement"

        const val PREF_TRIGGER_SEARCH = "vault_trigger_search"
        const val PREF_TRIGGER_LONG_PRESS = "vault_trigger_long_press"
        const val PREF_TRIGGER_MULTI_TAP = "vault_trigger_multi_tap"
    }
}


