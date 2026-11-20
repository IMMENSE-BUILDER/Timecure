package com.timecure.app.ui.screens.permissions

import android.Manifest
import android.content.Intent
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import com.timecure.app.ui.components.safeTopBarWindowInsets
import com.timecure.app.util.Permissions

private data class PermissionRow(
    val title: String,
    val why: String,
    val granted: Boolean,
    val critical: Boolean,
    val action: () -> Unit
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PermissionsScreen(
    onDone: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var state by remember { mutableStateOf(Permissions.check(context)) }
    // Re-check every time the user comes back from a system settings page.
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { state = Permissions.check(context) }

    val notificationLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        state = Permissions.check(context)
    }

    fun open(intent: Intent) = runCatching { context.startActivity(intent) }

    val rows = listOf(
        PermissionRow(
            "Notifications",
            "Lets the ringing screen appear and keeps the alarm service visible.",
            state.notifications, true
        ) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            } else open(Permissions.notificationSettings(context))
        },
        PermissionRow(
            "Exact alarms",
            "Required so the alarm fires at the exact minute, even in battery saver.",
            state.exactAlarms, true
        ) { open(Permissions.exactAlarmSettings(context)) },
        PermissionRow(
            "Full-screen takeover",
            "Lets the challenge open over the lock screen and wake the display.",
            state.fullScreen, true
        ) { open(Permissions.fullScreenSettings(context)) },
        PermissionRow(
            "Unrestricted battery",
            "Recommended: stops the system from delaying or killing the alarm service.",
            state.batteryUnrestricted, false
        ) { open(Permissions.batterySettings()) },
        PermissionRow(
            "Do Not Disturb access",
            "Optional: lets the alarm notification bypass custom Do Not Disturb rules.",
            state.dndAccess, false
        ) { open(Permissions.dndSettings()) }
    )

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                windowInsets = safeTopBarWindowInsets(),
                title = { Text("Make it unstoppable") },
                navigationIcon = {
                    IconButton(onClick = onDone) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
        ) {
            Text(
                text = if (state.criticalOk) "All critical permissions are granted."
                else "${state.missingCriticalCount} critical permission(s) still needed. Without them an alarm can fail to ring.",
                style = MaterialTheme.typography.bodyLarge,
                color = if (state.criticalOk) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)
            )
            rows.forEach { row ->
                ListItem(
                    leadingContent = {
                        Icon(
                            imageVector = if (row.granted) Icons.Filled.CheckCircle else Icons.Filled.Warning,
                            contentDescription = null,
                            tint = if (row.granted) MaterialTheme.colorScheme.primary
                            else if (row.critical) MaterialTheme.colorScheme.error
                            else MaterialTheme.colorScheme.tertiary
                        )
                    },
                    headlineContent = { Text(row.title) },
                    supportingContent = {
                        Column {
                            Text(row.why)
                            Text(
                                text = if (row.granted) "Granted" else if (row.critical) "Needs action" else "Recommended",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    },
                    trailingContent = {
                        if (!row.granted) {
                            FilledTonalButton(
                                onClick = row.action,
                                modifier = Modifier.semantics { contentDescription = "Allow ${row.title}" }
                            ) { Text("Allow") }
                        }
                    }
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            }
            Spacer(Modifier.height(24.dp))
            Button(
                onClick = onDone,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp).height(52.dp)
            ) { Text(if (state.criticalOk) "Continue" else "Continue anyway") }
            Spacer(Modifier.height(24.dp))
        }
    }
}
