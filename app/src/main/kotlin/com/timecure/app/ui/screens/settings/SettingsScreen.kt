package com.timecure.app.ui.screens.settings

import android.os.Build
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.timecure.app.TimecureApp
import com.timecure.app.alarm.AlarmActions
import com.timecure.app.alarm.AlarmScheduler
import com.timecure.app.data.local.PreferencesManager
import com.timecure.app.data.local.ThemeMode
import com.timecure.app.model.ChallengeDifficulty
import com.timecure.app.ui.components.safeTopBarWindowInsets
import com.timecure.app.ui.theme.AppThemePalette
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(
    themeMode: ThemeMode,
    themePalette: AppThemePalette,
    dynamicColor: Boolean,
    missingPermissions: Int,
    onThemeModeChange: (ThemeMode) -> Unit,
    onThemePaletteChange: (AppThemePalette) -> Unit,
    onDynamicColorChange: (Boolean) -> Unit,
    onUserNameChange: (String) -> Unit,
    onOpenPermissions: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val container = (context.applicationContext as TimecureApp).container
    val prefs = container.preferences

    var userName by remember { mutableStateOf(prefs.userName) }
    var defaultGoal by remember { mutableStateOf(prefs.defaultGoal) }
    var defaultDifficulty by remember { mutableStateOf(prefs.defaultDifficulty) }

    var aiEnabled by remember { mutableStateOf(prefs.aiEnabled) }
    var gatewayUrl by remember { mutableStateOf(prefs.gatewayUrl) }
    var apiKey by remember { mutableStateOf(prefs.apiKey) }
    var showApiKey by remember { mutableStateOf(false) }
    var modelName by remember { mutableStateOf(prefs.modelName) }
    var modelMenuExpanded by remember { mutableStateOf(false) }
    var systemInstructions by remember { mutableStateOf(prefs.aiSystemInstructions) }
    var lastStatus by remember { mutableStateOf(prefs.lastAiStatus) }
    var isTestingAi by remember { mutableStateOf(false) }
    var testResultOk by remember { mutableStateOf<Boolean?>(null) }

    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Settings") },
                windowInsets = safeTopBarWindowInsets()
            )
        },
        snackbarHost = { SnackbarHost(snackbar) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(start = 20.dp, end = 20.dp, top = 4.dp, bottom = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // ---------- Personalization ----------
            Text("Personalization", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)

            OutlinedTextField(
                value = userName,
                onValueChange = {
                    userName = it
                    prefs.userName = it
                    onUserNameChange(it)
                },
                label = { Text("Your name (optional)") },
                supportingText = { Text("Used for your morning greeting and wake-up victory card") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = defaultGoal,
                onValueChange = {
                    defaultGoal = it
                    prefs.defaultGoal = it
                },
                label = { Text("Default morning goal") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Column {
                Text("Default challenge difficulty", style = MaterialTheme.typography.labelLarge)
                Spacer(Modifier.height(6.dp))
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    val diffs = ChallengeDifficulty.entries
                    diffs.forEachIndexed { idx, d ->
                        SegmentedButton(
                            selected = defaultDifficulty == d,
                            onClick = {
                                defaultDifficulty = d
                                prefs.defaultDifficulty = d
                            },
                            shape = SegmentedButtonDefaults.itemShape(idx, diffs.size)
                        ) { Text(d.displayName) }
                    }
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            // ---------- Appearance & Themes ----------
            Text("Appearance & App Theme", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)

            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                val modes = ThemeMode.entries
                modes.forEachIndexed { idx, mode ->
                    SegmentedButton(
                        selected = themeMode == mode,
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onThemeModeChange(mode)
                        },
                        shape = SegmentedButtonDefaults.itemShape(idx, modes.size)
                    ) { Text(mode.label) }
                }
            }

            Text("Color Theme", style = MaterialTheme.typography.labelLarge)
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                maxItemsInEachRow = 2
            ) {
                AppThemePalette.entries.forEach { pal ->
                    val isSelected = !dynamicColor && themePalette == pal
                    val swatchInteraction = remember { MutableInteractionSource() }
                    val swatchPressed by swatchInteraction.collectIsPressedAsState()
                    val swatchScale by animateFloatAsState(
                        targetValue = if (swatchPressed) 0.96f else 1f,
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessHigh
                        ),
                        label = "theme_swatch_scale"
                    )
                    Surface(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            if (dynamicColor) onDynamicColorChange(false)
                            onThemePaletteChange(pal)
                        },
                        interactionSource = swatchInteraction,
                        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer
                        else MaterialTheme.colorScheme.surfaceContainerLow,
                        shape = MaterialTheme.shapes.medium,
                        border = BorderStroke(
                            width = if (isSelected) 2.dp else 1.dp,
                            color = if (isSelected) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.outlineVariant
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .graphicsLayer {
                                scaleX = swatchScale
                                scaleY = swatchScale
                            }
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                                    listOf(pal.previewPrimary, pal.previewSecondary, pal.previewTertiary).forEach { c ->
                                        Canvas(modifier = Modifier.size(16.dp)) {
                                            drawCircle(color = c)
                                        }
                                    }
                                }
                                if (isSelected) {
                                    Icon(
                                        Icons.Default.CheckCircle,
                                        contentDescription = "Selected theme",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                            Text(
                                text = pal.displayName,
                                style = MaterialTheme.typography.titleSmall,
                                color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer
                                else MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = pal.tagline,
                                style = MaterialTheme.typography.labelSmall,
                                color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                                else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                ListItem(
                    headlineContent = { Text("Dynamic wallpaper color") },
                    supportingContent = { Text("Override custom theme with your Android wallpaper palette") },
                    trailingContent = {
                        Switch(checked = dynamicColor, onCheckedChange = onDynamicColorChange)
                    }
                )
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            // ---------- Vercel AI Gateway ----------
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(8.dp))
                Text("Vercel AI Gateway", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
            }

            ListItem(
                headlineContent = { Text("Enable AI Coach challenges") },
                supportingContent = { Text("Generates goal-tailored puzzles online; local engine backs it up offline") },
                trailingContent = {
                    Switch(
                        checked = aiEnabled,
                        onCheckedChange = {
                            aiEnabled = it
                            prefs.aiEnabled = it
                            if (it) container.challengeRepository.resetAiCooldown()
                        }
                    )
                }
            )

            OutlinedTextField(
                value = gatewayUrl,
                onValueChange = { gatewayUrl = it },
                label = { Text("Gateway endpoint URL") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = apiKey,
                onValueChange = { apiKey = it },
                label = { Text("API key") },
                visualTransformation = if (showApiKey) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                trailingIcon = {
                    IconButton(onClick = { showApiKey = !showApiKey }) {
                        Icon(
                            imageVector = if (showApiKey) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                            contentDescription = if (showApiKey) "Hide API key" else "Show API key"
                        )
                    }
                },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            ExposedDropdownMenuBox(
                expanded = modelMenuExpanded,
                onExpandedChange = { modelMenuExpanded = it },
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedTextField(
                    value = modelName,
                    onValueChange = {
                        modelName = it
                        prefs.modelName = it
                    },
                    label = { Text("AI Model") },
                    supportingText = { Text("Applies app-wide (Alarms, Vision, Tools & Coach)") },
                    trailingIcon = {
                        ExposedDropdownMenuDefaults.TrailingIcon(expanded = modelMenuExpanded)
                    },
                    singleLine = true,
                    modifier = Modifier
                        .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryEditable, enabled = true)
                        .fillMaxWidth()
                )
                ExposedDropdownMenu(
                    expanded = modelMenuExpanded,
                    onDismissRequest = { modelMenuExpanded = false }
                ) {
                    PreferencesManager.SUPPORTED_MODELS.forEach { option ->
                        val isCurrent = modelName.equals(option.id, ignoreCase = true)
                        DropdownMenuItem(
                            text = {
                                Column {
                                    Text(option.displayName, style = MaterialTheme.typography.titleSmall)
                                    Text(
                                        text = "${option.id} • ${option.subtitle}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            },
                            trailingIcon = {
                                if (isCurrent) {
                                    Icon(
                                        Icons.Default.CheckCircle,
                                        contentDescription = "Active model",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            },
                            onClick = {
                                modelName = option.id
                                prefs.modelName = option.id
                                container.challengeRepository.resetAiCooldown()
                                modelMenuExpanded = false
                                scope.launch {
                                    snackbar.showSnackbar("Active model switched to ${option.displayName}")
                                }
                            }
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "AI Model System Instructions",
                    style = MaterialTheme.typography.labelLarge
                )
                TextButton(
                    onClick = { systemInstructions = PreferencesManager.DEFAULT_AI_SYSTEM_INSTRUCTIONS }
                ) { Text("Reset default") }
            }

            OutlinedTextField(
                value = systemInstructions,
                onValueChange = { systemInstructions = it },
                label = { Text("System perspective & instructions") },
                supportingText = { Text("Defines how $modelName thinks, plans routines, and responds across Timecure") },
                minLines = 3,
                maxLines = 6,
                modifier = Modifier.fillMaxWidth()
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = {
                        prefs.gatewayUrl = gatewayUrl
                        prefs.apiKey = apiKey
                        prefs.modelName = modelName
                        prefs.aiSystemInstructions = systemInstructions
                        container.challengeRepository.resetAiCooldown()
                        scope.launch { snackbar.showSnackbar("AI model & system instructions saved") }
                    },
                    modifier = Modifier.weight(1f).height(48.dp)
                ) { Text("Save AI config") }

                FilledTonalButton(
                    onClick = {
                        if (!isTestingAi) {
                            prefs.gatewayUrl = gatewayUrl
                            prefs.apiKey = apiKey
                            prefs.modelName = modelName
                            prefs.aiSystemInstructions = systemInstructions
                            container.challengeRepository.resetAiCooldown()
                            isTestingAi = true
                            scope.launch {
                                val result = container.aiClient.testConnection()
                                isTestingAi = false
                                result.onSuccess { msg ->
                                    testResultOk = true
                                    lastStatus = msg
                                    prefs.lastAiStatus = msg
                                }.onFailure { err ->
                                    testResultOk = false
                                    val msg = err.message ?: "Connection failed"
                                    lastStatus = msg
                                    prefs.lastAiStatus = msg
                                }
                            }
                        }
                    },
                    enabled = !isTestingAi,
                    modifier = Modifier.weight(1f).height(48.dp)
                ) {
                    if (isTestingAi) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                    } else {
                        Icon(Icons.Default.CloudDone, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Test AI")
                    }
                }
            }

            if (lastStatus.isNotBlank()) {
                val isOk = testResultOk == true || lastStatus.startsWith("OK") || lastStatus.startsWith("Connected")
                Surface(
                    color = if (isOk) MaterialTheme.colorScheme.primaryContainer
                    else MaterialTheme.colorScheme.errorContainer,
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (isOk) Icons.Default.CheckCircle else Icons.Default.Warning,
                            contentDescription = null,
                            tint = if (isOk) MaterialTheme.colorScheme.onPrimaryContainer
                            else MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(Modifier.width(10.dp))
                        Text(
                            text = lastStatus,
                            style = MaterialTheme.typography.bodySmall,
                            color = if (isOk) MaterialTheme.colorScheme.onPrimaryContainer
                            else MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            // ---------- Reliability & Simulation ----------
            Text("Reliability & Testing", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)

            ListItem(
                leadingContent = {
                    Icon(
                        imageVector = Icons.Default.Security,
                        contentDescription = null,
                        tint = if (missingPermissions == 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                    )
                },
                headlineContent = { Text("Permissions health check") },
                supportingContent = {
                    Text(
                        if (missingPermissions == 0) "All critical alarm permissions are granted"
                        else "$missingPermissions critical permission(s) missing — tap to fix"
                    )
                },
                modifier = Modifier.clickable(onClick = onOpenPermissions)
            )

            OutlinedButton(
                onClick = {
                    AlarmActions.ring(context, AlarmScheduler.TEST_ALARM_ID, rerings = 0)
                },
                modifier = Modifier.fillMaxWidth().height(50.dp)
            ) {
                Icon(Icons.Default.Alarm, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Simulate full lock-screen alarm")
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}
