package com.timecure.app.ui.screens.missions

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsWalk
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.timecure.app.model.WakeMissionType

fun missionIcon(type: WakeMissionType): ImageVector = when (type) {
    WakeMissionType.ITEM_HUNT -> Icons.Default.Search
    WakeMissionType.SAY_WORD_ON_BEAT -> Icons.Default.RecordVoiceOver
    WakeMissionType.FIND_COLOR_TILES -> Icons.Default.GridView
    WakeMissionType.TYPING -> Icons.Default.Keyboard
    WakeMissionType.MATH -> Icons.Default.Calculate
    WakeMissionType.STEP -> Icons.AutoMirrored.Filled.DirectionsWalk
}

fun missionTileBg(type: WakeMissionType): Color = when (type) {
    WakeMissionType.ITEM_HUNT, WakeMissionType.SAY_WORD_ON_BEAT -> Color(0xFF5D1D2C)
    WakeMissionType.FIND_COLOR_TILES, WakeMissionType.TYPING, WakeMissionType.MATH -> Color(0xFF0E3C49)
    WakeMissionType.STEP -> Color(0xFF39274E)
}

fun missionTileFg(type: WakeMissionType): Color = when (type) {
    WakeMissionType.ITEM_HUNT, WakeMissionType.SAY_WORD_ON_BEAT -> Color(0xFFFFD8DF)
    WakeMissionType.FIND_COLOR_TILES, WakeMissionType.TYPING, WakeMissionType.MATH -> Color(0xFF7BE3FF)
    WakeMissionType.STEP -> Color(0xFFD9B8FF)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MissionPickerSheet(
    onSelectType: (WakeMissionType) -> Unit,
    onDismiss: () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val sections = WakeMissionType.entries.groupBy { it.section }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Spacer(Modifier.size(40.dp))
                Text(
                    text = "Mission",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Close mission picker")
                }
            }

            sections.forEach { (sectionTitle, types) ->
                Text(
                    text = sectionTitle,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp)
                )
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    types.forEach { type ->
                        Surface(
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onSelectType(type)
                            },
                            color = MaterialTheme.colorScheme.surfaceContainerLow,
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    color = missionTileBg(type),
                                    contentColor = missionTileFg(type),
                                    shape = RoundedCornerShape(14.dp),
                                    modifier = Modifier.size(48.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = missionIcon(type),
                                            contentDescription = null,
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }
                                }
                                Spacer(Modifier.width(14.dp))
                                Text(
                                    text = type.displayName,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                                type.badge?.let { badgeText ->
                                    Spacer(Modifier.width(10.dp))
                                    val badgeContainer = when (badgeText) {
                                        "HOT" -> Color(0xFF4B1E24)
                                        "NEW" -> Color(0xFF4C2B10)
                                        else -> Color(0xFF453512)
                                    }
                                    val badgeContent = when (badgeText) {
                                        "HOT" -> Color(0xFFFF7B89)
                                        "NEW" -> Color(0xFFFFB266)
                                        else -> Color(0xFFF6CB52)
                                    }
                                    Surface(
                                        color = badgeContainer,
                                        contentColor = badgeContent,
                                        shape = RoundedCornerShape(6.dp)
                                    ) {
                                        Text(
                                            text = badgeText,
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}
