package com.example.scanpro.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Settings configuration screen.
 * Toggles preferences like Continuous Scan, Sound, Vibration, Clipboard copy, and REST product lookup.
 */
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(24.dp)
    ) {
        Text(
            text = "Settings",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = "Configure app behaviors and integration interfaces.",
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp, bottom = 24.dp)
        )

        // Section 1: Scanner Settings
        SettingHeader(title = "Scanner Configurations")
        
        SettingSwitchRow(
            icon = Icons.Outlined.VideoCameraFront,
            title = "Continuous Scanning Mode",
            description = "Keep camera active and scanning after finding a barcode",
            checked = uiState.continuousScan,
            onCheckedChange = { viewModel.setContinuousScan(it) }
        )

        SettingSwitchRow(
            icon = Icons.Outlined.ContentCopy,
            title = "Handle Duplicate Scans",
            description = "Bumps existing records to the top instead of adding duplicates",
            checked = uiState.handleDuplicates,
            onCheckedChange = { viewModel.setHandleDuplicates(it) }
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Section 2: Scanned Feedback
        SettingHeader(title = "Scan Feedback")

        SettingSwitchRow(
            icon = Icons.Outlined.VolumeUp,
            title = "Beep Notification",
            description = "Plays a short confirmation sound upon success",
            checked = uiState.soundFeedback,
            onCheckedChange = { viewModel.setSoundFeedback(it) }
        )

        SettingSwitchRow(
            icon = Icons.Outlined.Vibration,
            title = "Vibration Haptics",
            description = "Triggers a quick vibration pulse upon success",
            checked = uiState.vibrationFeedback,
            onCheckedChange = { viewModel.setVibrationFeedback(it) }
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Section 3: Integrations & Actions
        SettingHeader(title = "Actions & Integrations")

        SettingSwitchRow(
            icon = Icons.Outlined.Assignment,
            title = "Auto-Copy to Clipboard",
            description = "Automatically copy scanned text values to system clipboard",
            checked = uiState.autoCopyToClipboard,
            onCheckedChange = { viewModel.setAutoCopyToClipboard(it) }
        )

        SettingSwitchRow(
            icon = Icons.Outlined.CloudSync,
            title = "REST Product Info Lookup",
            description = "Enable simulated lookup to resolve UPC/EAN metadata online",
            checked = uiState.productLookup,
            onCheckedChange = { viewModel.setProductLookup(it) }
        )

        Spacer(modifier = Modifier.height(32.dp))
    }
}

@Composable
fun SettingHeader(title: String) {
    Text(
        text = title,
        fontSize = 14.sp,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(vertical = 8.dp)
    )
}

@Composable
fun SettingSwitchRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(24.dp)
        )
        
        Spacer(modifier = Modifier.width(16.dp))
        
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
            Text(text = description, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }

        Spacer(modifier = Modifier.width(8.dp))

        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = MaterialTheme.colorScheme.primary,
                checkedTrackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
            )
        )
    }
}
