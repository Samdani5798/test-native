package com.aistudio.imported.test

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.webkit.JavascriptInterface
import android.widget.Toast

class WebAppNativeBridge(
    private val context: Context,
    private val onTakePictureRequested: () -> Unit
) {
    @JavascriptInterface
    fun showToast(message: String) {
        Toast.makeText(context, message, Toast.LENGTH_LONG).show()
    }

    @JavascriptInterface
    fun vibrate(durationMs: Long) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
                vibratorManager.defaultVibrator.vibrate(VibrationEffect.createOneShot(durationMs, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
                vibrator.vibrate(VibrationEffect.createOneShot(durationMs, VibrationEffect.DEFAULT_AMPLITUDE))
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    @JavascriptInterface
    fun fetchLocation(): String {
        return """{"latitude": 37.7749, "longitude": -122.4194, "accuracy": 15.0, "timestamp": ${System.currentTimeMillis()}}"""
    }

    @JavascriptInterface
    fun scheduleNotification(id: Int, title: String, message: String, delayMs: Long) {
        try {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val intent = Intent(context, NotificationReceiver::class.java).apply {
                putExtra("id", id)
                putExtra("title", title)
                putExtra("message", message)
            }

            val pendingIntent = PendingIntent.getBroadcast(
                context,
                id,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val triggerAtMillis = System.currentTimeMillis() + delayMs
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
            } else {
                alarmManager.setExact(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    @JavascriptInterface
    fun takePicture() {
        onTakePictureRequested()
    }

    @JavascriptInterface
    fun playAudio(soundType: String) {
        try {
            val toneType = when (soundType.lowercase()) {
                "bell", "beep" -> ToneGenerator.TONE_CDMA_PIP
                "alert" -> ToneGenerator.TONE_PROP_BEEP2
                "success" -> ToneGenerator.TONE_CDMA_CONFIRM
                else -> ToneGenerator.TONE_PROP_BEEP
            }
            val toneGenerator = ToneGenerator(AudioManager.STREAM_MUSIC, 100)
            toneGenerator.startTone(toneType, 300)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
