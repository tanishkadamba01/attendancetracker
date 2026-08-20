package com.example.attendancetracker.ui.stats

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.attendancetracker.data.model.AttendanceStatus
import com.example.attendancetracker.theme.Amber
import com.example.attendancetracker.theme.Coral
import com.example.attendancetracker.theme.Indigo60
import com.example.attendancetracker.theme.Mint
import kotlin.math.min

@Composable
fun StatsScreen(modifier: Modifier = Modifier) {
    val vm: StatsViewModel = viewModel()
    val subjectStats by vm.subjectStats.collectAsStateWithLifecycle()
    val haptic = LocalHapticFeedback.current

    var selectedSubjectForDetails by remember { mutableStateOf<SubjectStats?>(null) }
    var selectedStatsTab by remember { mutableStateOf(0) } // 0 = Overview, 1 = Calendar

    if (selectedSubjectForDetails != null) {
        SubjectDetailScreen(
            ss     = selectedSubjectForDetails!!,
            vm     = vm,
            onBack = { selectedSubjectForDetails = null }
        )
    } else {
        Column(
            modifier = modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Header with Segmented Tab Control
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                MaterialTheme.colorScheme.surface,
                                MaterialTheme.colorScheme.background
                            )
                        )
                    )
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text       = if (selectedStatsTab == 0) "Subject Statistics" else "Attendance Calendar",
                    style      = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color      = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text  = if (selectedStatsTab == 0) "Track individual performance per subject" else "Daily attendance history and timeline",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // Segmented Tab Selector
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    val isOverview = selectedStatsTab == 0
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (isOverview) Indigo60 else Color.Transparent)
                            .clickable {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                selectedStatsTab = 0
                            }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Overview",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = if (isOverview) FontWeight.Bold else FontWeight.Normal,
                            color = if (isOverview) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    val isCalendar = selectedStatsTab == 1
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (isCalendar) Indigo60 else Color.Transparent)
                            .clickable {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                selectedStatsTab = 1
                            }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Calendar",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = if (isCalendar) FontWeight.Bold else FontWeight.Normal,
                            color = if (isCalendar) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            if (selectedStatsTab == 0) {
                LazyColumn(
                    modifier            = Modifier.fillMaxSize(),
                    contentPadding      = PaddingValues(bottom = 32.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    if (subjectStats.isNotEmpty()) {
                        items(subjectStats, key = { it.subject.id }) { ss ->
                            AnimatedVisibility(
                                visible = true,
                                enter   = fadeIn(tween(300)) + slideInVertically(tween(300))
                            ) {
                                SubjectStatCard(
                                    ss      = ss,
                                    onClick = {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        selectedSubjectForDetails = ss
                                    }
                                )
                            }
                        }
                    } else {
                        item {
                            EngagingStatsEmptyState()
                        }
                    }
                }
            } else {
                CalendarScreen(vm = vm)
            }
        }
    }
}

@Composable
private fun SubjectStatCard(
    ss: SubjectStats,
    onClick: () -> Unit
) {
    val color = try {
        Color(android.graphics.Color.parseColor(ss.subject.colorHex))
    } catch (e: Exception) { Indigo60 }

    val animatedPct by animateFloatAsState(
        targetValue   = if (ss.total > 0) ss.percentage / 100f else 0f,
        animationSpec = tween(800, easing = FastOutSlowInEasing),
        label         = "subject_pct_${ss.subject.id}"
    )

    val targetPct = ss.subject.targetPercentage

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clickable(onClick = onClick),
        shape    = RoundedCornerShape(16.dp),
        colors   = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        border   = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment     = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment     = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(modifier = Modifier.size(12.dp).clip(CircleShape).background(color))
                    Column {
                        Text(
                            text       = ss.subject.name,
                            fontWeight = FontWeight.Bold,
                            style      = MaterialTheme.typography.bodyLarge,
                            color      = MaterialTheme.colorScheme.onBackground
                        )
                        if (ss.subject.code.isNotBlank()) {
                            Text(
                                text  = ss.subject.code,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text       = "%.1f%%".format(ss.percentage),
                        fontWeight = FontWeight.Bold,
                        style      = MaterialTheme.typography.titleMedium,
                        color      = when {
                            ss.percentage >= targetPct -> Mint
                            ss.percentage >= targetPct - 10f -> Amber
                            else -> Coral
                        }
                    )
                    Icon(Icons.Default.ChevronRight, contentDescription = "Details", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            // Progress bar
            Box(
                modifier = Modifier.fillMaxWidth().height(6.dp)
                    .clip(RoundedCornerShape(3.dp)).background(MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
            ) {
                Box(
                    modifier = Modifier.fillMaxWidth(animatedPct).height(6.dp)
                        .clip(RoundedCornerShape(3.dp)).background(color)
                )
            }

            // Metrics row
            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text  = "Attended: ${ss.attended} | Missed: ${ss.missed} | Total: ${ss.total}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text       = "Target: %.0f%%".format(targetPct),
                    style      = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.SemiBold,
                    color      = Indigo60
                )
            }

            // Target Requirement Info
            if (ss.total > 0) {
                if (ss.requiredToTarget > 0) {
                    Text(
                        text       = "⚠️ Attend ${ss.requiredToTarget} more class${if (ss.requiredToTarget == 1) "" else "es"} to reach %.0f%%".format(targetPct),
                        style      = MaterialTheme.typography.bodySmall,
                        color      = Coral,
                        fontWeight = FontWeight.SemiBold
                    )
                } else {
                    val safeSkips = ss.safeToSkip
                    Text(
                        text       = if (safeSkips > 0) "✅ Can miss $safeSkips more class${if (safeSkips == 1) "" else "es"}" else "🎯 Right on target!",
                        style      = MaterialTheme.typography.bodySmall,
                        color      = Mint,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

@Composable
private fun EngagingStatsEmptyState() {
    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
        shape    = RoundedCornerShape(20.dp),
        colors   = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        border   = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
    ) {
        Column(
            modifier            = Modifier.padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("📈", fontSize = 54.sp)
            Text(
                text       = "No Statistics Available",
                style      = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color      = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text      = "Add your subjects in the Schedule tab to start tracking detailed attendance metrics and safe skips!",
                style     = MaterialTheme.typography.bodyMedium,
                color     = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

// ─── Subject Details Subscreen ───────────────────────────────────────────────

@Composable
private fun SubjectDetailScreen(
    ss: SubjectStats,
    vm: StatsViewModel,
    onBack: () -> Unit
) {
    BackHandler(onBack = onBack)

    val historyLog by vm.getSubjectHistoryLog(ss.subject.id).collectAsStateWithLifecycle(initialValue = emptyList())
    val color = try { Color(android.graphics.Color.parseColor(ss.subject.colorHex)) } catch (e: Exception) { Indigo60 }
    val targetPct = ss.subject.targetPercentage

    val animatedPct by animateFloatAsState(
        targetValue   = ss.percentage / 100f,
        animationSpec = tween(1000, easing = FastOutSlowInEasing),
        label         = "detail_circle"
    )

    Column(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        // Header with Back Button
        Row(
            modifier          = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = MaterialTheme.colorScheme.onBackground)
            }
            Spacer(modifier = Modifier.width(8.dp))
            Box(modifier = Modifier.size(14.dp).clip(CircleShape).background(color))
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Text(ss.subject.name, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
                if (ss.subject.code.isNotBlank()) {
                    Text(ss.subject.code, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }

        LazyColumn(
            contentPadding      = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Subject Gauge & Overview Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape    = RoundedCornerShape(20.dp),
                    colors   = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    border   = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                ) {
                    Column(
                        modifier            = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        val trackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                        Box(contentAlignment = Alignment.Center) {
                            Canvas(modifier = Modifier.size(140.dp)) {
                                val stroke   = 14.dp.toPx()
                                val diameter = min(size.width, size.height) - stroke
                                val tl       = Offset((size.width - diameter) / 2f, (size.height - diameter) / 2f)
                                val sz       = Size(diameter, diameter)
                                drawArc(trackColor, -90f, 360f, false, tl, sz, style = Stroke(stroke, cap = StrokeCap.Round))
                                drawArc(color, -90f, 360f * animatedPct, false, tl, sz, style = Stroke(stroke, cap = StrokeCap.Round))
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("%.1f%%".format(ss.percentage), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.ExtraBold, color = color)
                                Text("Target: %.0f%%".format(targetPct), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }

                        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround) {
                            DetailMetric("Attended", ss.attended.toString(), Mint)
                            DetailMetric("Missed", ss.missed.toString(), Coral)
                            DetailMetric("Total", ss.total.toString(), Indigo60)
                        }

                        // Target Info Banner
                        val bannerMsg = when {
                            ss.total == 0 -> "No class records yet for this subject"
                            ss.requiredToTarget > 0 -> "Attend ${ss.requiredToTarget} more consecutive class${if (ss.requiredToTarget == 1) "" else "es"} to reach %.0f%% target".format(targetPct)
                            ss.safeToSkip > 0 -> "You can safely miss ${ss.safeToSkip} more class${if (ss.safeToSkip == 1) "" else "es"} while staying above target"
                            else -> "Right on target! Any missed class will drop you below %.0f%%".format(targetPct)
                        }
                        Text(
                            text       = bannerMsg,
                            style      = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold,
                            color      = if (ss.requiredToTarget > 0) Coral else Mint,
                            textAlign  = TextAlign.Center,
                            modifier   = Modifier.fillMaxWidth().background(if (ss.requiredToTarget > 0) Coral.copy(alpha = 0.1f) else Mint.copy(alpha = 0.1f), RoundedCornerShape(10.dp)).padding(10.dp)
                        )
                    }
                }
            }

            // Attendance Predictions & What-If Planner
            item {
                AttendancePredictionCard(
                    currentTotal = ss.total,
                    currentAttended = ss.attended,
                    targetPercentage = targetPct,
                    vm = vm
                )
            }

            // Attendance History Log
            item {
                Text("Attendance History Log", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
            }

            if (historyLog.isEmpty()) {
                item {
                    Text("No attendance history recorded yet.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                items(historyLog) { log ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment     = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(log.date, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onBackground)
                            Text(log.slotTime, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }

                        val (statusText, statusColor) = when (log.status) {
                            AttendanceStatus.PRESENT    -> Pair("Attended", Mint)
                            AttendanceStatus.ABSENT     -> Pair("Missed", Coral)
                            AttendanceStatus.REASSIGNED -> Pair("Reassigned", Amber)
                            AttendanceStatus.CANCELLED  -> Pair("Cancelled", MaterialTheme.colorScheme.onSurfaceVariant)
                            AttendanceStatus.PENDING    -> Pair("Pending", Indigo60)
                        }

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(statusColor.copy(alpha = 0.15f))
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text(statusText, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = statusColor)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DetailMetric(label: String, value: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold, color = color)
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun AttendancePredictionCard(
    currentTotal: Int,
    currentAttended: Int,
    targetPercentage: Float,
    vm: StatsViewModel
) {
    var extraAttended by remember { mutableStateOf(0) }
    var extraMissed by remember { mutableStateOf(0) }
    val haptic = LocalHapticFeedback.current

    val projection = remember(currentTotal, currentAttended, targetPercentage, extraAttended, extraMissed) {
        vm.calculateProjection(
            currentTotal     = currentTotal,
            currentAttended  = currentAttended,
            targetPercentage = targetPercentage,
            extraAttended    = extraAttended,
            extraMissed      = extraMissed
        )
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape    = RoundedCornerShape(20.dp),
        colors   = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        border   = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = "Attendance Simulator",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "Plan future attendance & test what-if scenarios",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))

            // Stepper controls
            PredictionStepper(
                label = "If I attend next classes",
                count = extraAttended,
                color = Mint,
                onDecrease = {
                    if (extraAttended > 0) {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        extraAttended--
                    }
                },
                onIncrease = {
                    if (extraAttended < 30) {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        extraAttended++
                    }
                }
            )

            PredictionStepper(
                label = "If I miss next classes",
                count = extraMissed,
                color = Coral,
                onDecrease = {
                    if (extraMissed > 0) {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        extraMissed--
                    }
                },
                onIncrease = {
                    if (extraMissed < 30) {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        extraMissed++
                    }
                }
            )

            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))

            // Live simulated output
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        if (projection.isTargetMet) Mint.copy(alpha = 0.12f)
                        else Coral.copy(alpha = 0.12f)
                    )
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Simulated Attendance",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "%.1f%%".format(projection.projectedPercentage),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.ExtraBold,
                            color = if (projection.isTargetMet) Mint else Coral
                        )
                        if (extraAttended > 0 || extraMissed > 0) {
                            val sign = if (projection.deltaPct >= 0) "+" else ""
                            Text(
                                text = "($sign%.1f%%)".format(projection.deltaPct),
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold,
                                color = if (projection.deltaPct >= 0) Mint else Coral
                            )
                        }
                    }
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (projection.isTargetMet) Mint.copy(alpha = 0.2f) else Coral.copy(alpha = 0.2f))
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = if (projection.isTargetMet) "Target Met (%.0f%%)".format(targetPercentage) else "Below Target",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (projection.isTargetMet) Mint else Coral
                    )
                }
            }

            if (extraAttended > 0 || extraMissed > 0) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    Text(
                        text = "Reset Simulation",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = Indigo60,
                        modifier = Modifier
                            .clickable {
                                extraAttended = 0
                                extraMissed = 0
                            }
                            .padding(4.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun PredictionStepper(
    label: String,
    count: Int,
    color: Color,
    onDecrease: () -> Unit,
    onIncrease: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(color))
            Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onBackground)
        }

        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilledTonalIconButton(
                onClick = onDecrease,
                enabled = count > 0,
                modifier = Modifier.size(32.dp),
                shape = RoundedCornerShape(8.dp),
                colors = IconButtonDefaults.filledTonalIconButtonColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            ) {
                Icon(Icons.Default.Remove, contentDescription = "Decrease", modifier = Modifier.size(16.dp))
            }

            Text(
                text = "+$count",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = if (count > 0) color else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.width(36.dp),
                textAlign = TextAlign.Center
            )

            FilledTonalIconButton(
                onClick = onIncrease,
                modifier = Modifier.size(32.dp),
                shape = RoundedCornerShape(8.dp),
                colors = IconButtonDefaults.filledTonalIconButtonColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            ) {
                Icon(Icons.Default.Add, contentDescription = "Increase", modifier = Modifier.size(16.dp))
            }
        }
    }
}
