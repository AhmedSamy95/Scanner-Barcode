package com.example.scanpro.ui.detail

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.scanpro.domain.model.BarcodeItem
import com.example.scanpro.domain.repository.BarcodeRepository
import com.example.scanpro.domain.usecase.AnalyzeBarcodeUseCase
import com.example.scanpro.domain.usecase.BarcodeAnalysis
import com.example.scanpro.ui.components.FormatChip
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DetailUiState(
    val barcode: BarcodeItem? = null,
    val analysis: BarcodeAnalysis? = null,
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val showDeleteConfirm: Boolean = false
)

/**
 * ViewModel managing barcode detail query parameters and analysis.
 */
@HiltViewModel
class DetailViewModel @Inject constructor(
    private val repository: BarcodeRepository,
    private val analyzeBarcodeUseCase: AnalyzeBarcodeUseCase,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val _uiState = MutableStateFlow(DetailUiState())
    val uiState: StateFlow<DetailUiState> = _uiState.asStateFlow()

    private val barcodeId: Long = checkNotNull(savedStateHandle["barcodeId"])

    init {
        loadBarcode()
    }

    private fun loadBarcode() {
        viewModelScope.launch {
            _uiState.value = DetailUiState(isLoading = true)
            val barcodeItem = repository.getBarcodeById(barcodeId)
            if (barcodeItem != null) {
                val analysisResult = analyzeBarcodeUseCase(barcodeItem)
                _uiState.value = DetailUiState(
                    barcode = barcodeItem,
                    analysis = analysisResult,
                    isLoading = false
                )
            } else {
                _uiState.value = DetailUiState(
                    errorMessage = "Barcode record not found.",
                    isLoading = false
                )
            }
        }
    }

    fun toggleFavorite() {
        val currentBarcode = uiState.value.barcode ?: return
        viewModelScope.launch {
            val updated = currentBarcode.copy(isFavorite = !currentBarcode.isFavorite)
            repository.updateBarcode(updated)
            _uiState.value = _uiState.value.copy(
                barcode = updated
            )
        }
    }

    fun deleteBarcode(onDeleted: () -> Unit) {
        val currentBarcode = uiState.value.barcode ?: return
        viewModelScope.launch {
            repository.deleteBarcode(currentBarcode)
            onDeleted()
        }
    }

    fun showDeleteDialog(show: Boolean) {
        _uiState.value = _uiState.value.copy(showDeleteConfirm = show)
    }
}

/**
 * Screen showing complete metadata, actions, and custom checksum/integrity checks for a barcode.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailScreen(
    onBack: () -> Unit,
    onNavigateToGenerator: (String) -> Unit = {},
    viewModel: DetailViewModel = hiltViewModel(),
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Details & Diagnostics", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Go back")
                    }
                },
                actions = {
                    if (uiState.barcode != null) {
                        IconButton(onClick = { viewModel.toggleFavorite() }) {
                            Icon(
                                imageVector = if (uiState.barcode!!.isFavorite) Icons.Default.Star else Icons.Default.StarBorder,
                                contentDescription = "Favorite",
                                tint = if (uiState.barcode!!.isFavorite) com.example.scanpro.theme.FavoriteColor else LocalContentColor.current
                            )
                        }
                        IconButton(onClick = { viewModel.showDeleteDialog(true) }) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Delete",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        modifier = modifier.fillMaxSize()
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (uiState.isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else if (uiState.errorMessage != null) {
                Text(
                    text = uiState.errorMessage!!,
                    color = MaterialTheme.colorScheme.error,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(32.dp)
                )
            } else if (uiState.barcode != null) {
                val barcode = uiState.barcode!!
                val analysis = uiState.analysis

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(scrollState)
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Large visual Card showing raw code value
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                FormatChip(format = barcode.format)
                                Text(
                                    text = barcode.contentType.displayName,
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            Text(
                                text = barcode.rawValue,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth()
                            )

                            Spacer(modifier = Modifier.height(24.dp))

                            // Core Action buttons
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceEvenly
                            ) {
                                // Copy
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    IconButton(
                                        onClick = {
                                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                            clipboard.setPrimaryClip(ClipData.newPlainText("Barcode raw value", barcode.rawValue))
                                            coroutineScope.launch {
                                                snackbarHostState.showSnackbar("Copied to clipboard")
                                            }
                                        }
                                    ) {
                                        Icon(Icons.Default.ContentCopy, contentDescription = "Copy")
                                    }
                                    Text("Copy", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }

                                // Share
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    IconButton(
                                        onClick = {
                                            val intent = Intent(Intent.ACTION_SEND).apply {
                                                type = "text/plain"
                                                putExtra(Intent.EXTRA_TEXT, barcode.rawValue)
                                            }
                                            context.startActivity(Intent.createChooser(intent, "Share value"))
                                        }
                                    ) {
                                        Icon(Icons.Default.Share, contentDescription = "Share")
                                    }
                                    Text("Share", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }

                                // Generate barcode
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    IconButton(
                                        onClick = { onNavigateToGenerator(barcode.rawValue) }
                                    ) {
                                        Icon(Icons.Default.QrCode, contentDescription = "Generate barcode")
                                    }
                                    Text("Generate", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }

                                // Custom URL action
                                if (barcode.contentType == com.example.scanpro.domain.model.BarcodeContentType.URL || barcode.rawValue.startsWith("http")) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        IconButton(
                                            onClick = {
                                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(barcode.rawValue))
                                                context.startActivity(intent)
                                            }
                                        ) {
                                            Icon(Icons.Default.Launch, contentDescription = "Open link")
                                        }
                                        Text("Open Link", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Analysis diagnostics section
                    if (analysis != null) {
                        Text(
                            text = "Code Analysis & Diagnostics",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.Start
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                AnalysisRow(label = "Character Count", value = "${analysis.characterCount} chars")
                                AnalysisRow(label = "Character Encoding", value = analysis.encodingType)
                                AnalysisRow(label = "Format Name", value = analysis.formatName)
                                AnalysisRow(label = "Content Semantics", value = analysis.contentTypeName)
                                AnalysisRow(
                                    label = "Flags",
                                    value = when {
                                        analysis.isNumericOnly -> "Numeric Only"
                                        analysis.isAlphanumeric -> "Alphanumeric"
                                        else -> "Contains Special Chars"
                                    }
                                )
                                AnalysisRow(label = "Capacity Level", value = analysis.estimatedDataCapacity)
                                AnalysisRow(
                                    label = "Checksum Integrity",
                                    value = if (analysis.checksumValid) "Valid (Checksum OK)" else "Standard Checksum"
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(32.dp))
                }
            }
        }
    }

    // Delete Confirmation Dialog
    if (uiState.showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { viewModel.showDeleteDialog(false) },
            title = { Text("Delete Barcode Record") },
            text = { Text("Are you sure you want to permanently delete this barcode from your history?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteBarcode {
                            viewModel.showDeleteDialog(false)
                            onBack()
                        }
                    }
                ) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.showDeleteDialog(false) }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun AnalysisRow(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
