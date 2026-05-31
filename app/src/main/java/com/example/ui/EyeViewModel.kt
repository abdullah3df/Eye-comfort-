package com.example.ui

import android.app.Application
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.room.Room
import com.example.EyeBreakService
import com.example.data.EyeBreakLog
import com.example.data.EyeDatabase
import com.example.data.EyeExercise
import com.example.data.EyeRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface UiState {
    object Idle : UiState
    object Loading : UiState
    data class Success(val exercise: EyeExercise) : UiState
    data class Error(val message: String) : UiState
}

enum class Screen {
    Dashboard,
    Exercise,
    PostFeedback
}

enum class EyeStrain(val label: String) {
    None("None"),
    Light("Light"),
    Medium("Medium"),
    Severe("Severe")
}

class EyeViewModel(application: Application) : AndroidViewModel(application) {

    private val db = Room.databaseBuilder(
        application,
        EyeDatabase::class.java,
        "eye_comfort_db"
    ).build()

    private val repository = EyeRepository(db.eyeBreakLogDao())

    private val prefs = application.getSharedPreferences("eye_relief_prefs", Context.MODE_PRIVATE)

    // Language code of choice. Default is English
    private val _selectedLanguage = MutableStateFlow("en")
    val selectedLanguage: StateFlow<String> = _selectedLanguage.asStateFlow()

    // Offline / Local catalog exercise configuration mode
    private val _isOfflineMode = MutableStateFlow(false)
    val isOfflineMode: StateFlow<Boolean> = _isOfflineMode.asStateFlow()

    // Current navigation screen
    private val _currentScreen = MutableStateFlow(Screen.Dashboard)
    val currentScreen: StateFlow<Screen> = _currentScreen.asStateFlow()

    // Gemini network fetch status
    private val _uiState = MutableStateFlow<UiState>(UiState.Idle)
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    // Session stats & historic logger
    val historyLogs: StateFlow<List<EyeBreakLog>> = repository.allLogs.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // Current assessed baseline strain
    private val _preStrainLevel = MutableStateFlow(EyeStrain.Medium)
    val preStrainLevel: StateFlow<EyeStrain> = _preStrainLevel.asStateFlow()

    // Post-exercise logged strain level
    private val _postStrainLevel = MutableStateFlow(EyeStrain.None)
    val postStrainLevel: StateFlow<EyeStrain> = _postStrainLevel.asStateFlow()

    // --- Active Exercise Timer State ---
    private val _timeLeft = MutableStateFlow(60)
    val timeLeft: StateFlow<Int> = _timeLeft.asStateFlow()

    private val _totalDuration = MutableStateFlow(60)
    val totalDuration: StateFlow<Int> = _totalDuration.asStateFlow()

    private val _isTimerRunning = MutableStateFlow(false)
    val isTimerRunning: StateFlow<Boolean> = _isTimerRunning.asStateFlow()

    // Pulsating indicator value for visual exercise breath guidelines (0f..1f)
    private val _visualOrbScale = MutableStateFlow(1f)
    val visualOrbScale: StateFlow<Float> = _visualOrbScale.asStateFlow()

    private var timerJob: Job? = null
    private var animationJob: Job? = null
    private var currentExercise: EyeExercise? = null

    init {
        // Load settings from persistent storage and sync with service utils
        val savedLang = prefs.getString("lang_code", "en") ?: "en"
        _selectedLanguage.value = savedLang
        EyeBreakService.setLanguage(application, savedLang)

        val savedOffline = prefs.getBoolean("offline_mode", false)
        _isOfflineMode.value = savedOffline

        startOrbAnimationLoop()
    }

    fun setLanguage(code: String) {
        _selectedLanguage.value = code
        prefs.edit().putString("lang_code", code).apply()
        EyeBreakService.setLanguage(getApplication(), code)
    }

    fun setOfflineMode(enabled: Boolean) {
        _isOfflineMode.value = enabled
        prefs.edit().putBoolean("offline_mode", enabled).apply()
    }

    fun startEyeProtection() {
        val context = getApplication<Application>()
        val intent = Intent(context, EyeBreakService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(intent)
        } else {
            context.startService(intent)
        }
    }

    fun stopEyeProtection() {
        val context = getApplication<Application>()
        val intent = Intent(context, EyeBreakService::class.java)
        context.stopService(intent)
    }

    fun updateServiceInterval(mins: Int) {
        val context = getApplication<Application>()
        EyeBreakService.setIntervalMinutes(context, mins)
    }

    fun setPreStrain(level: EyeStrain) {
        _preStrainLevel.value = level
        // Pre-fill post-strain with an improved level as a friendly default suggestion
        _postStrainLevel.value = when (level) {
            EyeStrain.None -> EyeStrain.None
            EyeStrain.Light -> EyeStrain.None
            EyeStrain.Medium -> EyeStrain.Light
            EyeStrain.Severe -> EyeStrain.Medium
        }
    }

    fun setPostStrain(level: EyeStrain) {
        _postStrainLevel.value = level
    }

    // Trigger Screen Break Session
    fun startScreenBreak() {
        viewModelScope.launch {
            _uiState.value = UiState.Loading
            _currentScreen.value = Screen.Exercise
            
            try {
                // Fetch localized exercise from Gemini API or immediately from local if offline-only mode
                val exercise = if (_isOfflineMode.value) {
                    repository.fetchLocalExercise(_selectedLanguage.value)
                } else {
                    repository.fetchEyeExercise(_selectedLanguage.value)
                }
                currentExercise = exercise
                _timeLeft.value = exercise.duration_seconds
                _totalDuration.value = exercise.duration_seconds
                _uiState.value = UiState.Success(exercise)
                startTimer()
            } catch (e: Exception) {
                // Instantly query local catalog in case of absolute failure to show immediate relief
                val localExercise = repository.fetchLocalExercise(_selectedLanguage.value)
                currentExercise = localExercise
                _timeLeft.value = localExercise.duration_seconds
                _totalDuration.value = localExercise.duration_seconds
                _uiState.value = UiState.Success(localExercise)
                startTimer()
            }
        }
    }

    // Timer control functions
    fun toggleTimer() {
        if (_isTimerRunning.value) {
            pauseTimer()
        } else {
            startTimer()
        }
    }

    private fun startTimer() {
        _isTimerRunning.value = true
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (_timeLeft.value > 0) {
                delay(1000)
                if (_isTimerRunning.value) {
                    _timeLeft.value -= 1
                }
            }
            _isTimerRunning.value = false
            // Autocomplete to rating screen when timer ends
            _currentScreen.value = Screen.PostFeedback
        }
    }

    fun pauseTimer() {
        _isTimerRunning.value = false
        timerJob?.cancel()
    }

    fun skipOrEndExercise() {
        pauseTimer()
        _currentScreen.value = Screen.PostFeedback
    }

    // Action to confirm post-feedback rating and save in SQLite history
    fun submitPostBreakFeedback() {
        viewModelScope.launch {
            val exercise = currentExercise
            if (exercise != null) {
                repository.saveCompletedBreak(
                    exercise = exercise,
                    langCode = _selectedLanguage.value,
                    preStrain = _preStrainLevel.value.name,
                    postStrain = _postStrainLevel.value.name
                )
            }
            resetToDashboard()
        }
    }

    fun resetToDashboard() {
        pauseTimer()
        currentExercise = null
        _uiState.value = UiState.Idle
        _currentScreen.value = Screen.Dashboard
    }

    fun clearLogHistory() {
        viewModelScope.launch {
            repository.clearHistory()
        }
    }

    // Cozy constant breathing oscillation to guide eyes gently
    private fun startOrbAnimationLoop() {
        animationJob?.cancel()
        animationJob = viewModelScope.launch {
            var growing = true
            while (true) {
                delay(40) // ~25 FPS for smooth light resource usage
                if (_isTimerRunning.value) {
                    val currentVal = _visualOrbScale.value
                    if (growing) {
                        _visualOrbScale.value = (currentVal + 0.015f).coerceAtMost(1.3f)
                        if (_visualOrbScale.value >= 1.3f) growing = false
                    } else {
                        _visualOrbScale.value = (currentVal - 0.015f).coerceAtLeast(0.7f)
                        if (_visualOrbScale.value <= 0.7f) growing = true
                    }
                } else {
                    // Relax rest scale state
                    _visualOrbScale.value = 1.0f
                }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        timerJob?.cancel()
        animationJob?.cancel()
        db.close()
    }
}
