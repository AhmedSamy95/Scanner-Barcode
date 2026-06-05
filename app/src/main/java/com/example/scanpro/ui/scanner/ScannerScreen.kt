package com.example.scanpro.ui.scanner

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Launch
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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

    Box(
        modifier = modifier.fillMaxSize().background(Color.Black)
    ) {
        if (cameraPermissionState.status.isGranted) {
            // Camera surface
            CameraPreview(
                isFlashOn = uiState.isFlashOn,
                zoomRatio = uiState.zoomRatio,
                onZoomChanged = { viewModel.setZoomRatio(it) },
                onBarcodeDetected = { viewModel.onBarcodesDetected(it) },
                onCameraReady = { viewModel.onCameraReady(it) }
            )

            // Transparent scan overlay
            ViewfinderOverlay()

            // Top Flash button
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(top = 16.dp),
                contentAlignment = Alignment.TopCenter
            ) {
                IconButton(
                    onClick = { viewModel.toggleFlash() },
                    colors = IconButtonDefaults.iconButtonColors(
                        containerColor = Color.Black.copy(alpha = 0.5f)
                    ),
                    modifier = Modifier.size(54.dp)
                ) {
                    Icon(
                        imageVector = if (uiState.isFlashOn) Icons.Default.FlashOn else Icons.Default.FlashOff,
                        contentDescription = "Toggle flashlight",
                        tint = if (uiState.isFlashOn) Color.Yellow else Color.White,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }

            // Bottom zoom control slider
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
                Text(
                    text = "Zoom: ${"%.1f".format(uiState.zoomRatio)}x",
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
                Slider(
                    value = uiState.zoomRatio,
                    onValueChange = { viewModel.setZoomRatio(it) },
                    valueRange = uiState.minZoom..uiState.maxZoom,
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

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        // Details button
                        Button(
                            onClick = {
                                viewModel.dismissBottomSheet()
                                onNavigateToDetail(scanItem.id)
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Info, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Full Details")
                        }

                        // Share / Web action button
                        if (scanItem.contentType == com.example.scanpro.domain.model.BarcodeContentType.URL || 
                            scanItem.rawValue.startsWith("http")) {
                            FilledTonalButton(
                                onClick = {
                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(scanItem.rawValue))
                                    context.startActivity(intent)
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.Launch, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Open Link")
                            }
                        } else {
                            FilledTonalButton(
                                onClick = {
                                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                        type = "text/plain"
                                        putExtra(Intent.EXTRA_TEXT, scanItem.rawValue)
                                    }
                                    context.startActivity(Intent.createChooser(shareIntent, "Share Scanned Code"))
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.Share, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Share")
                            }
                        }
                    }

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
