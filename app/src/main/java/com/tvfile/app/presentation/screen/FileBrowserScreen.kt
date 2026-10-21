package com.tvfile.app.presentation.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.remember
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.foundation.lazy.grid.itemsIndexed
import kotlinx.coroutines.delay
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.InsertDriveFile
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tvfile.app.data.model.FileCategory
import com.tvfile.app.data.model.TVFile
import com.tvfile.app.data.model.ViewMode
import com.tvfile.app.presentation.component.TVCard
import com.tvfile.app.presentation.theme.*

@Composable
fun FileBrowserScreen(
    files: List<TVFile>,
    viewMode: ViewMode,
    progressMap: Map<String, Float> = emptyMap(),
    onFileClick: (TVFile) -> Unit,
    onFileLongClick: (TVFile) -> Unit,
    modifier: Modifier = Modifier
) {
        val firstItemFocusRequester = remember { FocusRequester() }

    // 解决进入文件夹焦点跳回左侧 Tab 的核心修复：进入新目录后自动锁定首个文件
    LaunchedEffect(files) {
        if (files.isNotEmpty()) {
            kotlinx.coroutines.delay(120)
            try {
                firstItemFocusRequester.requestFocus()
            } catch (_: Exception) {}
        }
    }

    if (files.isEmpty()) {
        Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "当前目录下无文件",
                color = TVTextMuted,
                fontSize = 16.sp
            )
        }
        return
    }

    if (viewMode == ViewMode.GRID) {
        // 固定 5 列排版，保证大屏远视距下图标巨大、文字清晰
        LazyVerticalGrid(
            columns = GridCells.Fixed(5),
            contentPadding = PaddingValues(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = modifier.fillMaxSize()
        ) {
            itemsIndexed(files, key = { _, file -> file.id }) { index, file ->
                val progress = progressMap[file.id] ?: 0f
                val mod = if (index == 0) Modifier.focusRequester(firstItemFocusRequester) else Modifier
                FileGridCard(
                    file = file,
                    playbackProgress = progress,
                    modifier = mod,
                    onClick = { onFileClick(file) },
                    onLongClick = { onFileLongClick(file) }
                )
            }
        }
    } else {
        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = modifier.fillMaxSize()
        ) {
            items(files, key = { it.id }) { file ->
                val progress = progressMap[file.id] ?: 0f
                FileListRow(
                    file = file,
                    playbackProgress = progress,
                    onClick = { onFileClick(file) },
                    onLongClick = { onFileLongClick(file) }
                )
            }
        }
    }
}

@Composable
private fun FileGridCard(
    file: TVFile,
    playbackProgress: Float = 0f,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    val (icon, iconColor) = getFileVisuals(file)

    TVCard(
        onClick = onClick,
        onLongClick = onLongClick,
        shape = RoundedCornerShape(16.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = TVSurface,
                modifier = Modifier.size(76.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = iconColor,
                        modifier = Modifier.size(46.dp)
                    )
                }
            }

            Text(
                text = file.name,
                color = TVTextPrimary,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Text(
                text = file.formattedSize,
                color = TVTextSecondary,
                fontSize = 13.sp
            )

            // 视频断点记忆播放进度条指示
            if (file.category == FileCategory.VIDEO && playbackProgress > 0.02f) {
                LinearProgressIndicator(
                    progress = { playbackProgress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp),
                    color = TVAccentCyan,
                    trackColor = TVBorder
                )
            }
        }
    }
}

@Composable
private fun FileListRow(
    file: TVFile,
    playbackProgress: Float = 0f,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    val (icon, iconColor) = getFileVisuals(file)

    TVCard(
        onClick = onClick,
        onLongClick = onLongClick,
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 20.dp, vertical = 14.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconColor,
                modifier = Modifier.size(34.dp)
            )
            Spacer(modifier = Modifier.width(18.dp))
            Column(modifier = Modifier.weight(2f)) {
                Text(
                    text = file.name,
                    color = TVTextPrimary,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (file.category == FileCategory.VIDEO && playbackProgress > 0.02f) {
                    val percent = (playbackProgress * 100).toInt()
                    Text(
                        text = "已播放 $percent%",
                        color = TVAccentCyan,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
            Text(
                text = file.formattedSize,
                color = TVTextSecondary,
                fontSize = 14.sp,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = if (file.isDirectory) "进入 ❯" else "打开 ❯",
                color = TVAccentCyan,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

private fun getFileVisuals(file: TVFile): Pair<ImageVector, Color> {
    return if (file.isDirectory) {
        Pair(Icons.Default.Folder, TVAccentOrange)
    } else {
        when (file.category) {
            FileCategory.VIDEO -> Pair(Icons.Default.Movie, TVAccentRed)
            FileCategory.AUDIO -> Pair(Icons.Default.MusicNote, Color(0xFFE040FB))
            FileCategory.IMAGE -> Pair(Icons.Default.Image, Color(0xFF18FFFF))
            FileCategory.APK -> Pair(Icons.Default.Android, TVAccentGreen)
            FileCategory.DOCUMENT -> Pair(Icons.Default.Description, TVAccentBlue)
            else -> Pair(Icons.AutoMirrored.Filled.InsertDriveFile, TVTextSecondary)
        }
    }
}
