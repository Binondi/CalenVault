package com.devsorg.calenvault

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.WindowManager
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.devsorg.calenvault.databinding.ActivityChangePasswordBinding
import com.devsorg.calenvault.databinding.ActivitySetupPasswordBinding
import com.devsorg.calenvault.vault.activities.SetupSecurityQuestionActivity
import com.devsorg.calenvault.vault.utils.DialogUtil
import com.devsorg.calenvault.vault.utils.PrefsUtil

class SetupPasswordActivity : AppCompatActivity() {
    private var setupBinding: ActivitySetupPasswordBinding? = null
    private var changeBinding: ActivityChangePasswordBinding? = null
    private var hasPassword = false
    private val prefs: PrefsUtil by lazy { PrefsUtil(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setupFlagSecure()
        hasPassword = prefs.hasPassword()
        enableEdgeToEdge()

        if (hasPassword) {
            val binding2 = ActivityChangePasswordBinding.inflate(layoutInflater)
            changeBinding = binding2
            setContentView(binding2.root)
            setViewPadding(binding2.root)
            setupChangePasswordListeners(binding2)
        } else {
            val binding = ActivitySetupPasswordBinding.inflate(layoutInflater)
            setupBinding = binding
            setContentView(binding.root)
            binding.toolbar.navigationIcon = null
            setViewPadding(binding.root)
            setupCreatePasswordListeners(binding)
        }

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (!hasPassword) {
                    finishAffinity()
                } else {
                    finish()
                }
            }
        })
    }

    override fun onResume() {
        super.onResume()
        setupFlagSecure()
        // If password was already successfully set by next step, close this setup activity
        if (!hasPassword && prefs.hasPassword()) {
            finish()
        }
    }

    private fun setupFlagSecure() {
        if (prefs.getBoolean("screenshot_restriction", true)) {
            window.setFlags(
                WindowManager.LayoutParams.FLAG_SECURE,
                WindowManager.LayoutParams.FLAG_SECURE
            )
        } else {
            window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
        }
    }

    private fun setViewPadding(view: android.view.View) {
        ViewCompat.setOnApplyWindowInsetsListener(view) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    private fun setupCreatePasswordListeners(binding: ActivitySetupPasswordBinding) {
        binding.btnNextPassword.setOnClickListener {
            val password = binding.etPassword.text.toString().trim()
            val confirmPassword = binding.etConfirmPassword.text.toString().trim()

            if (password.isEmpty()) {
                binding.etPassword.error = getString(R.string.enter_password)
                return@setOnClickListener
            }
            if (password.length < 4 || password.length > 8) {
                binding.etPassword.error = getString(R.string.pin_must_be_4_to_8_digits)
                return@setOnClickListener
            }
            if (confirmPassword.isEmpty()) {
                binding.etConfirmPassword.error = getString(R.string.confirm_password)
                return@setOnClickListener
            }
            if (password != confirmPassword) {
                binding.etConfirmPassword.error = getString(R.string.passwords_don_t_match)
                return@setOnClickListener
            }

            val intent = Intent(this, SetupSecurityQuestionActivity::class.java).apply {
                putExtra(SetupSecurityQuestionActivity.EXTRA_PASSWORD, password)
            }
            startActivity(intent)
        }
        binding.toolbar.setNavigationOnClickListener {
            finish()
        }
    }

    private fun setupChangePasswordListeners(binding2: ActivityChangePasswordBinding) {
        binding2.btnChangePassword.setOnClickListener {
            val oldPassword = binding2.etOldPassword.text.toString().trim()
            val newPassword = binding2.etNewPassword.text.toString().trim()
            if (oldPassword.isEmpty()) {
                binding2.etOldPassword.error = getString(R.string.this_field_can_t_be_empty)
                return@setOnClickListener
            }
            if (newPassword.isEmpty()) {
                binding2.etNewPassword.error = getString(R.string.this_field_can_t_be_empty)
                return@setOnClickListener
            }

            if (prefs.validatePassword(oldPassword)) {
                if (oldPassword != newPassword) {
                    prefs.savePassword(newPassword)
                    Toast.makeText(this, getString(R.string.password_reset_successfully), Toast.LENGTH_SHORT).show()
                    finish()
                } else {
                    Toast.makeText(this, getString(R.string.old_password_and_new_password_not_be_same), Toast.LENGTH_SHORT).show()
                    binding2.etNewPassword.error = getString(R.string.old_password_and_new_password_not_be_same)
                }
            } else {
                Toast.makeText(this, getString(R.string.wrong_password_entered), Toast.LENGTH_SHORT).show()
                binding2.etOldPassword.error = getString(R.string.old_password_not_matching)
            }
        }
        binding2.btnResetPassword.setOnClickListener {
            if (prefs.getSecurityQuestion() != null) {
                showSecurityQuestionDialog(prefs.getSecurityQuestion().toString())
            } else {
                Toast.makeText(this, getString(R.string.this_field_can_t_be_empty), Toast.LENGTH_SHORT).show()
            }
        }
        binding2.toolbar.setNavigationOnClickListener {
            finish()
        }
    }

    private fun showSecurityQuestionDialog(securityQuestion: String) {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_security_question, null)

        val questionTextView: TextView = dialogView.findViewById(R.id.security_question)
        questionTextView.text = securityQuestion
        val inputEditText = dialogView.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.text_input_edit_text)

        DialogUtil(this).showMaterialDialog(
            title = getString(R.string.answer_the_security_question),
            message = "",
            positiveButtonText = getString(R.string.verify),
            negativeButtonText = getString(R.string.cancel),
            view = dialogView,
            callback = object : DialogUtil.DialogCallback {
                override fun onPositiveButtonClicked() {
                    val userAnswer = inputEditText.text?.toString()?.trim() ?: ""

                    if (userAnswer.isEmpty()) {
                        Toast.makeText(this@SetupPasswordActivity, getString(R.string.answer_cannot_be_empty), Toast.LENGTH_SHORT).show()
                    } else {
                        if (prefs.validateSecurityAnswer(userAnswer)) {
                            prefs.resetPassword()
                            Toast.makeText(this@SetupPasswordActivity, getString(R.string.password_successfully_reset), Toast.LENGTH_SHORT).show()
                            val intent = Intent(this@SetupPasswordActivity, SetupPasswordActivity::class.java)
                            startActivity(intent)
                            finish()
                        } else {
                            Toast.makeText(this@SetupPasswordActivity, getString(R.string.invalid_answer), Toast.LENGTH_SHORT).show()
                        }
                    }
                }
                override fun onNegativeButtonClicked() {}
            }
        )
    }
}
