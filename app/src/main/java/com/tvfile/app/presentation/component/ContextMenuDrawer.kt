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
import androidx.compose.ui.text.style.TextOverflow
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
            .width(340.dp)
            .fillMaxHeight(),
        color = TVSurface,
        border = BorderStroke(2.dp, TVAccentCyan.copy(alpha = 0.8f)),
        shape = RoundedCornerShape(topStart = 20.dp, bottomStart = 20.dp),
        shadowElevation = 24.dp
    ) {
        Column(
            modifier = Modifier
                .padding(24.dp)
                .fillMaxHeight(),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                text = "⚡ 文件操作",
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = targetFile?.name ?: "未选择文件",
                color = TVTextSecondary,
                fontSize = 15.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
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
                isDanger = true,
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
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(vertical = 12.dp)
            ) {
                Text(text = "关闭菜单 (BACK)", color = TVTextSecondary, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

/**
 * 【解决问题 3】：大字号 (18sp) + 彻底解决焦点背景与文字颜色冲突
 */
@Composable
private fun TVMenuItem(
    title: String,
    enabled: Boolean,
    isDanger: Boolean = false,
    focusRequester: FocusRequester? = null,
    onClick: () -> Unit
) {
    var mod = Modifier.fillMaxWidth()
    if (focusRequester != null) {
        mod = mod.focusRequester(focusRequester)
    }

    // 获焦背景与边框：危险操作用深红黑底+红框，普通项用深藏青底+青蓝发光框
    val focusedBg = if (isDanger) Color(0xFF42151B) else Color(0xFF0C385C)
    val focusedBorder = if (isDanger) TVAccentRed else TVAccentCyan
    val unfocusedTextColor = if (isDanger) TVAccentRed else Color.White

    TVCard(
        onClick = if (enabled) onClick else { {} },
        shape = RoundedCornerShape(12.dp),
        focusedBackgroundColor = focusedBg,
        unfocusedBackgroundColor = Color.Transparent,
        focusedBorderColor = focusedBorder,
        unfocusedBorderColor = Color.Transparent,
        focusedBorderWidth = 2.5.dp,
        modifier = mod
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 14.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                // 关键点：获焦时一律采用最高对比度纯白高亮字，消除任何色差冲突
                color = if (!enabled) TVTextMuted else Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
