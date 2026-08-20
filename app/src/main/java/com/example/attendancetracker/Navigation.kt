package com.example.attendancetracker

import android.app.Activity
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.attendancetracker.theme.AttendanceTrackerTheme
import com.example.attendancetracker.theme.Coral
import com.example.attendancetracker.theme.Indigo60
import com.example.attendancetracker.ui.home.HomeScreen
import com.example.attendancetracker.ui.settings.SettingsScreen
import com.example.attendancetracker.ui.stats.StatsScreen
import com.example.attendancetracker.ui.timetable.TimetableScreen

data class BottomNavItem(val label: String, val icon: ImageVector)

private val navItems = listOf(
    BottomNavItem("Home",     Icons.Default.Home),
    BottomNavItem("Schedule", Icons.Default.CalendarToday),
    BottomNavItem("Stats",    Icons.Default.BarChart),
)

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun MainNavigation() {
    val context = LocalContext.current
    val app = context.applicationContext as AttendanceApplication
    val themeMode by app.themePreferences.themeMode.collectAsStateWithLifecycle()
    val onboardingCompleted by app.themePreferences.onboardingCompleted.collectAsStateWithLifecycle()

    var selectedTab    by remember { mutableIntStateOf(0) }
    var showSettings   by remember { mutableStateOf(false) }
    var showExitDialog by remember { mutableStateOf(false) }

    // System Back Handling
    if (showSettings) {
        BackHandler {
            showSettings = false
            selectedTab  = 0
        }
    } else if (!onboardingCompleted) {
        BackHandler {
            app.themePreferences.setOnboardingCompleted(true)
        }
    } else {
        BackHandler {
            showExitDialog = true
        }
    }

    AttendanceTrackerTheme(mode = themeMode) {
        Box(modifier = Modifier.fillMaxSize()) {
            if (showExitDialog) {
                ExitAppDialog(
                    onDismiss = { showExitDialog = false },
                    onExit    = {
                        showExitDialog = false
                        (context as? Activity)?.finish()
                    }
                )
            }

            if (showSettings) {
                SettingsScreen(
                    onBack = {
                        showSettings = false
                        selectedTab  = 0
                    }
                )
            } else {
                Scaffold(
                    bottomBar = {
                        NavigationBar(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                            tonalElevation = androidx.compose.ui.unit.Dp.Unspecified
                        ) {
                            navItems.forEachIndexed { idx, item ->
                                val isSelected = idx == selectedTab
                                NavigationBarItem(
                                    selected = isSelected,
                                    onClick  = { selectedTab = idx },
                                    icon     = {
                                        Icon(
                                            imageVector        = item.icon,
                                            contentDescription = item.label
                                        )
                                    },
                                    label    = {
                                        Text(
                                            text       = item.label,
                                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                                        )
                                    },
                                    colors = NavigationBarItemDefaults.colors(
                                        selectedIconColor   = Indigo60,
                                        selectedTextColor   = Indigo60,
                                        indicatorColor      = Indigo60.copy(alpha = 0.18f),
                                        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                        unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                )
                            }
                        }
                    },
                    containerColor = MaterialTheme.colorScheme.background
                ) { innerPadding ->
                    Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
                        AnimatedContent(
                            targetState = selectedTab,
                            transitionSpec = {
                                fadeIn(animationSpec = tween(250)) togetherWith fadeOut(animationSpec = tween(200))
                            },
                            label = "tab_switch"
                        ) { targetTab ->
                            when (targetTab) {
                                0 -> HomeScreen(onOpenSettings = { showSettings = true })
                                1 -> TimetableScreen()
                                2 -> StatsScreen()
                            }
                        }
                    }
                }
            }

            // Interactive Onboarding Tutorial Overlay
            if (!onboardingCompleted && !showSettings) {
                com.example.attendancetracker.ui.onboarding.TutorialOverlay(
                    onNavigateTab = { tabIndex -> selectedTab = tabIndex },
                    onDismiss = { app.themePreferences.setOnboardingCompleted(true) }
                )
            }
        }
    }
}

@Composable
private fun ExitAppDialog(
    onDismiss: () -> Unit,
    onExit: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape  = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(
                modifier            = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text       = "Exit App?",
                    style      = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color      = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text  = "Are you sure you want to exit the application?",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier              = Modifier.fillMaxWidth().padding(top = 8.dp)
                ) {
                    OutlinedButton(
                        onClick  = onDismiss,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Cancel")
                    }
                    Button(
                        onClick  = onExit,
                        colors   = ButtonDefaults.buttonColors(containerColor = Coral),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Exit")
                    }
                }
            }
        }
    }
}
