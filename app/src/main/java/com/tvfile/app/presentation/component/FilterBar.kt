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
        horizontalArrangement = Arrangement.spacedBy(12.dp)
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
        isSelected -> Color(0xFF0F3150)
        else -> TVSurface
    }

    val textColor = when {
        isFocused -> Color.White
        isSelected -> TVAccentCyan
        else -> TVTextSecondary
    }

    val borderColor = when {
        isFocused -> TVAccentCyan
        isSelected -> TVAccentCyan.copy(alpha = 0.8f)
        else -> Color.Transparent
    }

    // 大屏强化：增大按键与字号 (16sp, 内边距 20dp*10dp)
    Surface(
        shape = RoundedCornerShape(24.dp),
        color = bgColor,
        border = BorderStroke(if (isFocused || isSelected) 2.dp else 1.dp, borderColor),
        shadowElevation = if (isFocused) 8.dp else 0.dp,
        modifier = Modifier.clickable(
            interactionSource = interactionSource,
            indication = null,
            onClick = onClick
        )
    ) {
        Text(
            text = category.displayName,
            color = textColor,
            fontSize = 19.sp,
            fontWeight = if (isSelected || isFocused) FontWeight.Bold else FontWeight.Medium,
            modifier = Modifier.padding(horizontal = 22.dp, vertical = 12.dp)
        )
    }
}
