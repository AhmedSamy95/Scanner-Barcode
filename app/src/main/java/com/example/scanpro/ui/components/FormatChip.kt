package com.example.scanpro.ui.components

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.scanpro.domain.model.BarcodeFormatType
import com.example.scanpro.theme.PrimaryAccent
import com.example.scanpro.theme.SecondaryAccent

/**
 * A styled badge/chip displaying a barcode's format.
 * Color scales dynamically based on whether it is a 1D linear or 2D matrix format.
 */
@Composable
fun FormatChip(
    format: BarcodeFormatType,
    modifier: Modifier = Modifier
) {
    val backgroundColor = if (format.is2D) {
        PrimaryAccent.copy(alpha = 0.15f)
    } else {
        SecondaryAccent.copy(alpha = 0.15f)
    }
    
    val textColor = if (format.is2D) {
        PrimaryAccent
    } else {
        Color(0xFFB39DDB) // Light purple for electric secondary
    }

    Surface(
        color = backgroundColor,
        shape = RoundedCornerShape(6.dp),
        modifier = modifier
    ) {
        Text(
            text = format.displayName,
            color = textColor,
            fontSize = 11.sp,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}
