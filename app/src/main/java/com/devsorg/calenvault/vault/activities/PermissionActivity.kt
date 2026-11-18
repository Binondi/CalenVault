package com.devsorg.calenvault.vault.activities

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.WindowManager
import androidx.activity.OnBackPressedCallback
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.devsorg.calenvault.CalenVaultApp
import com.devsorg.calenvault.databinding.ActivityPermissionBinding
import com.devsorg.calenvault.vault.utils.PrefsUtil
import com.devsorg.calenvault.vault.utils.StoragePermissionUtil

class PermissionActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPermissionBinding
    private val prefs: PrefsUtil by lazy { PrefsUtil(this) }
    private val storagePermissionUtil: StoragePermissionUtil by lazy { StoragePermissionUtil(this) }

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        storagePermissionUtil.handlePermissionResult(permissions)
        updatePermissionUi(storagePermissionUtil.hasStoragePermission())
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setupFlagSecure()
        enableEdgeToEdge()

        binding = ActivityPermissionBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.permissionRoot) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        binding.toolbar.navigationIcon = null

        setupListeners()
        updatePermissionUi(storagePermissionUtil.hasStoragePermission())

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                finish()
            }
        })
    }

    override fun onResume() {
        super.onResume()
        setupFlagSecure()
        updatePermissionUi(storagePermissionUtil.hasStoragePermission())
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

    private fun updatePermissionUi(isGranted: Boolean) {
        if (isGranted) {
            binding.layoutPermissionNeeded.visibility = View.GONE
            binding.layoutPermissionGranted.visibility = View.VISIBLE
        } else {
            binding.layoutPermissionNeeded.visibility = View.VISIBLE
            binding.layoutPermissionGranted.visibility = View.GONE
        }
    }

    private fun setupListeners() {
        binding.btnGrantPermission.setOnClickListener {
            storagePermissionUtil.requestStoragePermission(permissionLauncher) {
                updatePermissionUi(true)
            }
        }

        binding.btnFinish.setOnClickListener {
            val intent = Intent(this, com.devsorg.calenvault.MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
            startActivity(intent)
            finish()
        }
    }
}

