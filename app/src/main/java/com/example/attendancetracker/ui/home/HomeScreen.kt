package com.example.attendancetracker.ui.home

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateIntAsState
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
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.attendancetracker.data.model.AttendanceStatus
import com.example.attendancetracker.data.model.Subject
import com.example.attendancetracker.theme.Amber
import com.example.attendancetracker.theme.Coral
import com.example.attendancetracker.theme.Indigo60
import com.example.attendancetracker.theme.Mint
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle as JavaTextStyle
import java.util.Locale
import kotlin.math.min

@Composable
fun HomeScreen(
    onOpenSettings: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val vm: HomeViewModel = viewModel()
    val state by vm.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current

    var showSundayPrompt by remember { mutableStateOf(vm.shouldShowSundayUpdatePrompt()) }

    if (showSundayPrompt) {
        SundayUpdatePromptDialog(
            onDismiss = {
                vm.dismissSundayUpdatePrompt()
                showSundayPrompt = false
            },
            onVisitWebsite = {
                vm.dismissSundayUpdatePrompt()
                showSundayPrompt = false
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://theattendancetracker.vercel.app"))
                context.startActivity(intent)
            }
        )
    }

    LazyColumn(
        modifier            = modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
        contentPadding      = PaddingValues(bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            HomeHeader(
                state          = state,
                onDateSelected = { date ->
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    vm.setDate(date)
                },
                onPrevWeek     = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    vm.previousWeek()
                },
                onNextWeek     = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    vm.nextWeek()
                },
                onOpenSettings = onOpenSettings
            )
        }
        item { SummaryCard(state = state) }
        item {
            Text(
                text       = "Today's Schedule",
                style      = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color      = MaterialTheme.colorScheme.onBackground,
                modifier   = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
            )
        }
        if (state.slotsWithDetails.isEmpty()) {
            item { EngagingHomeEmptyState() }
        } else {
            items(state.slotsWithDetails, key = { it.slot.id }) { swd ->
                AnimatedVisibility(
                    visible = true,
                    enter   = fadeIn(tween(300)) + slideInVertically(tween(300))
                ) {
                    ClassCard(
                        swd               = swd,
                        allSubjects       = state.allSubjects,
                        isAfter5PM        = state.isAfter5PM,
                        isBeforeStart     = state.isBeforeStartDate,
                        onToggleMissed    = { isMissed ->
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            vm.toggleMissed(swd.slot.id, isMissed)
                        },
                        onToggleCancelled = { isCancelled ->
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            vm.toggleCancelled(swd.slot.id, isCancelled)
                        },
                        onReassign        = { targetSubId ->
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            vm.reassignSlot(swd.slot.id, targetSubId)
                        },
                        onRevert          = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            vm.revertSlot(swd.slot.id)
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun HomeHeader(
    state: HomeUiState,
    onDateSelected: (LocalDate) -> Unit,
    onPrevWeek: () -> Unit,
    onNextWeek: () -> Unit,
    onOpenSettings: () -> Unit
) {
    val today      = LocalDate.now()
    val monday     = state.selectedDate.with(DayOfWeek.MONDAY)
    val sunday     = monday.plusDays(6)
    val weekDays   = (0..6).map { monday.plusDays(it.toLong()) }
    val monthFmt   = DateTimeFormatter.ofPattern("d MMM")

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
            .padding(16.dp)
    ) {
        Row(
            modifier              = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment     = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text       = "Attendance",
                    style      = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color      = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text  = "${monday.format(monthFmt)} – ${sunday.format(monthFmt)} ${sunday.year}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(onClick = onOpenSettings) {
                Icon(
                    imageVector        = Icons.Default.Settings,
                    contentDescription = "Settings",
                    tint               = MaterialTheme.colorScheme.onBackground
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Week Navigation Bar
        Row(
            modifier              = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment     = Alignment.CenterVertically
        ) {
            IconButton(onClick = onPrevWeek, enabled = state.canGoPrevWeek, modifier = Modifier.size(32.dp)) {
                Icon(
                    imageVector        = Icons.Default.ChevronLeft,
                    contentDescription = "Previous Week",
                    tint               = if (state.canGoPrevWeek) Indigo60 else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                )
            }
            Text(
                text       = if (monday <= today && today <= sunday) "This Week" else "${monday.format(monthFmt)} Week",
                style      = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color      = Indigo60
            )
            IconButton(onClick = onNextWeek, modifier = Modifier.size(32.dp)) {
                Icon(Icons.Default.ChevronRight, contentDescription = "Next Week", tint = Indigo60)
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // 7-Day Mon-Sun Calendar Slider
        Row(
            modifier              = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            weekDays.forEach { day ->
                DayChip(
                    date       = day,
                    isSelected = day == state.selectedDate,
                    isToday    = day == today,
                    onClick    = { onDateSelected(day) }
                )
            }
        }
    }
}

@Composable
private fun DayChip(date: LocalDate, isSelected: Boolean, isToday: Boolean, onClick: () -> Unit) {
    val dayName = date.dayOfWeek.getDisplayName(JavaTextStyle.SHORT, Locale.getDefault())
    val bgColor = when {
        isSelected -> Indigo60
        isToday    -> MaterialTheme.colorScheme.primaryContainer
        else       -> MaterialTheme.colorScheme.surfaceVariant
    }
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(bgColor)
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text  = dayName,
            style = MaterialTheme.typography.labelSmall,
            color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text       = date.dayOfMonth.toString(),
            style      = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color      = if (isSelected) Color.White else MaterialTheme.colorScheme.onBackground
        )
        if (isToday && !isSelected) {
            Spacer(modifier = Modifier.height(4.dp))
            Box(modifier = Modifier.size(4.dp).background(Indigo60, CircleShape))
        }
    }
}

@Composable
private fun SummaryCard(state: HomeUiState) {
    val pctFraction = if (state.totalClasses > 0) state.attendedClasses.toFloat() / state.totalClasses else 0f

    val animTotal by animateIntAsState(targetValue = state.totalClasses, animationSpec = tween(500), label = "tot")
    val animAttended by animateIntAsState(targetValue = state.attendedClasses, animationSpec = tween(500), label = "att")

    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        shape    = RoundedCornerShape(20.dp),
        colors   = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        border   = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
    ) {
        Row(modifier = Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
            AttendanceCircle(percentage = pctFraction, modifier = Modifier.size(90.dp))
            Spacer(modifier = Modifier.width(24.dp))
            Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.weight(1f)) {
                Text("Selected Day Stats", style = MaterialTheme.typography.labelSmall, color = Indigo60, fontWeight = FontWeight.Bold)
                StatRow("Total Classes", animTotal.toString())
                StatRow("Attended",      animAttended.toString())
                StatRow("Attendance",    "%.1f%%".format(state.attendancePercentage))
            }
        }
    }
}

@Composable
private fun AttendanceCircle(percentage: Float, modifier: Modifier = Modifier) {
    val animatedPct by animateFloatAsState(
        targetValue   = percentage,
        animationSpec = tween(1000, easing = FastOutSlowInEasing),
        label         = "circle_pct"
    )
    val color = when {
        percentage >= 0.85f -> Mint
        percentage >= 0.75f -> Amber
        percentage > 0f     -> Coral
        else                -> Indigo60
    }
    val trackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)

    Canvas(modifier = modifier) {
        val stroke   = 10.dp.toPx()
        val diameter = min(size.width, size.height) - stroke
        val topLeft  = Offset((size.width - diameter) / 2f, (size.height - diameter) / 2f)
        val arcSize  = Size(diameter, diameter)
        drawArc(trackColor, -90f, 360f, false, topLeft, arcSize,
            style = Stroke(stroke, cap = StrokeCap.Round))
        drawArc(color, -90f, 360f * animatedPct, false, topLeft, arcSize,
            style = Stroke(stroke, cap = StrokeCap.Round))
    }
}

@Composable
private fun StatRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(
            text  = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text       = value,
            style      = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.SemiBold,
            color      = MaterialTheme.colorScheme.onBackground
        )
    }
}

@Composable
private fun ClassCard(
    swd: SlotWithDetails,
    allSubjects: List<Subject>,
    isAfter5PM: Boolean,
    isBeforeStart: Boolean,
    onToggleMissed: (Boolean) -> Unit,
    onToggleCancelled: (Boolean) -> Unit,
    onReassign: (Int) -> Unit,
    onRevert: () -> Unit
) {
    val originalSubject = swd.subject
    val overrideSubject = swd.overrideSubject
    val isCancelled     = swd.record?.status == AttendanceStatus.CANCELLED
    val isReassigned    = swd.record?.status == AttendanceStatus.REASSIGNED && overrideSubject != null
    val isMissed        = swd.record?.status == AttendanceStatus.ABSENT
    val isExplicitPresent = swd.record?.status == AttendanceStatus.PRESENT

    val activeSubject = if (isReassigned) overrideSubject else originalSubject
    val subjectColor = try {
        Color(android.graphics.Color.parseColor(activeSubject?.colorHex ?: "#6650A4"))
    } catch (e: Exception) { Indigo60 }

    var showReassignDialog by remember { mutableStateOf(false) }

    if (showReassignDialog) {
        ReassignSubjectDialog(
            originalSubject  = originalSubject,
            allSubjects      = allSubjects,
            onDismiss        = { showReassignDialog = false },
            onSelectSubject  = { targetId ->
                onReassign(targetId)
                showReassignDialog = false
            },
            onRevertOriginal = {
                onRevert()
                showReassignDialog = false
            }
        )
    }

    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        shape    = RoundedCornerShape(16.dp),
        colors   = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        border   = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier.width(4.dp).height(44.dp)
                        .clip(RoundedCornerShape(2.dp)).background(if (isCancelled) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f) else subjectColor)
                )
                Spacer(modifier = Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text       = originalSubject?.name ?: "Unknown Subject",
                        style      = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold,
                        color      = if (isCancelled) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text  = "${swd.slot.startTime} – ${swd.slot.endTime}${if (swd.slot.room.isNotBlank()) " • ${swd.slot.room}" else ""}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                StatusBadge(
                    isCancelled       = isCancelled,
                    isMissed          = isMissed,
                    isReassigned      = isReassigned,
                    isExplicitPresent = isExplicitPresent,
                    isAfter5PM        = isAfter5PM,
                    isBeforeStart     = isBeforeStart,
                    overrideSubject   = overrideSubject
                )
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))

            // Action Row 1: Missed Class + Class Cancelled
            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment     = Alignment.CenterVertically
            ) {
                // Missed button
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (isMissed) Coral.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surface)
                        .border(1.dp, if (isMissed) Coral else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f), RoundedCornerShape(10.dp))
                        .clickable { onToggleMissed(isMissed) }
                        .padding(vertical = 9.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment     = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector        = if (isMissed) Icons.Default.CheckCircle else Icons.Default.Cancel,
                            contentDescription = null,
                            tint               = if (isMissed) Mint else Coral,
                            modifier           = Modifier.size(15.dp)
                        )
                        Text(
                            text       = if (isMissed) "Mark Attended" else "Missed Class",
                            style      = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color      = if (isMissed) Mint else Coral
                        )
                    }
                }

                // Cancelled button
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (isCancelled) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.18f) else MaterialTheme.colorScheme.surface)
                        .border(1.dp, if (isCancelled) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f), RoundedCornerShape(10.dp))
                        .clickable { onToggleCancelled(isCancelled) }
                        .padding(vertical = 9.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment     = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector        = if (isCancelled) Icons.Default.CheckCircle else Icons.Default.Block,
                            contentDescription = null,
                            tint               = if (isCancelled) Mint else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier           = Modifier.size(15.dp)
                        )
                        Text(
                            text       = if (isCancelled) "Uncancel" else "Class Cancelled",
                            style      = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color      = if (isCancelled) Mint else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Action Row 2: Taken by Another Subject
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (isReassigned) Amber.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surface)
                    .border(1.dp, if (isReassigned) Amber else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f), RoundedCornerShape(10.dp))
                    .clickable { showReassignDialog = true }
                    .padding(vertical = 9.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment     = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector        = Icons.Default.SwapHoriz,
                        contentDescription = null,
                        tint               = if (isReassigned) Amber else Indigo60,
                        modifier           = Modifier.size(16.dp)
                    )
                    Text(
                        text       = if (isReassigned) "Change Reassignment (Taken by ${overrideSubject?.name ?: "Other"})" else "Taken by Another Subject",
                        style      = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color      = if (isReassigned) Amber else Indigo60
                    )
                }
            }
        }
    }
}

@Composable
private fun StatusBadge(
    isCancelled: Boolean,
    isMissed: Boolean,
    isReassigned: Boolean,
    isExplicitPresent: Boolean,
    isAfter5PM: Boolean,
    isBeforeStart: Boolean,
    overrideSubject: Subject?
) {
    val (text, bgColor, textColor) = when {
        isBeforeStart -> Triple("⏳ Before Start Day", Indigo60.copy(alpha = 0.15f), Indigo60)
        isCancelled -> Triple("🚫 Cancelled", MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f), MaterialTheme.colorScheme.onSurfaceVariant)
        isMissed -> Triple("Missed", Coral.copy(alpha = 0.2f), Coral)
        isReassigned -> Triple("Taken by ${overrideSubject?.name ?: "Other"}", Amber.copy(alpha = 0.2f), Amber)
        isExplicitPresent || isAfter5PM -> Triple("✓ Attended", Mint.copy(alpha = 0.18f), Mint)
        else -> Triple("⏳ Pending (5 PM Auto)", Indigo60.copy(alpha = 0.18f), Indigo60)
    }

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(bgColor)
            .border(1.dp, textColor.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        Text(
            text       = text,
            style      = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color      = textColor
        )
    }
}

@Composable
private fun EngagingHomeEmptyState() {
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
            Text("🎉", fontSize = 54.sp)
            Text(
                text       = "No Classes Today!",
                style      = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color      = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text      = "You have a free day today. Enjoy your break or manage your weekly timetable in the Schedule tab.",
                style     = MaterialTheme.typography.bodyMedium,
                color     = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun ReassignSubjectDialog(
    originalSubject: Subject?,
    allSubjects: List<Subject>,
    onDismiss: () -> Unit,
    onSelectSubject: (Int) -> Unit,
    onRevertOriginal: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape  = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text       = "Class Taken by Another Subject",
                    style      = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color      = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text  = "Select which subject actually took this class slot:",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier            = Modifier.height(220.dp)
                ) {
                    val otherSubjects = allSubjects.filter { it.id != originalSubject?.id }
                    if (otherSubjects.isEmpty()) {
                        item {
                            Text(
                                text  = "No other subjects available. Please add another subject first.",
                                style = MaterialTheme.typography.bodySmall,
                                color = Coral
                            )
                        }
                    } else {
                        items(otherSubjects, key = { it.id }) { sub ->
                            val subColor = try { Color(android.graphics.Color.parseColor(sub.colorHex)) } catch (e: Exception) { Indigo60 }
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(MaterialTheme.colorScheme.surface)
                                    .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                                    .clickable { onSelectSubject(sub.id) }
                                    .padding(14.dp),
                                verticalAlignment     = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Box(modifier = Modifier.size(12.dp).clip(CircleShape).background(subColor))
                                Text(
                                    text       = sub.name,
                                    style      = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color      = MaterialTheme.colorScheme.onBackground,
                                    modifier   = Modifier.weight(1f)
                                )
                                Text("Select", style = MaterialTheme.typography.labelSmall, color = Indigo60)
                            }
                        }
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    OutlinedButton(
                        onClick  = onRevertOriginal,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Revert to Original", fontSize = 12.sp)
                    }
                    Button(onClick = onDismiss, modifier = Modifier.weight(1f)) {
                        Text("Cancel")
                    }
                }
            }
        }
    }
}

@Composable
private fun SundayUpdatePromptDialog(
    onDismiss: () -> Unit,
    onVisitWebsite: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape  = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            border = BorderStroke(1.dp, Indigo60.copy(alpha = 0.3f))
        ) {
            Column(
                modifier            = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(Indigo60.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector        = Icons.Default.SystemUpdate,
                        contentDescription = null,
                        tint               = Indigo60,
                        modifier           = Modifier.size(30.dp)
                    )
                }

                Text(
                    text       = "Weekly App Update",
                    style      = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.ExtraBold,
                    color      = MaterialTheme.colorScheme.onBackground,
                    textAlign  = TextAlign.Center
                )

                Text(
                    text       = "Check for the latest version and features of Attendance Tracker on our official website.",
                    style      = MaterialTheme.typography.bodyMedium,
                    color      = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign  = TextAlign.Center,
                    lineHeight = 20.sp
                )

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    modifier              = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick  = onDismiss,
                        modifier = Modifier.weight(1f),
                        shape    = RoundedCornerShape(12.dp)
                    ) {
                        Text("Dismiss", color = MaterialTheme.colorScheme.onBackground)
                    }
                    Button(
                        onClick  = onVisitWebsite,
                        modifier = Modifier.weight(1.2f),
                        colors   = ButtonDefaults.buttonColors(containerColor = Indigo60),
                        shape    = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.OpenInBrowser, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Visit Website", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                    }
                }
            }
        }
    }
}
