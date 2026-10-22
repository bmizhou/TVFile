package com.tvfile.app.presentation.screen

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.InsertDriveFile
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tvfile.app.data.model.FileCategory
import com.tvfile.app.data.model.TVFile
import com.tvfile.app.data.model.ViewMode
import com.tvfile.app.presentation.component.TVCard
import com.tvfile.app.presentation.theme.*
import kotlinx.coroutines.delay

@Composable
fun FileBrowserScreen(
    files: List<TVFile>,
    viewMode: ViewMode,
    progressMap: Map<String, Float> = emptyMap(),
    onFileClick: (TVFile) -> Unit,
    onFileLongClick: (TVFile) -> Unit,
    modifier: Modifier = Modifier
) {
    val configuration = LocalConfiguration.current
    val isPortrait = configuration.orientation == Configuration.ORIENTATION_PORTRAIT

    val firstItemFocusRequester = remember { FocusRequester() }

    // 彻底解决切目录焦点跳回左侧Tab的核心优化：
    // 移除 120ms 延迟，数据一旦就绪立即同步聚焦首项，零闪烁零逃逸！
    LaunchedEffect(files) {
        if (files.isNotEmpty()) {
            runCatching {
                firstItemFocusRequester.requestFocus()
            }
        }
    }

    if (files.isEmpty()) {
        Box(
            modifier = modifier.fillMaxSize().focusGroup(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "当前目录下无文件",
                color = TVTextMuted,
                fontSize = 17.sp
            )
        }
        return
    }

    if (viewMode == ViewMode.GRID) {
        // 【解决问题 4】：竖屏 3 列，横屏 5 列
        val columnCount = if (isPortrait) 3 else 5
        LazyVerticalGrid(
            columns = GridCells.Fixed(columnCount),
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

/**
 * 【解决问题 3】：大幅加大主标题 (17.5sp) 与次标题 (14.5sp)
 */
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

            // 主标题加粗加大至 17.5sp
            Text(
                text = file.name,
                color = TVTextPrimary,
                fontSize = 17.5.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            // 次标题加大至 14.5sp
            Text(
                text = file.formattedSize,
                color = TVTextSecondary,
                fontSize = 14.5.sp,
                fontWeight = FontWeight.Medium
            )

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
                modifier = Modifier.size(36.dp)
            )
            Spacer(modifier = Modifier.width(18.dp))
            Column(modifier = Modifier.weight(2f)) {
                // 主标题 18sp
                Text(
                    text = file.name,
                    color = TVTextPrimary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (file.category == FileCategory.VIDEO && playbackProgress > 0.02f) {
                    val percent = (playbackProgress * 100).toInt()
                    Text(
                        text = "已播放 $percent%",
                        color = TVAccentCyan,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
            // 次标题 15sp
            Text(
                text = file.formattedSize,
                color = TVTextSecondary,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = if (file.isDirectory) "进入 ❯" else "打开 ❯",
                color = TVAccentCyan,
                fontSize = 15.sp,
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
