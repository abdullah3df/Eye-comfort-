package com.example.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
                    Screen.Disclaimer -> DisclaimerScreen(viewModel)
                    Screen.Dashboard -> DashboardScreen(viewModel)
                    Screen.Exercise -> ExerciseScreen(viewModel)
                    Screen.PostFeedback -> PostFeedbackScreen(viewModel)
                }
            }
        }
    }
}

enum class DashboardTab {
    Home,
    Stats,
    Settings
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

    var activeTab by remember { mutableStateOf(DashboardTab.Home) }
    var showMedicalGuidelinesSheet by remember { mutableStateOf(false) }

    if (showMedicalGuidelinesSheet) {
        ModalBottomSheet(
            onDismissRequest = { showMedicalGuidelinesSheet = false },
            containerColor = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
        ) {
            MedicalGuidelinesSheetContent(
                selectedLanguage = selectedLanguage,
                onDismiss = { showMedicalGuidelinesSheet = false }
            )
        }
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
                                imageVector = when (activeTab) {
                                    DashboardTab.Home -> Icons.Default.Favorite
                                    DashboardTab.Stats -> Icons.Default.CheckCircle
                                    DashboardTab.Settings -> Icons.Default.Settings
                                },
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = when (activeTab) {
                                DashboardTab.Home -> Localization.get(selectedLanguage, "app_title")
                                DashboardTab.Stats -> if (selectedLanguage == "ar") "تحليل الاستراحة" else "Comfort Analytics"
                                DashboardTab.Settings -> Localization.get(selectedLanguage, "settings_title")
                            },
                            fontWeight = FontWeight.Black,
                            style = MaterialTheme.typography.titleLarge
                        )
                    }
                },
                actions = {
                    if (activeTab == DashboardTab.Home) {
                        IconButton(
                            onClick = { showMedicalGuidelinesSheet = true },
                            modifier = Modifier.testTag("info_medical_guidelines")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = "Medical Guidelines Info Icon",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(26.dp)
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = Color.Transparent
                )
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 8.dp
            ) {
                NavigationBarItem(
                    selected = activeTab == DashboardTab.Home,
                    onClick = { activeTab = DashboardTab.Home },
                    icon = { Icon(imageVector = Icons.Default.Home, contentDescription = null) },
                    label = { Text(text = if (selectedLanguage == "ar") "تفـعيل" else "Shield", fontWeight = FontWeight.Bold) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.primary,
                        unselectedIconColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                        selectedTextColor = MaterialTheme.colorScheme.primary,
                        unselectedTextColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                )
                NavigationBarItem(
                    selected = activeTab == DashboardTab.Stats,
                    onClick = { activeTab = DashboardTab.Stats },
                    icon = { Icon(imageVector = Icons.Default.CheckCircle, contentDescription = null) },
                    label = { Text(text = if (selectedLanguage == "ar") "الإحصائيات" else "Analytics", fontWeight = FontWeight.Bold) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.primary,
                        unselectedIconColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                        selectedTextColor = MaterialTheme.colorScheme.primary,
                        unselectedTextColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                )
                NavigationBarItem(
                    selected = activeTab == DashboardTab.Settings,
                    onClick = { activeTab = DashboardTab.Settings },
                    icon = { Icon(imageVector = Icons.Default.Settings, contentDescription = null) },
                    label = { Text(text = if (selectedLanguage == "ar") "الضبط واللغة" else "Settings", fontWeight = FontWeight.Bold) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.primary,
                        unselectedIconColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                        selectedTextColor = MaterialTheme.colorScheme.primary,
                        unselectedTextColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                )
            }
        }
    ) { paddingValues ->
        AnimatedContent(
            targetState = activeTab,
            transitionSpec = {
                fadeIn(animationSpec = spring()) + slideInHorizontally { it / 3 } togetherWith
                        fadeOut(animationSpec = spring()) + slideOutHorizontally { -it / 3 }
            },
            label = "TabContentTransition",
            modifier = Modifier.padding(paddingValues)
        ) { currentTab ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 20.dp, vertical = 6.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                when (currentTab) {
                    DashboardTab.Home -> {
                        // 1. GORGEOUS CENTRAL PULSING WIDGET
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 12.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(220.dp)
                                        .clip(CircleShape)
                                        .background(
                                            brush = Brush.radialGradient(
                                                colors = listOf(
                                                    if (isServiceRunning) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                                                    else MaterialTheme.colorScheme.outline.copy(alpha = 0.05f),
                                                    Color.Transparent
                                                )
                                            )
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(170.dp)
                                            .clip(CircleShape)
                                            .background(MaterialTheme.colorScheme.surface)
                                            .border(
                                                BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.12f)),
                                                shape = CircleShape
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Column(
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            verticalArrangement = Arrangement.Center,
                                            modifier = Modifier.padding(12.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Favorite,
                                                contentDescription = null,
                                                tint = if (isServiceRunning) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                                                modifier = Modifier.size(32.dp)
                                            )
                                            Spacer(modifier = Modifier.height(4.dp))
                                            if (isServiceRunning) {
                                                val progressVal = if (selectedIntervalMinutes > 0) (screenOnSeconds.toFloat() / (selectedIntervalMinutes * 60f)).coerceIn(0f, 1f) else 0f
                                                Text(
                                                    text = if (selectedLanguage == "ar") "درع النظر نشط" else "Shield Active",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.primary,
                                                    fontWeight = FontWeight.ExtraBold
                                                )
                                                Spacer(modifier = Modifier.height(2.dp))
                                                Text(
                                                    text = "%02d:%02d".format(screenOnSeconds / 60, screenOnSeconds % 60),
                                                    style = MaterialTheme.typography.headlineMedium,
                                                    fontWeight = FontWeight.Black,
                                                    color = MaterialTheme.colorScheme.onSurface
                                                )
                                                Spacer(modifier = Modifier.height(2.dp))
                                                Text(
                                                    text = if (selectedLanguage == "ar") "الهدف: $selectedIntervalMinutes د" else "Goal: ${selectedIntervalMinutes}m",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                                )
                                            } else {
                                                Text(
                                                    text = if (selectedLanguage == "ar") "الحماية الذكية متوقفة" else "Shield Deactivated",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.error,
                                                    fontWeight = FontWeight.ExtraBold
                                                )
                                                Spacer(modifier = Modifier.height(4.dp))
                                                Text(
                                                    text = "OFF",
                                                    style = MaterialTheme.typography.headlineMedium,
                                                    fontWeight = FontWeight.Black,
                                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                                )
                                                Spacer(modifier = Modifier.height(2.dp))
                                                Text(
                                                    text = if (selectedLanguage == "ar") "اضغط للتشغيل أدناه" else "Tap toggle below",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                                                    textAlign = TextAlign.Center
                                                )
                                            }
                                        }
                                    }

                                    if (isServiceRunning) {
                                        val progressVal = if (selectedIntervalMinutes > 0) (screenOnSeconds.toFloat() / (selectedIntervalMinutes * 60f)).coerceIn(0f, 1f) else 0f
                                        Canvas(modifier = Modifier.size(190.dp)) {
                                            drawArc(
                                                color = Color(0xFFABC7FF).copy(alpha = 0.2f),
                                                startAngle = -90f,
                                                sweepAngle = 360f,
                                                useCenter = false,
                                                style = Stroke(width = 4.dp.toPx())
                                            )
                                            drawArc(
                                                color = Color(0xFF005FB0),
                                                startAngle = -90f,
                                                sweepAngle = 360f * progressVal,
                                                useCenter = false,
                                                style = Stroke(width = 6.dp.toPx())
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // 2. PRIMARY MASTER SHIELD TRIGGER CARD (MINIMALIST SINGLE SWITCH TRIGGER)
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(24.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isServiceRunning) {
                                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f)
                                    } else {
                                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)
                                    }
                                ),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(20.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = if (selectedLanguage == "ar") "تشغيل حماية العين" else "Start/Stop Eye Protection",
                                            fontWeight = FontWeight.Bold,
                                            style = MaterialTheme.typography.titleMedium,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = if (isServiceRunning) {
                                                if (selectedLanguage == "ar") "درع النظر نشط وفعّال" else "Protection is active"
                                            } else {
                                                if (selectedLanguage == "ar") "الحماية متوقفة حالياً" else "Protection is stopped"
                                            },
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                        )
                                    }
                                    
                                    Switch(
                                        checked = isServiceRunning,
                                        onCheckedChange = { active ->
                                            if (active) {
                                                viewModel.startEyeProtection()
                                            } else {
                                                viewModel.stopEyeProtection()
                                            }
                                        },
                                        modifier = Modifier.testTag("protection_toggle_switch")
                                    )
                                }
                            }
                        }

                        // 3. EYE FATIGUE COMFORT ASSESSOR
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                shape = RoundedCornerShape(24.dp),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.12f)),
                                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                            ) {
                                Column(modifier = Modifier.padding(18.dp)) {
                                    Text(
                                        text = Localization.get(selectedLanguage, "assessed_strain"),
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = Localization.get(selectedLanguage, "assessed_strain_desc"),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                    )
                                    Spacer(modifier = Modifier.height(14.dp))

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

                                            val emoji = when (strain) {
                                                EyeStrain.None -> "😊"
                                                EyeStrain.Light -> "🙂"
                                                EyeStrain.Medium -> "😐"
                                                EyeStrain.Severe -> "😫"
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
                                                        if (isSelected) accentColor.copy(alpha = 0.12f)
                                                        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                                                    )
                                                    .border(
                                                        BorderStroke(
                                                            width = if (isSelected) 2.dp else 1.dp,
                                                            color = if (isSelected) accentColor else MaterialTheme.colorScheme.outline.copy(alpha = 0.12f)
                                                        ),
                                                        shape = RoundedCornerShape(16.dp)
                                                    )
                                                    .clickable { viewModel.setPreStrain(strain) }
                                                    .padding(vertical = 12.dp, horizontal = 4.dp),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                    Text(text = emoji, fontSize = 20.sp)
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

                        // 4. DYNAMIC MEDICAL INSIGHT RECOMMENDATIONS CARD
                        item {
                            val level = preStrain
                            val tipTitle = if (selectedLanguage == "ar") {
                                when (level) {
                                    EyeStrain.None -> "وقاية بصرية: حافظ على استقرار بصرك"
                                    EyeStrain.Light -> "توصية طبية عاجلة: ترطيب العين"
                                    EyeStrain.Medium -> "تمرين علاجي: قاعدة 20-20-20"
                                    EyeStrain.Severe -> "تنبيه طارئ: إراحة قرنية العين فوراً"
                                }
                            } else {
                                when (level) {
                                    EyeStrain.None -> "Visual Maintenance: Active Care"
                                    EyeStrain.Light -> "Ophthalmologist Advice: Hydrate Cornea"
                                    EyeStrain.Medium -> "Therapeutic Tip: 20-20-20 Rules"
                                    EyeStrain.Severe -> "Urgent Alert: Complete Ocular Relief"
                                }
                            }

                            val tipDesc = if (selectedLanguage == "ar") {
                                when (level) {
                                    EyeStrain.None -> "أنت بمستوى صحي رائع! استمر بضبط سطوع شاشة الهاتف للتكيف مع إضاءة المحيط لتقليل انقباض عضلات العين وتجنب التعب المفاجئ."
                                    EyeStrain.Light -> "تعاني عيناك من تشنج عضلي بسيط. ركز على تمرين الرمش المتتالي السريع (Blinking) بمعدل 10 مرات لإثارة الغدد الدمعية ومنع جفاف القرنية."
                                    EyeStrain.Medium -> "انقباض عضلات العين النشطة أعلى الآن. اتبع فورًا قانون (20-20-20): انظر لجسم يبعد 20 قدمًا لمدة 20 ثانية لتوطيد التركيز البؤري."
                                    EyeStrain.Severe -> "مستوى التوتر العصبي البصري متضخم جداً. ننصح بوقف استخدام الجهاز فورًا، وإغلاق العينين بالكامل لتهدئة عضلات القزحية وتجنب الصداع."
                                }
                            } else {
                                when (level) {
                                    EyeStrain.None -> "Your eyes are comfortably adjusted. We recommend ensuring screen glare limits and room hydration are stable."
                                    EyeStrain.Light -> "Slight fatigue is initiating. Take a quick blink test: blink 8-10 times consecutively to hydrate the cornea surface."
                                    EyeStrain.Medium -> "Excess tension is present on critical muscles. Align with the 20-20-20 medical rule: focus on an item 20ft away for 20 seconds."
                                    EyeStrain.Severe -> "Urgent optic strain alert. Immediately close your eyes completely, cup them with your palms (Palming technique) to block light."
                                }
                            }

                            val containerColor = when (level) {
                                EyeStrain.None -> Color(0xFFE8F5E9)
                                EyeStrain.Light -> Color(0xFFF1F8E9)
                                EyeStrain.Medium -> Color(0xFFFFF3E0)
                                EyeStrain.Severe -> Color(0xFFFFEBEE)
                            }

                            val contentColor = when (level) {
                                EyeStrain.None -> Color(0xFF2E7D32)
                                EyeStrain.Light -> Color(0xFF558B2F)
                                EyeStrain.Medium -> Color(0xFFE65100)
                                EyeStrain.Severe -> Color(0xFFC62828)
                            }

                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(20.dp),
                                colors = CardDefaults.cardColors(containerColor = containerColor),
                                border = BorderStroke(1.dp, contentColor.copy(alpha = 0.15f))
                            ) {
                                Row(
                                    modifier = Modifier.padding(16.dp),
                                    verticalAlignment = Alignment.Top
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(28.dp)
                                            .clip(CircleShape)
                                            .background(contentColor.copy(alpha = 0.15f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Info,
                                            contentDescription = null,
                                            tint = contentColor,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Text(
                                            text = tipTitle,
                                            style = MaterialTheme.typography.labelLarge,
                                            fontWeight = FontWeight.Bold,
                                            color = contentColor
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = tipDesc,
                                            style = MaterialTheme.typography.bodySmall,
                                            lineHeight = 17.sp,
                                            color = contentColor.copy(alpha = 0.85f)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    DashboardTab.Stats -> {
                        // 1. STATS METRICS ROW
                        item {
                            WellnessStatsRow(
                                selectedLanguage = selectedLanguage,
                                breaks = totalBreaks,
                                seconds = totalSecondsRelaxed,
                                improvements = improvementCount
                            )
                        }

                        // 2. THE CHOSEN RADICAL WEEKLY PROGRESS CHART
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                shape = RoundedCornerShape(24.dp),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f)),
                                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                            ) {
                                Column(modifier = Modifier.padding(18.dp)) {
                                    Text(
                                        text = if (selectedLanguage == "ar") "بيان معدل تفادي الإجهاد البصري" else "Optimum Break Stability Trend",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Spacer(modifier = Modifier.height(14.dp))

                                    val chartPoints = remember(historyLogs) {
                                        val points = historyLogs.take(5).reversed()
                                        points.map { it.durationSeconds.toFloat().coerceIn(10f, 120f) }
                                    }

                                    if (chartPoints.isNotEmpty()) {
                                        val primaryColor = MaterialTheme.colorScheme.primary
                                        val tertiaryColor = MaterialTheme.colorScheme.tertiary
                                        val surfaceVariant = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f)

                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(130.dp)
                                        ) {
                                            Canvas(modifier = Modifier.fillMaxSize()) {
                                                val spacing = size.width / (chartPoints.size + 1)
                                                val maxVal = 120f

                                                // Clean grid horizontal bounds
                                                for (i in 1..3) {
                                                    val y = size.height * (i / 4f)
                                                    drawLine(
                                                        color = surfaceVariant,
                                                        start = androidx.compose.ui.geometry.Offset(0f, y),
                                                        end = androidx.compose.ui.geometry.Offset(size.width, y),
                                                        strokeWidth = 1.dp.toPx()
                                                    )
                                                }

                                                // Paint the contemporary styled rounded bar vectors
                                                chartPoints.forEachIndexed { index, duration ->
                                                    val x = spacing * (index + 1)
                                                    val barHeight = size.height * (duration / maxVal)
                                                    val y = size.height - barHeight

                                                    drawRoundRect(
                                                        color = if (index == chartPoints.lastIndex) tertiaryColor else primaryColor,
                                                        topLeft = androidx.compose.ui.geometry.Offset(x - 12.dp.toPx(), y),
                                                        size = androidx.compose.ui.geometry.Size(24.dp.toPx(), barHeight),
                                                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(6.dp.toPx(), 6.dp.toPx())
                                                    )
                                                }
                                            }
                                        }
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = if (selectedLanguage == "ar") "← الجلسات السابقة" else "← Older sessions",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                                            )
                                            Text(
                                                text = if (selectedLanguage == "ar") "آخر استراحة مكتملة 🌟" else "Latest session Completed 🌟",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.tertiary,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    } else {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(90.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = if (selectedLanguage == "ar") "أكمل تمارين إراحة العين لتسجيل أولى الإحصائيات هنا." else "Log ocular break sessions to plot modern trend timelines here.",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                                                textAlign = TextAlign.Center
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // 3. LOGGER HEADER & ACTIONS
                        item {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = Localization.get(selectedLanguage, "wellbeing_log"),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.ExtraBold
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
                                            color = MaterialTheme.colorScheme.error,
                                            fontWeight = FontWeight.Bold
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
                                            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.25f),
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

                    DashboardTab.Settings -> {
                        // 1. LANGUAGE SETTINGS (CRITICAL OUT OF DASH MOTION!)
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                shape = RoundedCornerShape(24.dp),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f)),
                                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                            ) {
                                Column(modifier = Modifier.padding(18.dp)) {
                                    Text(
                                        text = Localization.get(selectedLanguage, "settings_lang"),
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = Localization.get(selectedLanguage, "settings_lang_desc"),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f)
                                    )
                                    Spacer(modifier = Modifier.height(14.dp))

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
                            }
                        }

                        // 2. ADAPTIVE ALERT INTERVAL GRIDS
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                shape = RoundedCornerShape(24.dp),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f)),
                                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                            ) {
                                Column(modifier = Modifier.padding(18.dp)) {
                                    Text(
                                        text = if (selectedLanguage == "ar") "مؤقت ودورة التذكير التلقائي" else "Auto-Alert Rest Interval",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = if (selectedLanguage == "ar") {
                                            "سيقوم النظام بفحص مدة التشغيل وإرسال إشعار استراحة فور تجاوز الوقت المحدد."
                                        } else {
                                            "Defines screen monitoring window intervals. Rest warning will fire post selected cycle duration."
                                        },
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                    )
                                    Spacer(modifier = Modifier.height(14.dp))

                                    FlowRow(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        verticalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        val intervals = listOf(
                                            1 to (if (selectedLanguage == "ar") "١ د للتجربة" else "1 M (Test)"),
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
                                                label = { Text(label, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold) },
                                                colors = FilterChipDefaults.filterChipColors(
                                                    selectedContainerColor = MaterialTheme.colorScheme.secondary,
                                                    selectedLabelColor = MaterialTheme.colorScheme.onSecondary
                                                )
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // 3. ENGINE MODE SWITCH AI / OFFLINE
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                shape = RoundedCornerShape(24.dp),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f)),
                                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .clickable { viewModel.setOfflineMode(!isOfflineMode) }
                                        .padding(18.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = Localization.get(selectedLanguage, "settings_mode"),
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
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

                        // 4. SCIENTIFIC ADVICE OF 20-20-20 EXPLAINERS
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                shape = RoundedCornerShape(24.dp),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.12f)),
                                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                            ) {
                                Column(modifier = Modifier.padding(18.dp)) {
                                    Text(
                                        text = if (selectedLanguage == "ar") "قاعدة 20-20-20 المعتمدة طبياً" else "The Medical 20-20-20 Rule",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = if (selectedLanguage == "ar") {
                                            "لأجل حماية بصرك ووقايته من أعراض متلازمة الرؤية الحاسوبية (Computer Vision Syndrome)، تنص الأكاديمية الأمريكية لطب العيون على تفعيل هذا المبدأ:\n\n" +
                                            "١. كل ٢٠ دقيقة من التحديق المستمر في الشاشة.\n" +
                                            "٢. خذ استراحة بصرية لتركيز نظرك على مجسم يبعد ٢٠ قدماً على الأقل.\n" +
                                            "٣. تأمل هذا المجسم وارمش عينيك بهدوء لمدة لا تقل عن ٢٠ ثانية لتعديل انقباضات العضلات الهدبية تماماً."
                                        } else {
                                            "To reduce symptoms of Computer Vision Syndrome and minimize long-term optical strain, ophthalmology experts advise checking these three instructions:\n\n" +
                                            "1. For every 20 minutes spent working in front of a flat screen.\n" +
                                            "2. Direct your gaze to an object at least 20 feet away.\n" +
                                            "3. Keep focusing on that distant point for at least 20 seconds to completely release ciliary muscle spasms."
                                        },
                                        style = MaterialTheme.typography.bodySmall,
                                        lineHeight = 18.sp,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                                    )
                                }
                            }
                        }

                        // 4.5 TERMS AND MEDICAL DISCLAIMER REVIEW CARD
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                shape = RoundedCornerShape(24.dp),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f)),
                                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .clickable { viewModel.showDisclaimer() }
                                        .padding(18.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = if (selectedLanguage == "ar") "شروط الخدمة وإخلاء المسؤولية" else "Terms & Medical Disclaimer",
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = if (selectedLanguage == "ar") {
                                                "راجع سياسة الخصوصية المحلية وبنود المسؤولية الطبية المعتمدة وموانع الاستخدام."
                                            } else {
                                                "Review local privacy, approved medical non-liability clauses, and terms of service."
                                            },
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                                            lineHeight = 16.sp
                                        )
                                    }
                                    Icon(
                                        imageVector = Icons.Default.Info,
                                        contentDescription = "Review Disclaimer Icon",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            }
                        }

                        // 5. SUGGESTION AND FEEDBACK DIRECTIVES SECTION
                        item {
                            val supportEmail = "info.cik@cikcoin.art"
                            val clipboardManager = androidx.compose.ui.platform.LocalClipboardManager.current

                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f)
                                ),
                                shape = RoundedCornerShape(24.dp),
                                border = BorderStroke(
                                    width = 1.dp,
                                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)
                                )
                            ) {
                                Column(modifier = Modifier.padding(20.dp)) {
                                    // Header with Email/Suggestion Icon
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(36.dp)
                                                .clip(CircleShape)
                                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Email,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Text(
                                            text = Localization.get(selectedLanguage, "suggestion_title"),
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(12.dp))

                                    // Description
                                    Text(
                                        text = Localization.get(selectedLanguage, "suggestion_desc"),
                                        style = MaterialTheme.typography.bodySmall,
                                        lineHeight = 18.sp,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                                    )

                                    Spacer(modifier = Modifier.height(10.dp))

                                    // Guidelines Lists
                                    Column(
                                        verticalArrangement = Arrangement.spacedBy(6.dp),
                                        modifier = Modifier.padding(start = 4.dp)
                                    ) {
                                        Text(
                                            text = Localization.get(selectedLanguage, "suggestion_guide1"),
                                            style = MaterialTheme.typography.bodySmall,
                                            lineHeight = 17.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f)
                                        )
                                        Text(
                                            text = Localization.get(selectedLanguage, "suggestion_guide2"),
                                            style = MaterialTheme.typography.bodySmall,
                                            lineHeight = 17.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f)
                                        )
                                        Text(
                                            text = Localization.get(selectedLanguage, "suggestion_guide3"),
                                            style = MaterialTheme.typography.bodySmall,
                                            lineHeight = 17.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f)
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(16.dp))

                                    Divider(
                                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                                    )

                                    Spacer(modifier = Modifier.height(14.dp))

                                    // Display Email Label & Interactive Copy Box
                                    Text(
                                        text = Localization.get(selectedLanguage, "suggestion_email_label"),
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))

                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                                            .border(
                                                width = 1.dp,
                                                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f),
                                                shape = RoundedCornerShape(12.dp)
                                            )
                                            .clickable {
                                                clipboardManager.setText(androidx.compose.ui.text.AnnotatedString(supportEmail))
                                                android.widget.Toast.makeText(
                                                    context,
                                                    Localization.get(selectedLanguage, "suggestion_email_copied"),
                                                    android.widget.Toast.LENGTH_SHORT
                                                ).show()
                                            }
                                            .padding(horizontal = 14.dp, vertical = 10.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                imageVector = Icons.Default.Face,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = supportEmail,
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.SemiBold,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        Icon(
                                            imageVector = Icons.Default.Share,
                                            contentDescription = "Copy Email",
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(14.dp))

                                    // Large Action Button to launch Email Client
                                    Button(
                                        onClick = {
                                            val emailIntent = android.content.Intent(android.content.Intent.ACTION_SENDTO).apply {
                                                data = android.net.Uri.parse("mailto:")
                                                putExtra(android.content.Intent.EXTRA_EMAIL, arrayOf(supportEmail))
                                                val subjectStr = if (selectedLanguage == "ar") {
                                                    "اقتراح لتطبيق راحة العين"
                                                } else {
                                                    "Eye Relief App Suggestion"
                                                }
                                                putExtra(android.content.Intent.EXTRA_SUBJECT, subjectStr)
                                            }
                                            try {
                                                context.startActivity(emailIntent)
                                            } catch (e: Exception) {
                                                // Fallback copy if failure
                                                clipboardManager.setText(androidx.compose.ui.text.AnnotatedString(supportEmail))
                                                android.widget.Toast.makeText(
                                                    context,
                                                    Localization.get(selectedLanguage, "suggestion_email_copied"),
                                                    android.widget.Toast.LENGTH_SHORT
                                                ).show()
                                            }
                                        },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(48.dp),
                                        shape = RoundedCornerShape(14.dp),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = MaterialTheme.colorScheme.primary
                                        )
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Send,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = Localization.get(selectedLanguage, "suggestion_btn"),
                                            style = MaterialTheme.typography.labelLarge,
                                            fontWeight = FontWeight.ExtraBold
                                        )
                                    }
                                }
                            }
                        }
                    }
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
                            val activeColor = MaterialTheme.colorScheme.primary
                            val trackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                            Canvas(modifier = Modifier.size(180.dp)) {
                                drawArc(
                                    color = trackColor,
                                    startAngle = -90f,
                                    sweepAngle = 360f,
                                    useCenter = false,
                                    style = Stroke(width = 3.dp.toPx())
                                )
                                drawArc(
                                    color = activeColor,
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

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun MedicalGuidelinesSheetContent(selectedLanguage: String, onDismiss: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.navigationBars)
            .padding(24.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (selectedLanguage == "ar") "الإرشادات الطبية المعتمدة" else "Approved Medical Guidelines",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.primary
            )
            IconButton(onClick = onDismiss) {
                Icon(imageVector = Icons.Default.Close, contentDescription = "Close Sheet")
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = if (selectedLanguage == "ar") {
                "أربع قواعد لقضاء راحة العين وحمايتها من الإجهاد الرقمي وفقاً لتوصيات الجمعية الرائدة للرعاية العينية:"
            } else {
                "Four essential methods backed by medical consensus to ease computer vision fatigue and digital strain:"
            },
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
        )
        Spacer(modifier = Modifier.height(20.dp))

        val guidelines = listOf(
            GuidelineItem(
                title_en = "1. 20-20-20 Rule",
                title_ar = "١. قاعدة 20-20-20",
                desc_en = "Every 20 minutes, focus on an object at least 20 feet away for 20 seconds. This relaxes the ciliary eye muscles and resets visual focus.",
                desc_ar = "كل ٢٠ دقيقة من النظر للشاشة، ركز على جسم يبعد ٢٠ قدماً (٦ أمتار) على الأقل لمدة ٢٠ ثانية لإراحة العضلة الهدبية للعين.",
                icon = Icons.Default.Favorite
            ),
            GuidelineItem(
                title_en = "2. Intentional Blinking",
                title_ar = "٢. الرمش بوعي وتعمّد",
                desc_en = "Squeeze your eyes gently and blink consciously to restore the tear film. Humans blink roughly 66% less while staring at monitors.",
                desc_ar = "أغمض عينيك بلطف وارمش بوعي لتوزيع الغشاء الدمعي. ينخفض معدل رمش الطبيعي لدى الإنسان بنسبة ٦٦٪ عند التحديق بالهواتف.",
                icon = Icons.Default.Refresh
            ),
            GuidelineItem(
                title_en = "3. Palming",
                title_ar = "٣. تمرين كف اليد (Palming)",
                desc_en = "Rub your hands together to generate gentle warmth, then place your cupped palms over closed eyes for 30-60s. Warm darkness relaxes the optic nerve.",
                desc_ar = "افرك كفيك معاً لتوليد طاقة دفء لطيفة، ثم غطِ عينيك المغلقتين برفق بوعاء يديك لمدة دقيقة لتسهيل إراحة الخلايا العصبية والقرنية.",
                icon = Icons.Default.Face
            ),
            GuidelineItem(
                title_en = "4. Screen Ergonomics",
                title_ar = "٤. بيئة ونظم الشاشة الصحية",
                desc_en = "Keep the phone exactly 30-40 cm away and hold the screen slightly below eye level (10-15 cm) to reduce tear film depletion and dynamic evaporation.",
                desc_ar = "أمسك الهاتف بمسافة ٣٠-٤٠ سم بعيداً، واجعل مستوى الشاشة أكثر انخفاضاً من مستوى عينيك بـ ١٠-١٥ سم لتفادي التبخر المتكرر للدمع.",
                icon = Icons.Default.CheckCircle
            )
        )

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(guidelines) { item ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f)),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = item.icon,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (selectedLanguage == "ar") item.title_ar else item.title_en,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = if (selectedLanguage == "ar") item.desc_ar else item.desc_en,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                                lineHeight = 18.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

data class GuidelineItem(
    val title_en: String,
    val title_ar: String,
    val desc_en: String,
    val desc_ar: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DisclaimerScreen(viewModel: EyeViewModel) {
    val selectedLanguage by viewModel.selectedLanguage.collectAsState()
    val isDisclaimerAlreadyAccepted = remember { viewModel.isDisclaimerAccepted() }
    var isAccepted by remember { mutableStateOf(isDisclaimerAlreadyAccepted) }
    val scrollState = rememberScrollState()

    val isAr = selectedLanguage == "ar"

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            if (isDisclaimerAlreadyAccepted) {
                CenterAlignedTopAppBar(
                    title = {
                        Text(
                            text = if (isAr) "شروط الاستخدام والخصوصية" else "Terms & Privacy",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = { viewModel.resetToDashboard() }) {
                            Icon(
                                imageVector = Icons.Default.ArrowBack,
                                contentDescription = "Back icon",
                                tint = MaterialTheme.colorScheme.onBackground
                            )
                        }
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = Color.Transparent
                    )
                )
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (!isDisclaimerAlreadyAccepted) {
                Spacer(modifier = Modifier.height(16.dp))
                
                // Header Shield Icon
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(32.dp)
                    )
                }
                
                Spacer(modifier = Modifier.height(16.dp))
            }
            
            // Title block
            Text(
                text = if (isAr) "شروط الاستخدام وإخلاء المسؤولية" else "Terms & Medical Disclaimer",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.primary,
                textAlign = TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(4.dp))
            
            Text(
                text = if (isAr) "يرجى قراءة الشروط والموافقة عليها للبدء" else "Please read and accept the guidelines to continue",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                textAlign = TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Scrollable text Area (inside a card)
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.12f)),
                shape = RoundedCornerShape(20.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(scrollState)
                    ) {
                        Text(
                            text = if (isAr) "١. إخلاء المسؤولية الطبية" else "1. Medical Disclaimer",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = if (isAr) {
                                "هذا التطبيق (مساعد حماية العين) ليس جهازاً طبياً. لم يتم تقييم محتواه أو تمارينه من قِبل أي منظمة صحية معتمدة لعلاج أمراض العيون أو الوقاية منها. التمارين والمقترحات والأنماات البصرية الارتدادية المقدمة منه، بما في ذلك قاعدة (20-20-20)، تهدف حصراً إلى التوعية بالعادات والراحة والوقاية المكتبية العامة. يجب استشارة طبيب عيون مرخص فور الشعور بأي ألم بصري أو ضعف حاد بالنظر."
                            } else {
                                "This application (Eye Break Assistant) is NOT a medical device. Its contents, features, and exercises have not been evaluated or certified by any ophthalmological association or regulatory healthcare agency to diagnose, treat, or prevent any optical or systemic disease. All suggestions and interactive guides (such as the 20-20-20 Rule and Palming) are intended strictly for dynamic posture, micro-relaxation, and eye comfort habits. Consult a licensed ophthalmologist immediately if you experience visual eye pain, severe fatigue, or visual deterioration."
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            lineHeight = 20.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                        )
                        
                        Spacer(modifier = Modifier.height(18.dp))
                        
                        Text(
                            text = if (isAr) "٢. حدود المسؤولية" else "2. Limitation of Liability",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = if (isAr) {
                                "باستخدامك لهذا التطبيق، فإنك تقر وتوافق على أن تشغيله وإرشادات التمارين تتم على مسؤوليتك الخاصة. لا يتحمل المطورون أو الجهة المالكة أي مسؤولية عن تطور أي عرض طبي، أو إجهاد حاد بالقرنية أو الصداع، أو متلازمات الرؤية الرقمية المقترنة بالأجهزة اللوحية."
                            } else {
                                "By utilizing this application, you explicitly acknowledge and agree that your participation in any visual exercise is at your sole personal risk. The developers assume no direct or indirect liability for any physical or dynamic optical syndromes, computer vision syndrome (CVS) advancement, chronic headaches, or any visual degradation allegedly correlated with using this comfort system."
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            lineHeight = 20.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                        )
                        
                        Spacer(modifier = Modifier.height(18.dp))
                        
                        Text(
                            text = if (isAr) "٣. سياسة حماية الخصية" else "3. Privacy Policy Summary",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = if (isAr) {
                                "يبقى سجل أخذ الاستراحات ومستوى التعب البصري (قبل وبعد التمرين) مخزناً بالكامل محلياً وبشكل مشفر في قاعدة بيانات SQLite مدمجة بجهازك الشخصي. لا نقوم برفع أو نقل أي بيانات صحية أو شخصية لخوادم خارجية على الإطلاق."
                            } else {
                                "All completed eye break records, dynamic timestamps, and pre/post fatigue evaluation logs are stored entirely in a secure local Room SQLite database embedded on your operating device. No health metrics, local usage analytics, or identity indexes are transmitted to remote servers."
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            lineHeight = 20.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Checkbox Container Row (minimum touch target 48dp, interactive, clickable)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { isAccepted = !isAccepted }
                    .padding(vertical = 12.dp, horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(
                    checked = isAccepted,
                    onCheckedChange = { isAccepted = it },
                    colors = CheckboxDefaults.colors(
                        checkedColor = MaterialTheme.colorScheme.primary,
                        uncheckedColor = MaterialTheme.colorScheme.outline
                    ),
                    modifier = Modifier.testTag("disclaimer_checkbox")
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (isAr) {
                        "لقد قرأت الشروط الطبية وإخلاء المسؤولية وأوافق عليها تماماً"
                    } else {
                        "I have read and agree to the Terms & Medical Disclaimer."
                    },
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.weight(1f)
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Accept Button
            Button(
                onClick = {
                    if (isAccepted) {
                        viewModel.acceptDisclaimer()
                    }
                },
                enabled = isAccepted,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    disabledContainerColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    disabledContentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .testTag("accept_button"),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text(
                    text = if (isAr) "موافقة ومتابعة" else "Accept & Continue",
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        }
    }
}
