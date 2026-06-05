package com.example.scanpro.ui.history

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.scanpro.domain.model.BarcodeItem
import com.example.scanpro.ui.components.BarcodeListItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader

/**
 * Screen displaying the user's measured barcode history with searching, favorites, and backup imports/exports.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    onNavigateToDetail: (Long) -> Unit,
    viewModel: HistoryViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val feedbackMessage by viewModel.feedbackMessage.collectAsState()
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    var showDeleteAllDialog by remember { mutableStateOf(false) }
    var speedDialExpanded by remember { mutableStateOf(false) }

    // Launcher for picking backup file to IMPORT
    var importTypeIsCsv by remember { mutableStateOf(true) }
    val importFileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            coroutineScope.launch {
                val content = readTextFromUri(context, uri)
                if (content != null) {
                    if (importTypeIsCsv) {
                        viewModel.importCsv(content)
                    } else {
                        viewModel.importJson(content)
                    }
                } else {
                    snackbarHostState.showSnackbar("Unable to read backup file.")
                }
            }
        }
    }

    // Launcher for saving EXPORT files
    val createCsvLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/csv")
    ) { uri: Uri? ->
        if (uri != null) {
            coroutineScope.launch {
                val content = viewModel.exportToCsv()
                if (content != null) {
                    writeTextToUri(context, uri, content, coroutineScope, snackbarHostState)
                } else {
                    snackbarHostState.showSnackbar("Failed to generate CSV export.")
                }
            }
        }
    }

    val createJsonLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri: Uri? ->
        if (uri != null) {
            coroutineScope.launch {
                val content = viewModel.exportToJson()
                if (content != null) {
                    writeTextToUri(context, uri, content, coroutineScope, snackbarHostState)
                } else {
                    snackbarHostState.showSnackbar("Failed to generate JSON export.")
                }
            }
        }
    }

    // Handle feedback notifications
    LaunchedEffect(feedbackMessage) {
        feedbackMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearFeedback()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Speed Dial Menu Items
                AnimatedVisibility(
                    visible = speedDialExpanded,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically()
                ) {
                    Column(
                        horizontalAlignment = Alignment.End,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Import CSV
                        ExtendedFloatingActionButton(
                            onClick = {
                                importTypeIsCsv = true
                                importFileLauncher.launch("*/*")
                                speedDialExpanded = false
                            },
                            icon = { Icon(Icons.Default.UploadFile, contentDescription = null) },
                            text = { Text("Import CSV", fontSize = 12.sp) },
                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.height(40.dp)
                        )

                        // Import JSON
                        ExtendedFloatingActionButton(
                            onClick = {
                                importTypeIsCsv = false
                                importFileLauncher.launch("*/*")
                                speedDialExpanded = false
                            },
                            icon = { Icon(Icons.Default.UploadFile, contentDescription = null) },
                            text = { Text("Import JSON", fontSize = 12.sp) },
                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.height(40.dp)
                        )

                        // Export CSV
                        ExtendedFloatingActionButton(
                            onClick = {
                                createCsvLauncher.launch("ScanPro_Backup_${System.currentTimeMillis()}.csv")
                                speedDialExpanded = false
                            },
                            icon = { Icon(Icons.Default.Download, contentDescription = null) },
                            text = { Text("Export CSV", fontSize = 12.sp) },
                            containerColor = MaterialTheme.colorScheme.secondaryContainer,
                            modifier = Modifier.height(40.dp)
                        )

                        // Export JSON
                        ExtendedFloatingActionButton(
                            onClick = {
                                createJsonLauncher.launch("ScanPro_Backup_${System.currentTimeMillis()}.json")
                                speedDialExpanded = false
                            },
                            icon = { Icon(Icons.Default.Download, contentDescription = null) },
                            text = { Text("Export JSON", fontSize = 12.sp) },
                            containerColor = MaterialTheme.colorScheme.secondaryContainer,
                            modifier = Modifier.height(40.dp)
                        )
                    }
                }

                // Main Toggle FAB
                FloatingActionButton(
                    onClick = { speedDialExpanded = !speedDialExpanded },
                    shape = CircleShape,
                    containerColor = MaterialTheme.colorScheme.primary
                ) {
                    Icon(
                        imageVector = if (speedDialExpanded) Icons.Default.Close else Icons.Default.Backup,
                        contentDescription = "Backup Options"
                    )
                }
            }
        },
        modifier = modifier.fillMaxSize()
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            // Header with Clear All option
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Measured History",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                if (uiState.barcodes.isNotEmpty()) {
                    IconButton(onClick = { showDeleteAllDialog = true }) {
                        Icon(
                            imageVector = Icons.Default.DeleteSweep,
                            contentDescription = "Clear all history",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Search Bar
            OutlinedTextField(
                value = uiState.searchQuery,
                onValueChange = { viewModel.onSearchQueryChanged(it) },
                placeholder = { Text("Search records by code value...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon = {
                    if (uiState.searchQuery.isNotEmpty()) {
                        IconButton(onClick = { viewModel.onSearchQueryChanged("") }) {
                            Icon(Icons.Default.Clear, contentDescription = null)
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp)),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Filter Tabs
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = !uiState.showFavoritesOnly,
                    onClick = { if (uiState.showFavoritesOnly) viewModel.toggleFavoritesFilter() },
                    label = { Text("All Records") }
                )

                FilterChip(
                    selected = uiState.showFavoritesOnly,
                    onClick = { if (!uiState.showFavoritesOnly) viewModel.toggleFavoritesFilter() },
                    label = { Text("Favorites") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (uiState.isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else if (uiState.barcodes.isEmpty()) {
                // Empty History Visual Card
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(32.dp)
                    ) {
                        Icon(
                            imageVector = if (uiState.showFavoritesOnly) Icons.Default.StarBorder else Icons.Default.History,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f),
                            modifier = Modifier.size(80.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = if (uiState.showFavoritesOnly) "No Starred Favorites" else "No Scanned History",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = if (uiState.showFavoritesOnly) "Starred items during scanning or creation will appear here." else "Scanned barcodes, QR codes or generated outputs will be stored here.",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                // Barcode List
                LazyColumn(
                    modifier = Modifier.weight(1f)
                ) {
                    items(
                        items = uiState.barcodes,
                        key = { it.id }
                    ) { item ->
                        var showItemMenu by remember { mutableStateOf(false) }

                        Box {
                            BarcodeListItem(
                                item = item,
                                onClick = { onNavigateToDetail(item.id) },
                                onLongClick = { showItemMenu = true },
                                onFavoriteToggle = { viewModel.toggleFavorite(item) }
                            )

                            // Long press action options menu
                            DropdownMenu(
                                expanded = showItemMenu,
                                onDismissRequest = { showItemMenu = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("Open / View Details") },
                                    leadingIcon = { Icon(Icons.Default.Info, contentDescription = null) },
                                    onClick = {
                                        showItemMenu = false
                                        onNavigateToDetail(item.id)
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Share Data Value") },
                                    leadingIcon = { Icon(Icons.Default.Share, contentDescription = null) },
                                    onClick = {
                                        showItemMenu = false
                                        val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                            type = "text/plain"
                                            putExtra(Intent.EXTRA_TEXT, item.rawValue)
                                        }
                                        context.startActivity(Intent.createChooser(shareIntent, "Share Scanned Value"))
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Delete Record", color = MaterialTheme.colorScheme.error) },
                                    leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
                                    onClick = {
                                        showItemMenu = false
                                        viewModel.deleteBarcode(item)
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Delete All Confirmation Dialog
    if (showDeleteAllDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteAllDialog = false },
            title = { Text("Clear All Records") },
            text = { Text("Are you sure you want to permanently erase all scanned and generated barcodes from history? This cannot be undone.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteAll()
                        showDeleteAllDialog = false
                    }
                ) {
                    Text("Clear All", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteAllDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

// Scoped IO helper to read files from Uri
private suspend fun readTextFromUri(context: Context, uri: Uri): String? = withContext(Dispatchers.IO) {
    try {
        val inputStream = context.contentResolver.openInputStream(uri)
        val reader = BufferedReader(InputStreamReader(inputStream))
        val stringBuilder = StringBuilder()
        var line: String?
        while (reader.readLine().also { line = it } != null) {
            stringBuilder.append(line).append('\n')
        }
        reader.close()
        inputStream?.close()
        stringBuilder.toString()
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}

// Scoped IO helper to write strings into Document provider Uri
private fun writeTextToUri(
    context: Context,
    uri: Uri,
    text: String,
    scope: kotlinx.coroutines.CoroutineScope,
    host: SnackbarHostState
) {
    scope.launch {
        val success = withContext(Dispatchers.IO) {
            try {
                val outputStream = context.contentResolver.openOutputStream(uri)
                outputStream?.write(text.toByteArray())
                outputStream?.close()
                true
            } catch (e: Exception) {
                e.printStackTrace()
                false
            }
        }
        if (success) {
            host.showSnackbar("Backup saved successfully!")
        } else {
            host.showSnackbar("Failed to write backup file.")
        }
    }
}
