package com.example.scanpro.ui.generator

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ArrowDropUp
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.scanpro.domain.model.BarcodeFormatType
import com.example.scanpro.theme.FavoriteColor

/**
 * Screen enabling users to generate various 1D and 2D barcode formats with customizable data.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GeneratorScreen(
    viewModel: GeneratorViewModel,
    initialValue: String? = null,
    onInitialValueConsumed: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    var expandedDropdown by remember { mutableStateOf(false) }
    val scrollState = rememberScrollState()

    // Set initial value from scanner if provided
    LaunchedEffect(initialValue) {
        if (!initialValue.isNullOrBlank()) {
            viewModel.onInputTextChanged(initialValue)
            onInitialValueConsumed()
        }
    }

    // Handle Snackbars for Success / Failure
    val snackbarHostState = remember { SnackbarHostState() }
    
    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearNotifications()
        }
    }

    LaunchedEffect(uiState.saveSuccessMessage) {
        uiState.saveSuccessMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearNotifications()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        modifier = modifier.fillMaxSize()
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(scrollState)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Barcode Generator",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Start
            )
            Text(
                text = "Select format type and write text to generate.",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                textAlign = TextAlign.Start
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Dropdown Selector for Barcode Format
            ExposedDropdownMenuBox(
                expanded = expandedDropdown,
                onExpandedChange = { expandedDropdown = it },
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedTextField(
                    value = uiState.selectedFormat.displayName,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Symbology Format") },
                    trailingIcon = {
                        Icon(
                            imageVector = if (expandedDropdown) Icons.Default.ArrowDropUp else Icons.Default.ArrowDropDown,
                            contentDescription = null
                        )
                    },
                    modifier = Modifier.fillMaxWidth().menuAnchor(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline
                    )
                )

                ExposedDropdownMenu(
                    expanded = expandedDropdown,
                    onDismissRequest = { expandedDropdown = false }
                ) {
                    // 2D Formats Header
                    DropdownMenuItem(
                        text = { Text("2D / MATRIX FORMATS", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = MaterialTheme.colorScheme.primary) },
                        onClick = {},
                        enabled = false
                    )
                    BarcodeFormatType.values().filter { it.is2D }.forEach { format ->
                        DropdownMenuItem(
                            text = { Text(format.displayName) },
                            onClick = {
                                viewModel.onFormatSelected(format)
                                expandedDropdown = false
                            }
                        )
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                    // 1D Formats Header
                    DropdownMenuItem(
                        text = { Text("1D / LINEAR BARCODES", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = MaterialTheme.colorScheme.secondary) },
                        onClick = {},
                        enabled = false
                    )
                    BarcodeFormatType.values().filter { !it.is2D && it != BarcodeFormatType.UNKNOWN }.forEach { format ->
                        DropdownMenuItem(
                            text = { Text(format.displayName) },
                            onClick = {
                                viewModel.onFormatSelected(format)
                                expandedDropdown = false
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Content Input Field
            OutlinedTextField(
                value = uiState.inputText,
                onValueChange = { viewModel.onInputTextChanged(it) },
                label = { Text("Data to Encode") },
                placeholder = { Text(if (uiState.selectedFormat.is2D) "Enter URL or text content" else "Enter numeric or alphanumeric characters") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = false,
                maxLines = 4
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Generate Button
            Button(
                onClick = { viewModel.generateBarcode() },
                modifier = Modifier.fillMaxWidth().height(50.dp),
                shape = RoundedCornerShape(8.dp)
            ) {
                if (uiState.isGenerating) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(24.dp))
                } else {
                    Text("Generate Barcode", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Visual Card previewing the output Bitmap
            if (uiState.generatedBitmap != null) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .wrapContentHeight(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Image(
                            bitmap = uiState.generatedBitmap!!.asImageBitmap(),
                            contentDescription = "Generated barcode image",
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(if (uiState.selectedFormat.is2D) 250.dp else 120.dp)
                                .background(androidx.compose.ui.graphics.Color.White, RoundedCornerShape(8.dp))
                                .padding(16.dp),
                            contentScale = ContentScale.Fit
                        )

                        Spacer(modifier = Modifier.height(24.dp))

                        // Controls Action Bar
                        Row(
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            // Save button
                            IconButtonWithLabel(
                                icon = Icons.Default.Save,
                                label = "Save",
                                onClick = { viewModel.saveToGallery() }
                            )

                            // Share button
                            IconButtonWithLabel(
                                icon = Icons.Default.Share,
                                label = "Share",
                                onClick = { viewModel.shareBarcode() }
                            )

                            // Favorite button
                            IconButtonWithLabel(
                                icon = Icons.Default.Favorite,
                                label = "Bookmark",
                                iconColor = if (uiState.isAddedToFavorites) FavoriteColor else MaterialTheme.colorScheme.onSurface,
                                onClick = { viewModel.addToFavorites() }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun IconButtonWithLabel(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    iconColor: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.onSurface
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
    ) {
        IconButton(onClick = onClick) {
            Icon(imageVector = icon, contentDescription = label, tint = iconColor, modifier = Modifier.size(24.dp))
        }
        Text(text = label, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
