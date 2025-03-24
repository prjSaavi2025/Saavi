package com.saavi.saavi.ui

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.os.Handler
import android.os.Looper
import android.util.Log

class VolumeButtonReceiver : BroadcastReceiver() {

    companion object {
        private var pressCount = 0
        private var lastPressTime = 0L
        private const val RESET_TIME_MS = 5000  // Reset counter after 5 seconds
        private const val PRESS_THRESHOLD = 1500 // Max time gap between presses (1.5 sec)
        private const val REQUIRED_PRESSES = 5

        private var lastVolumeLevel = -1 // Stores previous volume level
        private val handler = Handler(Looper.getMainLooper())
        private val resetCounterRunnable = Runnable {
            Log.d("VolumeReceiver", "Resetting counter to 0")
            pressCount = 0
        }
    }

    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action == "android.media.VOLUME_CHANGED_ACTION") {
            val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
            val currentVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)

            // Ignore duplicate triggers if volume hasn't changed
            if (currentVolume == lastVolumeLevel) {
                return
            }
            lastVolumeLevel = currentVolume // Update last volume

            val currentTime = System.currentTimeMillis()

            // If pressed within the allowed time, count it
            if (currentTime - lastPressTime <= PRESS_THRESHOLD) {
                pressCount++
            } else {
                pressCount = 1 // Reset count if gap too long
            }

            lastPressTime = currentTime
            Log.d("VolumeReceiver", "Volume Button Pressed $pressCount times")

            // Check if required presses have been reached
            if (pressCount >= REQUIRED_PRESSES) {
                launchApp(context)
                pressCount = 0 // Reset after launching
                handler.removeCallbacks(resetCounterRunnable)
                return
            }

            // Schedule a reset after 5 sec
            handler.removeCallbacks(resetCounterRunnable)
            handler.postDelayed(resetCounterRunnable, RESET_TIME_MS.toLong())
        }
    }

    private fun launchApp(context: Context) {
        val intent = context.packageManager.getLaunchIntentForPackage(context.packageName)
        if (intent != null) {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
            Log.d("VolumeReceiver", "App Launched!")
        } else {
            Log.e("VolumeReceiver", "Failed to launch app! Launch intent is null.")
        }
    }
}
