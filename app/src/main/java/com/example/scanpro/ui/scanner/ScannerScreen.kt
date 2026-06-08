package com.example.scanpro.ui.scanner

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.scanpro.domain.model.BarcodeContentType
import com.example.scanpro.domain.model.BarcodeItem
import com.example.scanpro.ui.components.FormatChip
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.accompanist.permissions.shouldShowRationale

/**
 * Main barcode/QR scanner camera screen with full Material 3 bottom sheet result details.
 */
@OptIn(ExperimentalPermissionsApi::class, ExperimentalMaterial3Api::class)
@Composable
fun ScannerScreen(
    onNavigateToDetail: (Long) -> Unit,
    onNavigateToGenerator: (String) -> Unit,
    viewModel: ScannerViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val cameraPermissionState = rememberPermissionState(android.Manifest.permission.CAMERA)

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
        onResult = { uri ->
            uri?.let { viewModel.onImageSelected(it) }
        }
    )

    Box(
        modifier = modifier.fillMaxSize().background(Color.Black)
    ) {
        if (cameraPermissionState.status.isGranted) {
            // Camera surface
            CameraPreview(
                isFlashOn = uiState.isFlashOn,
                zoomRatio = uiState.zoomRatio,
                scanMode = uiState.scanMode,
                ocrFrameScale = uiState.ocrFrameSize,
                barcodeScanner = viewModel.barcodeScanner,
                textRecognizer = viewModel.textRecognizer,
                onZoomChanged = { viewModel.setZoomRatio(it) },
                onBarcodeDetected = { viewModel.onBarcodesDetected(it) },
                onTextDetected = { viewModel.onTextDetected(it) },
                onCameraReady = { viewModel.onCameraReady(it) }
            )

            // Transparent scan overlay
            ViewfinderOverlay(
                rectScaleHeight = if (uiState.scanMode == ScanMode.BARCODE) 0.35f else uiState.ocrFrameSize,
                rectScaleWidth = if (uiState.scanMode == ScanMode.BARCODE) 0.7f else 0.85f
            )

            if (uiState.scanMode == ScanMode.OCR) {
                // OCR Real-time display
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (uiState.detectedText.isNotEmpty()) {
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = Color.Black.copy(alpha = 0.7f),
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.padding(bottom = 200.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    text = uiState.detectedText,
                                    fontSize = 15.sp,
                                    maxLines = 8,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Button(
                                    onClick = { viewModel.copyDetectedText() },
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                                ) {
                                    Icon(Icons.Default.ContentCopy, contentDescription = null)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Copy Text")
                                }
                            }
                        }
                    }
                }
            }

            // Top Flash & Gallery buttons
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(start = 24.dp, end = 24.dp, top = 16.dp),
            ) {
                IconButton(
                    onClick = { viewModel.toggleFlash() },
                    colors = IconButtonDefaults.iconButtonColors(
                        containerColor = Color.Black.copy(alpha = 0.5f)
                    ),
                    modifier = Modifier.size(54.dp).align(Alignment.TopStart)
                ) {
                    Icon(
                        imageVector = if (uiState.isFlashOn) Icons.Default.FlashOn else Icons.Default.FlashOff,
                        contentDescription = "Toggle flashlight",
                        tint = if (uiState.isFlashOn) Color.Yellow else Color.White,
                        modifier = Modifier.size(28.dp)
                    )
                }

                IconButton(
                    onClick = {
                        photoPickerLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    },
                    colors = IconButtonDefaults.iconButtonColors(
                        containerColor = Color.Black.copy(alpha = 0.5f)
                    ),
                    modifier = Modifier.size(54.dp).align(Alignment.TopEnd)
                ) {
                    if (uiState.isGalleryProcessing) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.PhotoLibrary,
                            contentDescription = "Scan from gallery",
                            tint = Color.White,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }
            }

            // Mode Selector
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(bottom = 24.dp),
                contentAlignment = Alignment.BottomCenter
            ) {
                Row(
                    modifier = Modifier
                        .background(Color.Black.copy(alpha = 0.7f), RoundedCornerShape(24.dp))
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    ModeButton(
                        selected = uiState.scanMode == ScanMode.BARCODE,
                        label = "Barcode",
                        icon = Icons.Default.QrCodeScanner,
                        onClick = { viewModel.setScanMode(ScanMode.BARCODE) }
                    )
                    ModeButton(
                        selected = uiState.scanMode == ScanMode.OCR,
                        label = "OCR / Text",
                        icon = Icons.Default.TextFields,
                        onClick = { viewModel.setScanMode(ScanMode.OCR) }
                    )
                }
            }

            // Control slider (Zoom or OCR Frame Size)
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(bottom = 96.dp)
                    .width(260.dp)
                    .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(12.dp))
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                val label = if (uiState.scanMode == ScanMode.BARCODE) {
                    "Zoom: ${"%.1f".format(uiState.zoomRatio)}x"
                } else {
                    "OCR Frame Height"
                }
                
                Text(
                    text = label,
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
                
                Slider(
                    value = if (uiState.scanMode == ScanMode.BARCODE) uiState.zoomRatio else uiState.ocrFrameSize,
                    onValueChange = { 
                        if (uiState.scanMode == ScanMode.BARCODE) viewModel.setZoomRatio(it) 
                        else viewModel.setOcrFrameSize(it)
                    },
                    valueRange = if (uiState.scanMode == ScanMode.BARCODE) {
                        uiState.minZoom..uiState.maxZoom
                    } else {
                        0.1f..0.8f
                    },
                    colors = SliderDefaults.colors(
                        thumbColor = MaterialTheme.colorScheme.primary,
                        activeTrackColor = MaterialTheme.colorScheme.primary,
                        inactiveTrackColor = Color.White.copy(alpha = 0.3f)
                    )
                )
            }

            // Paused overlay — scanning stopped after a capture, awaiting "Scan Again"
            if (uiState.isScanningPaused && !uiState.showBottomSheet) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.55f)),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "Scanning paused",
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = { viewModel.resumeScanning() }) {
                            Icon(Icons.Default.QrCodeScanner, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Scan Again")
                        }
                    }
                }
            }
        } else {
            // Permission missing / denied screen
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "Camera Permission Required",
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(16.dp))
                val explanation = if (cameraPermissionState.status.shouldShowRationale) {
                    "ScanPro requires camera access to analyze barcodes and QR codes in real-time. Please grant permission."
                } else {
                    "Camera permission has been denied. Please enable it in the system Settings to use the scanner."
                }
                Text(
                    text = explanation,
                    color = Color.LightGray,
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(24.dp))
                Button(
                    onClick = { cameraPermissionState.launchPermissionRequest() }
                ) {
                    Text("Grant Permission")
                }
            }
        }

        // Multiple results picker dialog (for Gallery)
        if (uiState.showMultiScanPicker) {
            AlertDialog(
                onDismissRequest = { viewModel.dismissMultiScanPicker() },
                title = { Text("Multiple Codes Detected") },
                text = {
                    Column {
                        Text("Select the code you want to open:", style = MaterialTheme.typography.bodyMedium)
                        Spacer(modifier = Modifier.height(16.dp))
                        LazyColumn(modifier = Modifier.heightIn(max = 300.dp)) {
                            items(uiState.multiScanResults) { item ->
                                ListItem(
                                    headlineContent = { Text(item.displayValue, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                                    supportingContent = { Text("${item.format.displayName} • ${item.contentType.displayName}") },
                                    leadingContent = { Icon(Icons.Default.QrCode, contentDescription = null) },
                                    modifier = Modifier.clickable { viewModel.onBarcodeSelectedFromList(item) }
                                )
                                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { viewModel.dismissMultiScanPicker() }) {
                        Text("Cancel")
                    }
                }
            )
        }

        // Result bottom sheet modal
        if (uiState.showBottomSheet && uiState.lastScannedBarcode != null) {
            val scanItem = uiState.lastScannedBarcode!!
            ModalBottomSheet(
                onDismissRequest = { viewModel.dismissBottomSheet() },
                sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
                containerColor = MaterialTheme.colorScheme.surface
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(horizontal = 24.dp, vertical = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Scan Successful",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FormatChip(format = scanItem.format)
                        Text(
                            text = scanItem.contentType.displayName,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = scanItem.rawValue,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        textAlign = TextAlign.Center,
                        maxLines = 4,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    SmartActionButtons(
                        scanItem = scanItem,
                        onNavigateToDetail = onNavigateToDetail,
                        viewModel = viewModel
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Generate Barcode button
                    OutlinedButton(
                        onClick = {
                            viewModel.dismissBottomSheet()
                            onNavigateToGenerator(scanItem.rawValue)
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.QrCode, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Generate Barcode")
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    }
}

@Composable
fun ModeButton(
    selected: Boolean,
    label: String,
    icon: ImageVector,
    onClick: () -> Unit
) {
    Surface(
        color = if (selected) MaterialTheme.colorScheme.primary else Color.Transparent,
        contentColor = if (selected) MaterialTheme.colorScheme.onPrimary else Color.White,
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(text = label, fontSize = 14.sp, fontWeight = FontWeight.Medium)
        }
    }
}

@Composable
fun SmartActionButtons(
    scanItem: BarcodeItem,
    onNavigateToDetail: (Long) -> Unit,
    viewModel: ScannerViewModel
) {
    val context = LocalContext.current
    val rawValue = scanItem.rawValue

    Row(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        // Main action button based on content type
        when (scanItem.contentType) {
            BarcodeContentType.URL -> {
                ActionBtn(
                    text = "Open Link",
                    icon = Icons.Default.Launch,
                    onClick = {
                        val uri = if (!rawValue.startsWith("http")) "https://$rawValue" else rawValue
                        try {
                            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(uri)))
                        } catch (e: Exception) { /* Handle error */ }
                    },
                    modifier = Modifier.weight(1f)
                )
            }
            BarcodeContentType.PHONE -> {
                ActionBtn(
                    text = "Call",
                    icon = Icons.Default.Phone,
                    onClick = {
                        context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:$rawValue")))
                    },
                    modifier = Modifier.weight(1f)
                )
            }
            BarcodeContentType.EMAIL -> {
                ActionBtn(
                    text = "Email",
                    icon = Icons.Default.Email,
                    onClick = {
                        val intent = Intent(Intent.ACTION_SENDTO).apply {
                            data = Uri.parse("mailto:$rawValue")
                        }
                        context.startActivity(intent)
                    },
                    modifier = Modifier.weight(1f)
                )
            }
            BarcodeContentType.WIFI -> {
                ActionBtn(
                    text = "Copy Info",
                    icon = Icons.Default.Wifi,
                    onClick = {
                        viewModel.copyDetectedText() // Generic copy logic could be used here
                    },
                    modifier = Modifier.weight(1f)
                )
            }
            else -> {
                ActionBtn(
                    text = "Share",
                    icon = Icons.Default.Share,
                    onClick = {
                        val shareIntent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_TEXT, rawValue)
                        }
                        context.startActivity(Intent.createChooser(shareIntent, "Share"))
                    },
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Details button
        ActionBtn(
            text = "Details",
            icon = Icons.Default.Info,
            onClick = {
                viewModel.dismissBottomSheet()
                onNavigateToDetail(scanItem.id)
            },
            modifier = Modifier.weight(1f),
            isTonal = true
        )
    }
}

@Composable
fun ActionBtn(
    text: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isTonal: Boolean = false
) {
    if (isTonal) {
        FilledTonalButton(onClick = onClick, modifier = modifier) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(text, fontSize = 13.sp)
        }
    } else {
        Button(onClick = onClick, modifier = modifier) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(text, fontSize = 13.sp)
        }
    }
}
