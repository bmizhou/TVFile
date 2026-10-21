package com.tvfile.app.presentation.screen

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tvfile.app.data.model.FileCategory
import com.tvfile.app.data.model.TVFile
import com.tvfile.app.presentation.component.TVButton
import com.tvfile.app.presentation.component.TVCard
import com.tvfile.app.presentation.theme.*

@Composable
fun WebTransferScreen(
    isServerRunning: Boolean,
    serverUrl: String,
    files: List<TVFile>,
    totalFilesCount: Int,
    selectedCategory: FileCategory,
    onToggleServer: () -> Unit,
    onCategorySelected: (FileCategory) -> Unit,
    onRefreshList: () -> Unit,
    onClearAll: () -> Unit,
    onFileClick: (TVFile) -> Unit,
    onDeleteFile: (TVFile) -> Unit,
    modifier: Modifier = Modifier
) {
    var fileToDelete by remember { mutableStateOf<TVFile?>(null) }
    var showClearConfirm by remember { mutableStateOf(false) }

    val serverBtnFocusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(150)
        try {
            serverBtnFocusRequester.requestFocus()
        } catch (_: Exception) {}
    }

    val totalBytes = files.sumOf { it.size }
    val totalFormatted = formatBytes(totalBytes)

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. 顶部：开启/关闭 HTTP 服务器功能栏 (TVButton 焦点醒目)
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = TVCardBg,
            border = BorderStroke(1.dp, TVBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .padding(horizontal = 20.dp, vertical = 14.dp)
                    .fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    TVButton(
                        onClick = onToggleServer,
                        focusRequester = serverBtnFocusRequester,
                        focusedBackgroundColor = if (isServerRunning) Color(0xFF5E1724) else TVAccentCyan,
                        unfocusedBackgroundColor = if (isServerRunning) Color(0xFF3B1A22) else TVAccentGreen,
                        focusedBorderColor = Color.White,
                        unfocusedBorderColor = if (isServerRunning) TVAccentRed.copy(alpha = 0.6f) else Color.Transparent,
                        contentPadding = PaddingValues(horizontal = 18.dp, vertical = 10.dp)
                    ) { isFocused ->
                        Text(
                            text = if (isServerRunning) "⏹ 关闭 HTTP 服务" else "▶ 开启 HTTP 服务",
                            color = if (isServerRunning) Color.White else Color.Black,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }

                    if (isServerRunning) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .background(TVAccentGreen, shape = CircleShape)
                            )
                            Text(
                                text = "服务已启动 (端口: 45789)",
                                color = TVAccentGreen,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFF0B101A),
                            border = BorderStroke(1.dp, TVAccentCyan.copy(alpha = 0.5f)),
                            modifier = Modifier.padding(horizontal = 4.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(text = "🌐", fontSize = 14.sp)
                                Text(
                                    text = serverUrl,
                                    color = TVAccentCyan,
                                    fontSize = 14.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    } else {
                        Text(
                            text = "HTTP 服务未开启，点击开启后局域网电脑/手机即可免线直传文件至电视",
                            color = TVTextMuted,
                            fontSize = 13.sp
                        )
                    }
                }

                if (isServerRunning) {
                    Text(
                        text = "📁 保存目录: /storage/emulated/0/tvfile/",
                        color = TVTextSecondary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        // 2. 下方：类似用户截图样式的「已接收文件列表」完整面板 (去掉了搜索框)
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = TVSurface,
            border = BorderStroke(1.dp, TVBorder),
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            Column(
                modifier = Modifier
                    .padding(22.dp)
                    .fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // 面板顶栏
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(text = "📁", fontSize = 22.sp)
                        Text(
                            text = "已接收文件列表",
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "共 " + totalFilesCount + " 项 · 占用 " + totalFormatted,
                            color = TVTextSecondary,
                            fontSize = 14.sp
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        TVButton(
                            onClick = onRefreshList,
                            focusedBackgroundColor = TVAccentBlue,
                            unfocusedBackgroundColor = TVCardBg,
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Refresh, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(text = "刷新列表", color = Color.White, fontSize = 13.sp)
                        }

                        TVButton(
                            onClick = { showClearConfirm = true },
                            focusedBackgroundColor = TVAccentRed,
                            unfocusedBackgroundColor = Color(0xFF331518),
                            focusedBorderColor = Color.White,
                            unfocusedBorderColor = TVAccentRed.copy(alpha = 0.5f),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                        ) { isFocused ->
                            Icon(imageVector = Icons.Default.Delete, contentDescription = null, tint = if (isFocused) Color.White else TVAccentRed, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(text = "清空历史", color = if (isFocused) Color.White else TVAccentRed, fontSize = 13.sp)
                        }
                    }
                }

                // 分类 Chip (无搜索框)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    CategoryChip(
                        title = "全部",
                        count = totalFilesCount,
                        isSelected = selectedCategory == FileCategory.ALL,
                        onClick = { onCategorySelected(FileCategory.ALL) }
                    )
                    CategoryChip(
                        title = "安装包",
                        icon = "📦",
                        isSelected = selectedCategory == FileCategory.APK,
                        onClick = { onCategorySelected(FileCategory.APK) }
                    )
                    CategoryChip(
                        title = "视频",
                        icon = "🎬",
                        isSelected = selectedCategory == FileCategory.VIDEO,
                        onClick = { onCategorySelected(FileCategory.VIDEO) }
                    )
                    CategoryChip(
                        title = "音乐",
                        icon = "🎵",
                        isSelected = selectedCategory == FileCategory.AUDIO,
                        onClick = { onCategorySelected(FileCategory.AUDIO) }
                    )
                    CategoryChip(
                        title = "图片",
                        icon = "🖼️",
                        isSelected = selectedCategory == FileCategory.IMAGE,
                        onClick = { onCategorySelected(FileCategory.IMAGE) }
                    )
                }

                if (files.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (isServerRunning) "暂无已接收的文件，请在手机/电脑浏览器中上传" else "服务未启动",
                            color = TVTextMuted,
                            fontSize = 15.sp
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(files, key = { it.id }) { file ->
                            ReceivedFileCard(
                                file = file,
                                onOpen = { onFileClick(file) },
                                onDelete = { fileToDelete = file }
                            )
                        }
                    }
                }
            }
        }
    }

    if (fileToDelete != null) {
        AlertDialog(
            onDismissRequest = { fileToDelete = null },
            title = { Text("确认从电视删除文件？", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold) },
            text = { Text("即将永久删除电视内部存储 tvfile/ 中的文件： " + fileToDelete!!.name, color = TVTextSecondary, fontSize = 14.sp) },
            confirmButton = {
                TVButton(
                    onClick = {
                        val f = fileToDelete!!
                        fileToDelete = null
                        onDeleteFile(f)
                    },
                    focusedBackgroundColor = TVAccentRed,
                    unfocusedBackgroundColor = TVAccentRed
                ) {
                    Text("确认删除", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { fileToDelete = null }) {
                    Text("取消", color = TVTextSecondary)
                }
            },
            containerColor = TVCardBg
        )
    }

    if (showClearConfirm) {
        AlertDialog(
            onDismissRequest = { showClearConfirm = false },
            title = { Text("确认清空所有已接收文件？", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold) },
            text = { Text("此操作将清空电视内部存储 tvfile/ 目录下的全部文件，操作不可逆！", color = TVTextSecondary, fontSize = 14.sp) },
            confirmButton = {
                TVButton(
                    onClick = {
                        showClearConfirm = false
                        onClearAll()
                    },
                    focusedBackgroundColor = TVAccentRed,
                    unfocusedBackgroundColor = TVAccentRed
                ) {
                    Text("确认清空", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearConfirm = false }) {
                    Text("取消", color = TVTextSecondary)
                }
            },
            containerColor = TVCardBg
        )
    }
}

@Composable
private fun CategoryChip(
    title: String,
    count: Int? = null,
    icon: String? = null,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val displayText = if (count != null) "$title ($count)" else title
    TVCard(
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        focusedBackgroundColor = Color(0xFF0F324E),
        unfocusedBackgroundColor = if (isSelected) Color(0xFF0B2538) else Color(0xFF131B2A),
        focusedBorderColor = TVAccentCyan,
        unfocusedBorderColor = if (isSelected) TVAccentCyan else TVBorder
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            if (icon != null) {
                Text(text = icon, fontSize = 14.sp)
            }
            Text(
                text = displayText,
                color = if (isSelected) TVAccentCyan else TVTextSecondary,
                fontSize = 13.5.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
            )
        }
    }
}

@Composable
private fun ReceivedFileCard(
    file: TVFile,
    onOpen: () -> Unit,
    onDelete: () -> Unit
) {
    val (typeIcon, iconColor) = getReceivedFileVisuals(file)

    TVCard(
        onClick = onOpen,
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 18.dp, vertical = 14.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Color(0xFF162033),
                border = BorderStroke(1.dp, Color(0xFF23314A)),
                modifier = Modifier.size(50.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = typeIcon,
                        contentDescription = null,
                        tint = iconColor,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = file.name,
                    color = Color.White,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(text = "💾 " + file.formattedSize, color = TVTextSecondary, fontSize = 12.5.sp)
                    Text(text = "🕒 " + formatDate(file.lastModified), color = TVTextSecondary, fontSize = 12.5.sp)
                    Text(text = "📁 tvfile/", color = TVAccentCyan, fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold)
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                TVButton(
                    onClick = onOpen,
                    focusedBackgroundColor = TVAccentCyan,
                    unfocusedBackgroundColor = Color(0xFF152033),
                    contentPadding = PaddingValues(0.dp),
                    modifier = Modifier.size(38.dp)
                ) {
                    Text(text = "📥", fontSize = 16.sp)
                }

                TVButton(
                    onClick = onDelete,
                    focusedBackgroundColor = TVAccentRed,
                    unfocusedBackgroundColor = Color(0xFF152033),
                    contentPadding = PaddingValues(0.dp),
                    modifier = Modifier.size(38.dp)
                ) {
                    Text(text = "🗑️", fontSize = 16.sp)
                }
            }
        }
    }
}

private fun getReceivedFileVisuals(file: TVFile): Pair<ImageVector, Color> {
    return when (file.category) {
        FileCategory.APK -> Pair(Icons.Default.Android, TVAccentGreen)
        FileCategory.VIDEO -> Pair(Icons.Default.Movie, TVAccentRed)
        FileCategory.AUDIO -> Pair(Icons.Default.MusicNote, Color(0xFFE040FB))
        FileCategory.IMAGE -> Pair(Icons.Default.Image, Color(0xFF18FFFF))
        else -> Pair(Icons.Default.InsertDriveFile, TVTextSecondary)
    }
}

private fun formatBytes(bytes: Long): String {
    if (bytes <= 0) return "0 MB"
    val mb = bytes.toDouble() / (1024.0 * 1024.0)
    return if (mb >= 1000) String.format("%.1f GB", mb / 1024.0) else String.format("%.1f MB", mb)
}

private fun formatDate(ms: Long): String {
    if (ms <= 0) return "未知时间"
    val sdf = java.text.SimpleDateFormat("MM-dd HH:mm", java.util.Locale.getDefault())
    return sdf.format(java.util.Date(ms))
}
