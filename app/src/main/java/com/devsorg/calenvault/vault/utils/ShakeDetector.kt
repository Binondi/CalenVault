package com.devsorg.calenvault.vault.utils

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import kotlin.math.sqrt

class ShakeDetector(
    context: Context,
    private val onShake: () -> Unit
) : SensorEventListener {

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
    private val accelerometer = sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

    private var shakeTimestamp: Long = 0
    private var shakeCount = 0
    private var sensitivityLevel = 3
    private var isListening = false

    fun setSensitivity(level: Int) {
        sensitivityLevel = level.coerceIn(1, 5)
    }

    fun start() {
        if (!isListening && accelerometer != null) {
            isListening = sensorManager?.registerListener(
                this,
                accelerometer,
                SensorManager.SENSOR_DELAY_UI
            ) ?: false
        }
    }

    fun stop() {
        if (isListening) {
            sensorManager?.unregisterListener(this)
            isListening = false
        }
        shakeCount = 0
        shakeTimestamp = 0
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event == null || event.sensor.type != Sensor.TYPE_ACCELEROMETER) return

        val x = event.values[0]
        val y = event.values[1]
        val z = event.values[2]

        val gX = x / SensorManager.GRAVITY_EARTH
        val gY = y / SensorManager.GRAVITY_EARTH
        val gZ = z / SensorManager.GRAVITY_EARTH

        val gForce = sqrt((gX * gX + gY * gY + gZ * gZ).toDouble()).toFloat()

        val threshold = when (sensitivityLevel) {
            1 -> 2.7f
            2 -> 2.3f
            3 -> 1.9f
            4 -> 1.6f
            5 -> 1.35f
            else -> 1.9f
        }

        val now = System.currentTimeMillis()

        if (gForce > threshold) {
            if (shakeTimestamp + 500 > now) {
                shakeCount++
                if (shakeCount >= 2) {
                    if (now - lastTriggerTime > 1500) {
                        lastTriggerTime = now
                        shakeCount = 0
                        onShake()
                    }
                }
            } else {
                shakeCount = 1
            }
            shakeTimestamp = now
        } else if (shakeTimestamp + 800 < now) {
            shakeCount = 0
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    companion object {
        private var lastTriggerTime: Long = 0

        fun vibrate(context: Context) {
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                    vibratorManager?.defaultVibrator?.vibrate(
                        VibrationEffect.createOneShot(120, VibrationEffect.DEFAULT_AMPLITUDE)
                    )
                } else {
                    @Suppress("DEPRECATION")
                    val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        vibrator?.vibrate(
                            VibrationEffect.createOneShot(120, VibrationEffect.DEFAULT_AMPLITUDE)
                        )
                    } else {
                        @Suppress("DEPRECATION")
                        vibrator?.vibrate(120)
                    }
                }
            } catch (_: Exception) {}
        }
    }
}


