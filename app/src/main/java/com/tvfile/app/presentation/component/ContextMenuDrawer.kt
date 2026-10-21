package com.tvfile.app.presentation.component

import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
    Surface(
        modifier = modifier
            .width(280.dp)
            .fillMaxHeight(),
        color = TVSurface,
        border = BorderStroke(1.dp, TVAccentCyan.copy(alpha = 0.4f)),
        shape = RoundedCornerShape(topStart = 16.dp, bottomStart = 16.dp),
        shadowElevation = 16.dp
    ) {
        Column(
            modifier = Modifier
                .padding(20.dp)
                .fillMaxHeight(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = "⚡ 文件操作",
                color = TVTextPrimary,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = targetFile?.name ?: "未选择文件",
                color = TVTextSecondary,
                fontSize = 12.sp,
                maxLines = 1
            )
            HorizontalDivider(color = TVBorder, thickness = 1.dp)

            MenuItem(title = "📋 复制 (Copy)", enabled = targetFile != null, onClick = onCopy)
            MenuItem(title = "✂️ 剪切 (Cut)", enabled = targetFile != null, onClick = onCut)
            MenuItem(title = "📥 粘贴到此处", enabled = canPaste, onClick = onPaste)
            MenuItem(title = "🗑️ 删除文件", enabled = targetFile != null, color = TVAccentRed, onClick = onDelete)
            MenuItem(title = "✏️ 重命名", enabled = targetFile != null, onClick = onRename)
            MenuItem(title = "ℹ️ 属性详情", enabled = targetFile != null, onClick = onDetails)

            Spacer(modifier = Modifier.weight(1f))
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = TVCardBg),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(text = "关闭菜单 (BACK)", color = TVTextSecondary, fontSize = 12.sp)
            }
        }
    }
}

@Composable
private fun MenuItem(
    title: String,
    enabled: Boolean,
    color: Color = TVTextPrimary,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = Color.Transparent,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled, onClick = onClick)
    ) {
        Text(
            text = title,
            color = if (enabled) color else TVTextMuted,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(vertical = 10.dp, horizontal = 12.dp)
        )
    }
}
