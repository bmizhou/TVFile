package com.tvfile.app.presentation.component

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tvfile.app.presentation.theme.TVAccentCyan
import com.tvfile.app.presentation.theme.TVTextMuted
import com.tvfile.app.presentation.theme.TVTextSecondary
import java.net.URLDecoder

@Composable
fun BreadcrumbBar(
    rootName: String,
    currentPath: String,
    onRootClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    // 解决中文 URL 编码导致的显示错误与乱码问题
    val decodedCurrentPath = try {
        URLDecoder.decode(currentPath, "UTF-8")
    } catch (_: Exception) {
        currentPath
    }

    val displaySub = if (decodedCurrentPath.isBlank() || decodedCurrentPath == "/") "" else decodedCurrentPath.trimEnd('/').substringAfterLast('/')

    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Icon(
            imageVector = Icons.Default.Folder,
            contentDescription = null,
            tint = TVAccentCyan,
            modifier = Modifier.size(28.dp)
        )
        Text(
            text = rootName,
            color = if (displaySub.isBlank()) Color.White else TVTextSecondary,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold
        )
        if (displaySub.isNotBlank()) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                contentDescription = null,
                tint = TVTextMuted,
                modifier = Modifier.size(18.dp)
            )
            Text(
                text = displaySub,
                color = Color.White,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
