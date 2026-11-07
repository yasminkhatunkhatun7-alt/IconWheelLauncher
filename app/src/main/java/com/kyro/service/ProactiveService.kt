package com.kyro.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Log
import androidx.core.app.NotificationCompat
import com.kyro.AppModule
import com.kyro.KyroApplication
import java.util.Calendar

class ProactiveService : Service() {

    private val handler = Handler(Looper.getMainLooper())
    private lateinit var appModule: AppModule
    private val checkIntervalMs = 15 * 60 * 1000L // 15 mins

    private val proactiveRunnable = object : Runnable {
        override fun run() {
            try {
                checkBatteryAndTime()
            } catch (e: Exception) {
                Log.e("KYRO_PROACTIVE", "Proactive check failed", e)
            } finally {
                handler.postDelayed(this, checkIntervalMs)
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        appModule = (application as KyroApplication).appModule
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val notification = NotificationCompat.Builder(this, "kyro_proactive")
            .setContentTitle("KYRO Proactive Monitoring")
            .setContentText("Monitoring system vitals and schedule.")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .build()

        try { startForeground(1, notification) } catch (e: Exception) { Log.e("KYRO_PROACTIVE", "startForeground failed", e) }

        handler.post(proactiveRunnable)

        return START_STICKY
    }

    private fun checkBatteryAndTime() {
        // Feature 14: Proactive Assistant Battery & Time checks
        val batteryStatus: Intent? = IntentFilter(Intent.ACTION_BATTERY_CHANGED).let { ifilter ->
            applicationContext.registerReceiver(null, ifilter)
        }

        val level: Int = batteryStatus?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale: Int = batteryStatus?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1

        if (level != -1 && scale != -1) {
            val batteryPct = level * 100 / scale

            if (batteryPct < 15) {
                appModule.ttsEngine.speak("Sir, battery is low.")
            } else if (batteryPct == 100) {
                appModule.ttsEngine.speak("Battery is full. Please unplug the charger.")
            }
        }

        val calendar = Calendar.getInstance()
        val hour = calendar.get(Calendar.HOUR_OF_DAY)
        if (hour >= 23 || hour < 4) { // 11 PM to 4 AM
            appModule.ttsEngine.speak("It's quite late Sir. You should sleep now.")
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val serviceChannel = NotificationChannel(
                "kyro_proactive",
                "Proactive Service Channel",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(serviceChannel)
        }
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacksAndMessages(null)
    }
}
