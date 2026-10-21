package com.tvfile.app.presentation.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tvfile.app.data.model.TVFile
import com.tvfile.app.presentation.theme.*

@Composable
fun ContextMenuDrawer(
    targetFile: TVFile?,
    canPaste: Boolean,
    onCopy: () -> Unit,
    onCut: () -> Unit,
    onPaste: () -> Unit,
    onDelete: () -> Unit,
    onRename: () -> Unit,
    onDetails: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val firstFocusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(120)
        try {
            firstFocusRequester.requestFocus()
        } catch (_: Exception) {}
    }

    Surface(
        modifier = modifier
            .width(320.dp)
            .fillMaxHeight(),
        color = TVSurface,
        border = BorderStroke(1.5.dp, TVAccentCyan.copy(alpha = 0.8f)),
        shape = RoundedCornerShape(topStart = 20.dp, bottomStart = 20.dp),
        shadowElevation = 24.dp
    ) {
        Column(
            modifier = Modifier
                .padding(22.dp)
                .fillMaxHeight(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "⚡ 文件操作",
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = targetFile?.name ?: "未选择文件",
                color = TVTextSecondary,
                fontSize = 13.sp,
                maxLines = 1
            )
            HorizontalDivider(color = TVBorder, thickness = 1.dp)

            TVMenuItem(
                title = "📋 复制 (Copy)",
                enabled = targetFile != null,
                focusRequester = firstFocusRequester,
                onClick = onCopy
            )
            TVMenuItem(
                title = "✂️ 剪切 (Cut)",
                enabled = targetFile != null,
                onClick = onCut
            )
            TVMenuItem(
                title = "📥 粘贴到此处",
                enabled = canPaste,
                onClick = onPaste
            )
            TVMenuItem(
                title = "🗑️ 删除文件",
                enabled = targetFile != null,
                textColor = TVAccentRed,
                onClick = onDelete
            )
            TVMenuItem(
                title = "✏️ 重命名",
                enabled = targetFile != null,
                onClick = onRename
            )
            TVMenuItem(
                title = "ℹ️ 属性详情",
                enabled = targetFile != null,
                onClick = onDetails
            )

            Spacer(modifier = Modifier.weight(1f))

            TVButton(
                onClick = onDismiss,
                focusedBackgroundColor = Color(0xFF28364F),
                unfocusedBackgroundColor = TVCardBg,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(text = "关闭菜单 (BACK)", color = TVTextSecondary, fontSize = 13.sp)
            }
        }
    }
}

@Composable
private fun TVMenuItem(
    title: String,
    enabled: Boolean,
    textColor: Color = Color.White,
    focusRequester: FocusRequester? = null,
    onClick: () -> Unit
) {
    var mod = Modifier.fillMaxWidth()
    if (focusRequester != null) {
        mod = mod.focusRequester(focusRequester)
    }

    TVCard(
        onClick = if (enabled) onClick else { {} },
        shape = RoundedCornerShape(10.dp),
        focusedBackgroundColor = TVAccentBlue,
        unfocusedBackgroundColor = Color.Transparent,
        focusedBorderColor = TVAccentCyan,
        unfocusedBorderColor = Color.Transparent,
        modifier = mod
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 14.dp, vertical = 12.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                color = if (enabled) textColor else TVTextMuted,
                fontSize = 14.5.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}
