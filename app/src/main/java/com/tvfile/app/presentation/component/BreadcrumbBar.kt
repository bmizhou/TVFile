package com.tvfile.app.presentation.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tvfile.app.presentation.theme.TVAccentCyan
import com.tvfile.app.presentation.theme.TVTextMuted
import com.tvfile.app.presentation.theme.TVTextPrimary
import com.tvfile.app.presentation.theme.TVTextSecondary

@Composable
fun BreadcrumbBar(
    rootName: String,
    currentPath: String,
    onRootClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val displaySub = if (currentPath.isBlank() || currentPath == "/") "" else currentPath.trimEnd('/').substringAfterLast('/')

    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(
            imageVector = Icons.Default.Folder,
            contentDescription = null,
            tint = TVAccentCyan,
            modifier = Modifier.size(18.dp)
        )
        Text(
            text = rootName,
            color = if (displaySub.isBlank()) TVTextPrimary else TVTextSecondary,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.clickable { onRootClick() }
        )
        if (displaySub.isNotBlank()) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                contentDescription = null,
                tint = TVTextMuted,
                modifier = Modifier.size(12.dp)
            )
            Text(
                text = displaySub,
                color = TVTextPrimary,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
