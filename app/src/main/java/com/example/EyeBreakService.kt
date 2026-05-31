package com.example

import android.app.*
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.example.ui.Localization
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class EyeBreakService : Service() {

    private val serviceJob = SupervisorJob()
    private val serviceScope = CoroutineScope(Dispatchers.Main + serviceJob)
    private var tickerJob: Job? = null

    private var isScreenOn = true
    private var screenReceiver: BroadcastReceiver? = null

    companion object {
        private const val CHANNEL_ID = "eye_break_monitoring_channel"
        private const val NOTIFICATION_ID = 8801

        // Static reactive flows readable by components in the same process
        private val _isRunning = MutableStateFlow(false)
        val isRunning = _isRunning.asStateFlow()

        private val _screenOnTimeSeconds = MutableStateFlow(0L)
        val screenOnTimeSeconds = _screenOnTimeSeconds.asStateFlow()

        private val _selectedIntervalMinutes = MutableStateFlow(20) // default 20 mins
        val selectedIntervalMinutes = _selectedIntervalMinutes.asStateFlow()

        private val _isAlertTriggered = MutableStateFlow(false)
        val isAlertTriggered = _isAlertTriggered.asStateFlow()

        fun setLanguage(context: Context, langCode: String) {
            val prefs = context.getSharedPreferences("eye_relief_prefs", Context.MODE_PRIVATE)
            prefs.edit().putString("lang_code", langCode).apply()
        }

        fun getLanguage(context: Context): String {
            val prefs = context.getSharedPreferences("eye_relief_prefs", Context.MODE_PRIVATE)
            return prefs.getString("lang_code", "en") ?: "en"
        }

        fun setIntervalMinutes(context: Context, mins: Int) {
            val prefs = context.getSharedPreferences("eye_relief_prefs", Context.MODE_PRIVATE)
            prefs.edit().putInt("interval_mins", mins).apply()
            _selectedIntervalMinutes.value = mins
        }

        fun getIntervalMinutes(context: Context): Int {
            val prefs = context.getSharedPreferences("eye_relief_prefs", Context.MODE_PRIVATE)
            val mins = prefs.getInt("interval_mins", 20)
            _selectedIntervalMinutes.value = mins
            return mins
        }

        fun resetAlert() {
            _isAlertTriggered.value = false
            _screenOnTimeSeconds.value = 0L
        }
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        getIntervalMinutes(this) // initialize state

        // Dynamically register BroadcastReceiver for screen actions
        screenReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                when (intent?.action) {
                    Intent.ACTION_SCREEN_ON -> {
                        isScreenOn = true
                        resumeTicker()
                    }
                    Intent.ACTION_SCREEN_OFF -> {
                        isScreenOn = false
                        pauseTicker()
                    }
                }
            }
        }

        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_SCREEN_ON)
            addAction(Intent.ACTION_SCREEN_OFF)
        }
        registerReceiver(screenReceiver, filter)

        _isRunning.value = true
        startForeground(NOTIFICATION_ID, buildStatusNotification())
        resumeTicker()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action
        if (action == "STOP") {
            stopSelf()
            return START_NOT_STICKY
        }
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun resumeTicker() {
        if (tickerJob?.isActive == true) return
        tickerJob = serviceScope.launch {
            while (isActive) {
                delay(1000)
                if (isScreenOn) {
                    _screenOnTimeSeconds.value++
                    val targetSeconds = _selectedIntervalMinutes.value * 60L
                    if (_screenOnTimeSeconds.value >= targetSeconds) {
                        _isAlertTriggered.value = true
                        updateNotification(isAlert = true)
                    } else {
                        updateNotification(isAlert = false)
                    }
                }
            }
        }
    }

    private fun pauseTicker() {
        tickerJob?.cancel()
    }

    private fun updateNotification(isAlert: Boolean) {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NOTIFICATION_ID, if (isAlert) buildAlertNotification() else buildStatusNotification())
    }

    private fun buildStatusNotification(): Notification {
        val lang = getLanguage(this)
        val elapsedMins = _screenOnTimeSeconds.value / 60
        val elapsedSecs = _screenOnTimeSeconds.value % 60

        val title = Localization.get(lang, "app_title")
        val statusLabelTemplate = if (lang == "ar") {
            "حماية العين الذكية مفعلة 🟢\nزمن استخدام الشاشة المستمر: %02d:%02d دقيقة"
        } else {
            "Continuous active eye strain defense 🟢\nActive screen time: %02d:%02d mins"
        }
        val statusMessage = statusLabelTemplate.format(elapsedMins, elapsedSecs)

        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("SHOW_EXERCISE", true)
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val stopIntent = Intent(this, EyeBreakService::class.java).apply {
            action = "STOP"
        }
        val stopPendingIntent = PendingIntent.getService(
            this,
            1,
            stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val stopText = if (lang == "ar") "إيقاف الحماية" else "Stop Protection"

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(statusMessage)
            .setStyle(NotificationCompat.BigTextStyle().bigText(statusMessage))
            .setSmallIcon(android.R.drawable.ic_menu_compass)
            .setOngoing(true)
            .setSilent(true)
            .setContentIntent(pendingIntent)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, stopText, stopPendingIntent)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun buildAlertNotification(): Notification {
        val lang = getLanguage(this)
        
        val alertTitle = if (lang == "ar") {
            "حان وقت راحة العين! 👁"
        } else {
            "Time to rest your eyes! 👁"
        }
        
        val alertMessage = if (lang == "ar") {
            "دَع هاتفك وانظر بعيداً الآن. اضغط هنا لبدء تمارين الاسترخاء الذكية."
        } else {
            "Leave your phone and look away. Tap here to start localized eye relief exercises."
        }

        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("SHOW_EXERCISE", true)
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(alertTitle)
            .setContentText(alertMessage)
            .setStyle(NotificationCompat.BigTextStyle().bigText(alertMessage))
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setOngoing(true)
            .setVibrate(longArrayOf(0, 500, 200, 500))
            .setContentIntent(pendingIntent)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Continuous Eye Safety Monitor"
            val descriptionText = "Tracks continuous screen usage and sends ophthalmologist tips on screen breaks."
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
                enableVibration(true)
            }
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    override fun onDestroy() {
        _isRunning.value = false
        pauseTicker()
        serviceJob.cancel()
        screenReceiver?.let {
            try {
                unregisterReceiver(it)
            } catch (e: Exception) {
                // ignore
            }
        }
        super.onDestroy()
    }
}
