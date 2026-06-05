package com.example.scanpro.ui.components

import android.text.format.DateFormat
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.scanpro.domain.model.BarcodeItem
import com.example.scanpro.theme.FavoriteColor

/**
 * List item row for a single barcode record in the history list.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun BarcodeListItem(
    item: BarcodeItem,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onFavoriteToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    val dateString = DateFormat.format("MMM dd, yyyy hh:mm a", item.timestamp).toString()

    ElevatedCard(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.elevatedCardElevation(
            defaultElevation = 2.dp
        )
    ) {
        Row(
            modifier = Modifier
                .combinedClickable(
                    onClick = onClick,
                    onLongClick = onLongClick
                )
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FormatChip(format = item.format)
                    
                    Text(
                        text = item.contentType.displayName,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Medium
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = item.displayValue.ifBlank { item.rawValue },
                    fontSize = 15.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = dateString,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            IconButton(onClick = onFavoriteToggle) {
                Icon(
                    imageVector = if (item.isFavorite) Icons.Default.Star else Icons.Default.StarBorder,
                    contentDescription = if (item.isFavorite) "Remove from favorites" else "Add to favorites",
                    tint = if (item.isFavorite) FavoriteColor else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                )
            }
        }
    }
}
