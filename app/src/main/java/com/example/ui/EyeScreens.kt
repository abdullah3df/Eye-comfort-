package com.example.ui

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.EyeBreakService
import com.example.data.EyeBreakLog
import com.example.data.EyeExercise
import java.text.SimpleDateFormat
import java.util.*

// Formats EyeExercise into pretty-print JSON metadata
fun EyeExercise.toJsonString(): String {
    return """{
  "exercise_title": "$exercise_title",
  "steps": "$steps",
  "duration_seconds": $duration_seconds,
  "benefit": "$benefit"
}"""
}

@Composable
fun EyeBreakApp(viewModel: EyeViewModel) {
    val currentScreen by viewModel.currentScreen.collectAsState()
    val selectedLanguage by viewModel.selectedLanguage.collectAsState()

    // Dynamically adjust layout direction to respect native RTL for Arabic
    val layoutDirection = if (selectedLanguage == "ar") LayoutDirection.Rtl else LayoutDirection.Ltr

    CompositionLocalProvider(LocalLayoutDirection provides layoutDirection) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            AnimatedContent(
                targetState = currentScreen,
                transitionSpec = {
                    fadeIn() + slideInVertically { it / 3 } togetherWith
                            fadeOut() + slideOutVertically { -it / 3 }
                },
                label = "ScreenTransition"
            ) { screen ->
                when (screen) {
                    Screen.Dashboard -> DashboardScreen(viewModel)
                    Screen.Exercise -> ExerciseScreen(viewModel)
                    Screen.PostFeedback -> PostFeedbackScreen(viewModel)
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(viewModel: EyeViewModel) {
    val context = LocalContext.current
    val selectedLanguage by viewModel.selectedLanguage.collectAsState()
    val isOfflineMode by viewModel.isOfflineMode.collectAsState()
    val preStrain by viewModel.preStrainLevel.collectAsState()
    val historyLogs by viewModel.historyLogs.collectAsState()

    // Read reactive flows directly from Foreground Service
    val isServiceRunning by EyeBreakService.isRunning.collectAsState()
    val screenOnSeconds by EyeBreakService.screenOnTimeSeconds.collectAsState()
    val selectedIntervalMinutes by EyeBreakService.selectedIntervalMinutes.collectAsState()

    // Aggregate statistics from local Room store
    val totalBreaks = historyLogs.size
    val totalSecondsRelaxed = historyLogs.sumOf { it.durationSeconds }
    val improvementCount = historyLogs.count { log ->
        val pre = getStrainWeight(log.preStrainLevel)
        val post = getStrainWeight(log.postStrainLevel)
        post < pre
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Favorite,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = Localization.get(selectedLanguage, "app_title"),
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleLarge
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = Color.Transparent
                )
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 20.dp, vertical = 6.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // MAIN COMPACT SERVICE CONTROLLER
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(28.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isServiceRunning) {
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.65f)
                        } else {
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        }
                    ),
                    border = BorderStroke(
                        width = 1.dp,
                        color = if (isServiceRunning) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = if (selectedLanguage == "ar") "درع حماية النظر الذكي" else "Optical Break Shield",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = if (isServiceRunning) {
                                if (selectedLanguage == "ar") "حالة الحماية: نشطة بالخلفية 🟢" else "Protection Status: ACTIVE IN BACKGROUND 🟢"
                            } else {
                                if (selectedLanguage == "ar") "حالة الحماية: معطلة 🔴" else "Protection Status: INACTIVE 🔴"
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (isServiceRunning) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                        )

                        Spacer(modifier = Modifier.height(20.dp))

                        // SINGLE MASTER START/STOP BUTTON
                        Button(
                            onClick = {
                                if (isServiceRunning) {
                                    viewModel.stopEyeProtection()
                                } else {
                                    viewModel.startEyeProtection()
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(60.dp)
                                .testTag("protection_toggle_button"),
                            shape = CircleShape,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isServiceRunning) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                            ),
                            elevation = ButtonDefaults.buttonElevation(defaultElevation = 6.dp)
                        ) {
                            Icon(
                                imageVector = if (isServiceRunning) Icons.Default.Close else Icons.Default.PlayArrow,
                                contentDescription = null,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = if (isServiceRunning) {
                                    if (selectedLanguage == "ar") "إلغاء التفعيل والبرنامج" else "Deactivate Eye Protection"
                                } else {
                                    if (selectedLanguage == "ar") "تفعيل وحماية العين" else "Activate Eye Protection"
                                },
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        // REALTIME STATS TRACKER OVERLAY
                        if (isServiceRunning) {
                            Spacer(modifier = Modifier.height(24.dp))
                            val targetSeconds = selectedIntervalMinutes * 60L
                            val progress = if (targetSeconds > 0) (screenOnSeconds.toFloat() / targetSeconds).coerceIn(0f, 1f) else 0f
                            
                            val elapsedMins = screenOnSeconds / 60
                            val elapsedSecs = screenOnSeconds % 60

                            Column(modifier = Modifier.fillMaxWidth()) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = if (selectedLanguage == "ar") "مؤقت الاستخدام المستمر:" else "Continuous Screen-On Time:",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                    )
                                    Text(
                                        text = "$elapsedMins:%02d / $selectedIntervalMinutes:00".format(elapsedSecs),
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                LinearProgressIndicator(
                                    progress = { progress },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(10.dp)
                                        .clip(CircleShape),
                                    color = MaterialTheme.colorScheme.primary,
                                    trackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
                                )
                                Spacer(modifier = Modifier.height(14.dp))
                                // Quick break override button
                                OutlinedButton(
                                    onClick = { viewModel.startScreenBreak() },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = CircleShape
                                ) {
                                    Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = if (selectedLanguage == "ar") "أخذ استراحة يدوية فورية" else "Take Quick Manual Break",
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // DYNAMIC INLINE CONFIGURATION BOX
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(24.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.12f)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            text = if (selectedLanguage == "ar") "لوحة ضبط التفضيلات والخيارات" else "Settings & Configurations",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        // 1. SELECTABLE LANGUAGE PILLS
                        Column {
                            Text(
                                text = Localization.get(selectedLanguage, "target_lang"),
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            FlowRow(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                val languages = listOf(
                                    "en" to "🇺🇸 EN",
                                    "ar" to "🇸🇦 AR",
                                    "fr" to "🇫🇷 FR",
                                    "de" to "🇩🇪 DE",
                                    "es" to "🇪🇸 ES"
                                )
                                languages.forEach { (code, label) ->
                                    val isSelected = selectedLanguage == code
                                    FilterChip(
                                        selected = isSelected,
                                        onClick = { viewModel.setLanguage(code) },
                                        label = { Text(label, fontWeight = FontWeight.Bold) },
                                        modifier = Modifier.testTag("language_selector_$code"),
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                                            selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                                        )
                                    )
                                }
                            }
                        }

                        Divider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))

                        // 2. CHOOSE SCREEN INTERVAL RATE (X mins)
                        Column {
                            Text(
                                text = if (selectedLanguage == "ar") "دورة جدولة التنبيه التلقائي:" else "Auto-Alert Interval Cycle:",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = if (selectedLanguage == "ar") {
                                    "سيقوم التطبيق بتحديث التنبيه بـ الستارة فور تخطي الاستخدام الفعلي الوقت المحدد."
                                } else {
                                    "The app background service updates automatically once screen usage passes selected duration."
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            FlowRow(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                val intervals = listOf(
                                    1 to (if (selectedLanguage == "ar") "١ دقيقة للتجربة" else "1 Min (Test)"),
                                    5 to (if (selectedLanguage == "ar") "٥ دقائق" else "5 Mins"),
                                    20 to (if (selectedLanguage == "ar") "٢٠ دقيقة" else "20 Mins"),
                                    30 to (if (selectedLanguage == "ar") "٣٠ دقيقة" else "30 Mins"),
                                    60 to (if (selectedLanguage == "ar") "٦٠ دقيقة" else "60 Mins")
                                )
                                intervals.forEach { (mins, label) ->
                                    val isSelected = selectedIntervalMinutes == mins
                                    FilterChip(
                                        selected = isSelected,
                                        onClick = { viewModel.updateServiceInterval(mins) },
                                        label = { Text(label, style = MaterialTheme.typography.labelSmall) },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = MaterialTheme.colorScheme.secondary,
                                            selectedLabelColor = MaterialTheme.colorScheme.onSecondary
                                        )
                                    )
                                }
                            }
                        }

                        Divider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))

                        // 3. GENERATION STYLE AI / OFFLINE
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = Localization.get(selectedLanguage, "settings_mode"),
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = if (isOfflineMode) {
                                        Localization.get(selectedLanguage, "settings_mode_offline")
                                    } else {
                                        Localization.get(selectedLanguage, "settings_mode_gemini")
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                )
                            }
                            Switch(
                                checked = !isOfflineMode,
                                onCheckedChange = { viewModel.setOfflineMode(!it) },
                                modifier = Modifier.testTag("mode_switch")
                            )
                        }
                    }
                }
            }

            // HISTORIC COMFORT STATUS DASH DETAILS
            item {
                WellnessStatsRow(
                    selectedLanguage = selectedLanguage,
                    breaks = totalBreaks,
                    seconds = totalSecondsRelaxed,
                    improvements = improvementCount
                )
            }

            // STRAIN ASSESSOR BASIS
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(24.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.12f)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp)
                    ) {
                        Text(
                            text = Localization.get(selectedLanguage, "assessed_strain"),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = Localization.get(selectedLanguage, "assessed_strain_desc"),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                            modifier = Modifier.padding(bottom = 14.dp)
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            EyeStrain.values().forEach { strain ->
                                val isSelected = preStrain == strain
                                val accentColor = when (strain) {
                                    EyeStrain.None -> Color(0xFF4CAF50)
                                    EyeStrain.Light -> Color(0xFF8BC34A)
                                    EyeStrain.Medium -> Color(0xFFFF9800)
                                    EyeStrain.Severe -> Color(0xFFF44336)
                                }

                                val translatedLabel = when (strain) {
                                    EyeStrain.None -> Localization.get(selectedLanguage, "strain_none")
                                    EyeStrain.Light -> Localization.get(selectedLanguage, "strain_light")
                                    EyeStrain.Medium -> Localization.get(selectedLanguage, "strain_medium")
                                    EyeStrain.Severe -> Localization.get(selectedLanguage, "strain_severe")
                                }

                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(16.dp))
                                        .background(
                                            if (isSelected) accentColor.copy(alpha = 0.15f)
                                            else MaterialTheme.colorScheme.background
                                        )
                                        .border(
                                            BorderStroke(
                                                width = if (isSelected) 2.dp else 1.dp,
                                                color = if (isSelected) accentColor else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
                                            ),
                                            shape = RoundedCornerShape(16.dp)
                                        )
                                        .clickable { viewModel.setPreStrain(strain) }
                                        .padding(vertical = 12.dp, horizontal = 4.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Box(
                                            modifier = Modifier
                                                .size(10.dp)
                                                .clip(CircleShape)
                                                .background(accentColor)
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = translatedLabel,
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                            color = if (isSelected) accentColor else MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // HISTORY LOGS SECTION
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = Localization.get(selectedLanguage, "wellbeing_log"),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    if (historyLogs.isNotEmpty()) {
                        TextButton(
                            onClick = { viewModel.clearLogHistory() },
                            modifier = Modifier.testTag("clear_history_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Trash Icon",
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = Localization.get(selectedLanguage, "clear_history"),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            }

            if (historyLogs.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = Localization.get(selectedLanguage, "no_breaks"),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            } else {
                items(historyLogs, key = { it.id }) { log ->
                    LogItemCard(selectedLanguage, log)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExerciseScreen(viewModel: EyeViewModel) {
    val selectedLanguage by viewModel.selectedLanguage.collectAsState()
    val uiState by viewModel.uiState.collectAsState()
    val timeLeft by viewModel.timeLeft.collectAsState()
    val totalDuration by viewModel.totalDuration.collectAsState()
    val isRunning by viewModel.isTimerRunning.collectAsState()
    val animatedScale by viewModel.visualOrbScale.collectAsState()

    var showJsonDrawer by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = Localization.get(selectedLanguage, "relax_eyes"),
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = { viewModel.resetToDashboard() },
                        modifier = Modifier.testTag("back_to_dashboard")
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back navigation icon"
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = Color.Transparent
                )
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when (val state = uiState) {
                is UiState.Idle -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                    }
                }

                is UiState.Loading -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CircularProgressIndicator(strokeWidth = 4.dp, color = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = Localization.get(selectedLanguage, "loading_title"),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = Localization.get(selectedLanguage, "loading_desc"),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                            textAlign = TextAlign.Center
                        )
                    }
                }

                is UiState.Success -> {
                    val exercise = state.exercise
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 24.dp, vertical = 12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        // Title
                        Text(
                            text = exercise.exercise_title,
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onBackground,
                            textAlign = TextAlign.Center
                        )

                        // Visual Orb Timer
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(200.dp)
                                .padding(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(160.dp)
                                    .scale(animatedScale * 1.15f)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.08f))
                            )

                            Box(
                                modifier = Modifier
                                    .size(130.dp)
                                    .scale(animatedScale)
                                    .border(4.dp, MaterialTheme.colorScheme.primary, CircleShape)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.surface),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    Text(
                                        text = "$timeLeft",
                                        style = MaterialTheme.typography.displayMedium.copy(
                                            fontSize = 42.sp,
                                            fontWeight = FontWeight.Black,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    )
                                    Text(
                                        text = Localization.get(selectedLanguage, "seconds"),
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = FontWeight.ExtraBold,
                                            letterSpacing = 2.sp,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    )
                                }
                            }

                            val progress = if (totalDuration > 0) timeLeft.toFloat() / totalDuration else 1f
                            Canvas(modifier = Modifier.size(180.dp)) {
                                drawArc(
                                    color = Color(0xFFABC7FF).copy(alpha = 0.3f),
                                    startAngle = -90f,
                                    sweepAngle = 360f,
                                    useCenter = false,
                                    style = Stroke(width = 3.dp.toPx())
                                )
                                drawArc(
                                    color = Color(0xFF005FB0),
                                    startAngle = -90f,
                                    sweepAngle = 360f * progress,
                                    useCenter = false,
                                    style = Stroke(width = 4.dp.toPx())
                                )
                            }
                        }

                        // Medical Benefit Card
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                verticalAlignment = Alignment.Top
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(24.dp)
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(MaterialTheme.colorScheme.primary),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(15.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = Localization.get(selectedLanguage, "medical_benefit"),
                                        style = MaterialTheme.typography.labelMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer
                                        )
                                    )
                                    Spacer(modifier = Modifier.height(3.dp))
                                    Text(
                                        text = exercise.benefit,
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            lineHeight = 16.sp,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.9f)
                                        )
                                    )
                                }
                            }
                        }

                        // Instructions Card
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                            shape = RoundedCornerShape(18.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = Localization.get(selectedLanguage, "instructions"),
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.secondary
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = exercise.steps,
                                    style = MaterialTheme.typography.bodyMedium,
                                    textAlign = TextAlign.Center,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }

                        // JSON EXTENSION METADATA PREVIEW (For requirements compliance!)
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            shape = RoundedCornerShape(16.dp),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.2f))
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { showJsonDrawer = !showJsonDrawer },
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.Info,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.secondary,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = if (selectedLanguage == "ar") "كود الـ JSON التابع لـ Gemini (مطور)" else "Raw Gemini JSON Metadata",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.secondary
                                        )
                                    }
                                    Icon(
                                        imageVector = if (showJsonDrawer) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.secondary
                                    )
                                }
                                if (showJsonDrawer) {
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(
                                                color = Color(0xFF1E1E1E),
                                                shape = RoundedCornerShape(10.dp)
                                            )
                                            .padding(12.dp)
                                    ) {
                                        Text(
                                            text = exercise.toJsonString(),
                                            color = Color(0xFFA3D8A5),
                                            style = MaterialTheme.typography.bodySmall.copy(
                                                fontSize = 11.sp,
                                                fontFamily = FontFamily.Monospace
                                            )
                                        )
                                    }
                                }
                            }
                        }

                        // Options Row
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(
                                onClick = { viewModel.toggleTimer() },
                                modifier = Modifier
                                    .size(50.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f))
                                    .testTag("pause_resume_button")
                            ) {
                                if (isRunning) {
                                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Box(modifier = Modifier.size(width = 4.dp, height = 16.dp).background(MaterialTheme.colorScheme.primary))
                                        Box(modifier = Modifier.size(width = 4.dp, height = 16.dp).background(MaterialTheme.colorScheme.primary))
                                    }
                                } else {
                                    Icon(
                                        imageVector = Icons.Default.PlayArrow,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(30.dp)
                                    )
                                }
                            }

                            Button(
                                onClick = { viewModel.skipOrEndExercise() },
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                modifier = Modifier
                                    .height(48.dp)
                                    .testTag("skip_exercise_button"),
                                shape = RoundedCornerShape(24.dp)
                            ) {
                                Icon(Icons.Default.Check, contentDescription = null)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = Localization.get(selectedLanguage, "complete_rest"),
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                is UiState.Error -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = Localization.get(selectedLanguage, "failed_compile"),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = state.message,
                            style = MaterialTheme.typography.bodySmall,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = { viewModel.resetToDashboard() }
                        ) {
                            Text(text = Localization.get(selectedLanguage, "return_dashboard"))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PostFeedbackScreen(viewModel: EyeViewModel) {
    val selectedLanguage by viewModel.selectedLanguage.collectAsState()
    val postStrain by viewModel.postStrainLevel.collectAsState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Favorite,
                        contentDescription = "Feedback icon",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(36.dp)
                    )
                }

                Spacer(modifier = Modifier.height(18.dp))

                Text(
                    text = Localization.get(selectedLanguage, "finished_title"),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = Localization.get(selectedLanguage, "finished_desc"),
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )

                Spacer(modifier = Modifier.height(20.dp))

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    EyeStrain.values().forEach { strain ->
                        val isSelected = postStrain == strain
                        val accentColor = when (strain) {
                            EyeStrain.None -> Color(0xFF4CAF50)
                            EyeStrain.Light -> Color(0xFF8BC34A)
                            EyeStrain.Medium -> Color(0xFFFF9800)
                            EyeStrain.Severe -> Color(0xFFF44336)
                        }

                        val translatedStrain = when (strain) {
                            EyeStrain.None -> Localization.get(selectedLanguage, "strain_none")
                            EyeStrain.Light -> Localization.get(selectedLanguage, "strain_light")
                            EyeStrain.Medium -> Localization.get(selectedLanguage, "strain_medium")
                            EyeStrain.Severe -> Localization.get(selectedLanguage, "strain_severe")
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .background(
                                    if (isSelected) accentColor.copy(alpha = 0.12f)
                                    else MaterialTheme.colorScheme.background
                                )
                                .clickable { viewModel.setPostStrain(strain) }
                                .padding(vertical = 14.dp, horizontal = 16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = isSelected,
                                onClick = { viewModel.setPostStrain(strain) },
                                colors = RadioButtonDefaults.colors(selectedColor = accentColor)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "${Localization.get(selectedLanguage, "eyes_feel_prefix")}: $translatedStrain",
                                modifier = Modifier.weight(1f),
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) accentColor else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = { viewModel.submitPostBreakFeedback() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("submit_feedback_button"),
                    shape = RoundedCornerShape(24.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Icon(Icons.Default.Check, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = Localization.get(selectedLanguage, "save_log"),
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                TextButton(
                    onClick = { viewModel.resetToDashboard() }
                ) {
                    Text(
                        text = Localization.get(selectedLanguage, "cancel_discard"),
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

@Composable
fun WellnessStatsRow(selectedLanguage: String, breaks: Int, seconds: Int, improvements: Int) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        StatPill(
            modifier = Modifier.weight(1f),
            iconValue = Icons.Default.Favorite,
            title = Localization.get(selectedLanguage, "breaks_today"),
            valueText = "$breaks",
            badgeColor = MaterialTheme.colorScheme.primary
        )
        StatPill(
            modifier = Modifier.weight(1f),
            iconValue = Icons.Default.Settings,
            title = Localization.get(selectedLanguage, "rest_time"),
            valueText = "${seconds / 60}m ${seconds % 60}s",
            badgeColor = Color(0xFFDCA732)
        )
        StatPill(
            modifier = Modifier.weight(1f),
            iconValue = Icons.Default.CheckCircle,
            title = Localization.get(selectedLanguage, "ocular_relief"),
            valueText = "$improvements ${Localization.get(selectedLanguage, "logs_suffix")}",
            badgeColor = Color(0xFF4CAF50)
        )
    }
}

@Composable
fun StatPill(
    modifier: Modifier = Modifier,
    iconValue: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    valueText: String,
    badgeColor: Color
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.08f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(badgeColor.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = iconValue,
                    contentDescription = null,
                    tint = badgeColor,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = valueText,
                style = MaterialTheme.typography.bodyLarge,
                fontSize = 15.sp,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f),
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun LogItemCard(selectedLanguage: String, log: EyeBreakLog) {
    val dateString = remember(log.timestamp) {
        val sdf = SimpleDateFormat("MMM d, hh:mm a", Locale.getDefault())
        sdf.format(Date(log.timestamp))
    }

    val preColor = getStrainColor(log.preStrainLevel)
    val postColor = getStrainColor(log.postStrainLevel)

    val translatedPre = when (log.preStrainLevel) {
        "None" -> Localization.get(selectedLanguage, "strain_none")
        "Light" -> Localization.get(selectedLanguage, "strain_light")
        "Medium" -> Localization.get(selectedLanguage, "strain_medium")
        "Severe" -> Localization.get(selectedLanguage, "strain_severe")
        else -> log.preStrainLevel
    }

    val translatedPost = when (log.postStrainLevel) {
        "None" -> Localization.get(selectedLanguage, "strain_none")
        "Light" -> Localization.get(selectedLanguage, "strain_light")
        "Medium" -> Localization.get(selectedLanguage, "strain_medium")
        "Severe" -> Localization.get(selectedLanguage, "strain_severe")
        else -> log.postStrainLevel
    }

    val improvements = getStrainWeight(log.preStrainLevel) > getStrainWeight(log.postStrainLevel)

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.08f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(
                        if (improvements) Color(0xFFE8F5E9) else MaterialTheme.colorScheme.surfaceVariant
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (improvements) Icons.Default.CheckCircle else Icons.Default.Info,
                    contentDescription = null,
                    tint = if (improvements) Color(0xFF4CAF50) else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = log.exerciseTitle,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "$dateString • ${log.durationSeconds}s",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "$translatedPre ➔ $translatedPost",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        color = if (improvements) Color(0xFF2E7D32) else MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}

fun getStrainWeight(levelName: String): Int {
    return when (levelName.lowercase()) {
        "none" -> 0
        "light" -> 1
        "medium" -> 2
        "severe" -> 3
        else -> 1
    }
}

fun getStrainColor(levelName: String): Color {
    return when (levelName.lowercase()) {
        "none" -> Color(0xFF4CAF50)
        "light" -> Color(0xFF8BC34A)
        "medium" -> Color(0xFFFF9800)
        "severe" -> Color(0xFFF44336)
        else -> Color(0xFF8BC34A)
    }
}
