package com.tvfile.app.presentation.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tvfile.app.data.model.FileCategory
import com.tvfile.app.presentation.theme.*

@Composable
fun FilterBar(
    selectedCategory: FileCategory,
    onCategorySelected: (FileCategory) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        FileCategory.values().forEach { cat ->
            val isSelected = selectedCategory == cat
            FilterChipItem(
                category = cat,
                isSelected = isSelected,
                onClick = { onCategorySelected(cat) }
            )
        }
    }
}

@Composable
private fun FilterChipItem(
    category: FileCategory,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()

    val bgColor = when {
        isFocused -> TVAccentBlue
        isSelected -> Color(0xFF142845)
        else -> TVSurface
    }

    val textColor = when {
        isFocused -> Color.White
        isSelected -> TVAccentCyan
        else -> TVTextSecondary
    }

    val borderColor = when {
        isFocused -> TVAccentCyan
        isSelected -> TVAccentCyan.copy(alpha = 0.5f)
        else -> Color.Transparent
    }

    Surface(
        shape = RoundedCornerShape(20.dp),
        color = bgColor,
        border = BorderStroke(1.dp, borderColor),
        modifier = Modifier.clickable(
            interactionSource = interactionSource,
            indication = null,
            onClick = onClick
        )
    ) {
        Text(
            text = category.displayName,
            color = textColor,
            fontSize = 12.sp,
            fontWeight = if (isSelected || isFocused) FontWeight.Bold else FontWeight.Normal,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
        )
    }
}
