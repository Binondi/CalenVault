package com.devsorg.calenvault.vault.activities

import android.content.Intent
import android.os.Bundle
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import com.devsorg.calenvault.CalenVaultApp
import com.devsorg.calenvault.MainActivity
import com.devsorg.calenvault.vault.utils.PrefsUtil
import com.devsorg.calenvault.vault.utils.ShakeDetector

abstract class BaseVaultActivity : AppCompatActivity() {

    protected val prefs: PrefsUtil by lazy { PrefsUtil(this) }
    private var shakeDetector: ShakeDetector? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setupFlagSecure()
    }

    override fun onResume() {
        super.onResume()
        setupFlagSecure()
        setupShakeDetector()
        val app = application as? CalenVaultApp
        if (app != null && !app.isVaultSessionActive && prefs.getBoolean("is_vault_enabled", true)) {
            val intent = Intent(this, MainActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            startActivity(intent)
            finish()
        }
    }

    override fun onPause() {
        super.onPause()
        shakeDetector?.stop()
    }

    private fun setupShakeDetector() {
        if (prefs.isShakeToCloseEnabled()) {
            if (shakeDetector == null) {
                shakeDetector = ShakeDetector(this) {
                    onShakeDetected()
                }
            }
            shakeDetector?.setSensitivity(prefs.getShakeSensitivity())
            shakeDetector?.start()
        } else {
            shakeDetector?.stop()
        }
    }

    private fun onShakeDetected() {
        if (!isFinishing && !isDestroyed) {
            if (prefs.isShakeVibrateEnabled()) {
                ShakeDetector.vibrate(this)
            }
            val app = application as? CalenVaultApp
            app?.isVaultSessionActive = false
            val intent = Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
            startActivity(intent)
            finishAffinity()
        }
    }

    protected fun setupFlagSecure() {
        if (prefs.getBoolean("screenshot_restriction", true)) {
            window.setFlags(
                WindowManager.LayoutParams.FLAG_SECURE,
                WindowManager.LayoutParams.FLAG_SECURE
            )
        } else {
            window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
        }
    }
}


