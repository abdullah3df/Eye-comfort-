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
    private var screenOffTimestamp: Long = 0L

    companion object {
        private const val CHANNEL_ID_SILENT = "eye_break_silent_channel"
        private const val CHANNEL_ID_ALARM = "eye_break_alarm_channel"
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
                        if (screenOffTimestamp > 0L) {
                            val timeOffline = System.currentTimeMillis() - screenOffTimestamp
                            if (timeOffline >= 60000L) {
                                // 1-minute rest rule satisfied: reset timer completely
                                _screenOnTimeSeconds.value = 0L
                                _isAlertTriggered.value = false
                            }
                        }
                        resumeTicker()
                        updateNotification(_isAlertTriggered.value)
                    }
                    Intent.ACTION_SCREEN_OFF -> {
                        isScreenOn = false
                        screenOffTimestamp = System.currentTimeMillis()
                        pauseTicker()
                        updateNotification(_isAlertTriggered.value)
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

        // Subscribe to alert state changes internally to update notification exactly once on state transition
        serviceScope.launch {
            _isAlertTriggered.collect { triggered ->
                if (_isRunning.value) {
                    updateNotification(triggered)
                }
            }
        }

        resumeTicker()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action
        if (action == "STOP") {
            stopSelf()
            return START_NOT_STICKY
        }
        try {
            createNotificationChannel()
            // Immediately start foreground service using the correct state's notification
            val initialNotification = if (_isAlertTriggered.value) buildAlertNotification() else buildStatusNotification()
            startForeground(NOTIFICATION_ID, initialNotification)
            android.util.Log.d("EyeBreakService", "Service started successfully")
        } catch (e: Exception) {
            android.util.Log.e("EyeBreakService", "Error: failed to start", e)
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
                        if (!_isAlertTriggered.value) {
                            _isAlertTriggered.value = true
                        }
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
        
        val title = if (lang == "ar") {
            if (isScreenOn) "حماية العين نشطة" else "حماية العين متوقفة مؤقتاً"
        } else {
            if (isScreenOn) "Eye Protection Active" else "Eye Protection Paused"
        }
        
        val statusMessage = if (lang == "ar") {
            if (isScreenOn) {
                "حماية العين نشطة 👁 - جاري مراقبة وقت الشاشة."
            } else {
                "تم إيقاف المراقب مؤقتاً لأن الشاشة مغلقة."
            }
        } else {
            if (isScreenOn) {
                "Eye Protection Active 👁 - Monitoring your screen time."
            } else {
                "Monitoring paused while screen is off."
            }
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

        return NotificationCompat.Builder(this, CHANNEL_ID_SILENT)
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
            "Time's up! 👁"
        }
        
        val alertMessage = if (lang == "ar") {
            "انتهى الوقت! ابتعد عن شاشة الهاتف وأرح عينيك لمدة 20 ثانية 👁"
        } else {
            "Time's up! Look away from your phone and rest your eyes for 20 seconds 👁"
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

        return NotificationCompat.Builder(this, CHANNEL_ID_ALARM)
            .setContentTitle(alertTitle)
            .setContentText(alertMessage)
            .setStyle(NotificationCompat.BigTextStyle().bigText(alertMessage))
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setOngoing(true)
            .setVibrate(longArrayOf(0, 500, 200, 500))
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setContentIntent(pendingIntent)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            
            // CHANNEL 1: Silent & Persistent (Importance Low)
            val silentChannel = NotificationChannel(
                CHANNEL_ID_SILENT,
                "Eye Safety Background Monitor",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows the active screen tracking status silently in the background."
                enableVibration(false)
                setSound(null, null)
            }
            notificationManager.createNotificationChannel(silentChannel)

            // CHANNEL 2: Alert & Loud (Importance High)
            val alarmChannel = NotificationChannel(
                CHANNEL_ID_ALARM,
                "Eye Break Alerts",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Triggers high-priority eye safety alarms and rest alerts."
                enableVibration(true)
                enableLights(true)
            }
            notificationManager.createNotificationChannel(alarmChannel)
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
