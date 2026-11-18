package com.devsorg.calenvault.vault.activities

import android.content.Intent
import android.os.Bundle
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.devsorg.calenvault.R
import com.devsorg.calenvault.databinding.ActivitySetupSecurityQuestionBinding
import com.devsorg.calenvault.vault.utils.PrefsUtil

class SetupSecurityQuestionActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySetupSecurityQuestionBinding
    private val prefs: PrefsUtil by lazy { PrefsUtil(this) }
    private var pendingPassword: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setupFlagSecure()
        enableEdgeToEdge()

        pendingPassword = intent.getStringExtra(EXTRA_PASSWORD)
        if (pendingPassword.isNullOrEmpty()) {
            finish()
            return
        }

        binding = ActivitySetupSecurityQuestionBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.main) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        setupToolbar()
        setupSecurityQuestions()
        setupListeners()
    }

    override fun onResume() {
        super.onResume()
        setupFlagSecure()
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

    private fun setupToolbar() {
        binding.toolbar.setNavigationOnClickListener {
            finish()
        }
    }

    private fun setupSecurityQuestions() {
        val questions = arrayOf(
            getString(R.string.what_is_your_pet_name),
            getString(R.string.what_is_your_birth_city),
            getString(R.string.what_is_your_favorite_book),
            getString(R.string.what_is_your_mother_maiden_name),
            getString(R.string.what_is_your_favorite_color),
            getString(R.string.custom_question)
        )
        val adapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, questions)
        binding.etSecurityQuestion.setAdapter(adapter)
        binding.etSecurityQuestion.inputType = android.text.InputType.TYPE_NULL

        binding.etSecurityQuestion.isFocusable = false
        binding.etSecurityQuestion.isFocusableInTouchMode = false
        binding.etSecurityQuestion.isCursorVisible = false
        binding.etSecurityQuestion.keyListener = null

        binding.etSecurityQuestion.setOnItemClickListener { _, _, position, _ ->
            if (questions[position] == getString(R.string.custom_question)) {
                binding.etSecurityQuestion.setText("")
                binding.etSecurityQuestion.isFocusable = true
                binding.etSecurityQuestion.isFocusableInTouchMode = true
                binding.etSecurityQuestion.isCursorVisible = true
                binding.etSecurityQuestion.keyListener = android.text.method.TextKeyListener.getInstance()
                binding.etSecurityQuestion.requestFocus()

                val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
                imm.showSoftInput(binding.etSecurityQuestion, InputMethodManager.SHOW_IMPLICIT)
            } else {
                binding.etSecurityQuestion.isFocusable = false
                binding.etSecurityQuestion.isFocusableInTouchMode = false
                binding.etSecurityQuestion.isCursorVisible = false
                binding.etSecurityQuestion.keyListener = null

                val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
                imm.hideSoftInputFromWindow(binding.etSecurityQuestion.windowToken, 0)
            }
        }
    }

    private fun setupListeners() {
        binding.btnNextSecurityQuestion.setOnClickListener {
            val question = binding.etSecurityQuestion.text.toString().trim()
            val answer = binding.etSecurityAnswer.text.toString().trim()

            if (question.isEmpty()) {
                binding.etSecurityQuestion.error = getString(R.string.enter_security_question)
                return@setOnClickListener
            }
            if (answer.isEmpty()) {
                binding.etSecurityAnswer.error = getString(R.string.enter_security_answer)
                return@setOnClickListener
            }

            val password = pendingPassword ?: return@setOnClickListener

            // Save password and security QA
            prefs.savePassword(password)
            prefs.saveSecurityQA(question, answer)
            Toast.makeText(this, getString(R.string.password_set_successfully), Toast.LENGTH_SHORT).show()

            // Navigate to Permission Screen
            val intent = Intent(this, PermissionActivity::class.java)
            startActivity(intent)
            finish()
        }
    }

    companion object {
        const val EXTRA_PASSWORD = "extra_password"
    }
}

