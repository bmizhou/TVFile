package com.tvfile.app.presentation.screen

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.ui.StyledPlayerView
import com.tvfile.app.data.model.TVFile
import com.tvfile.app.presentation.theme.TVAccentCyan
import com.tvfile.app.presentation.theme.TVCardBg
import kotlinx.coroutines.delay

@Composable
fun VideoPlayerOverlay(
    file: TVFile,
    playUri: String,
    initialPosition: Long = 0L,
    onSaveProgress: (positionMs: Long, durationMs: Long) -> Unit,
    onClose: () -> Unit
) {
    val context = LocalContext.current
    var showResumeToast by remember { mutableStateOf(initialPosition > 5000L) }
    var resumeTimeText by remember { mutableStateOf(formatTime(initialPosition)) }

    val exoPlayer = remember {
        ExoPlayer.Builder(context).build().apply {
            val mediaItem = MediaItem.fromUri(Uri.parse(playUri))
            setMediaItem(mediaItem)
            prepare()
            if (initialPosition > 5000L) {
                seekTo(initialPosition)
            }
            playWhenReady = true
        }
    }

    // 退出时与周期性持久化保存进度
    DisposableEffect(Unit) {
        onDispose {
            val pos = exoPlayer.currentPosition
            val dur = exoPlayer.duration
            onSaveProgress(pos, dur)
            exoPlayer.release()
        }
    }

    // 周期性保存进度 (每 3 秒)
    LaunchedEffect(exoPlayer) {
        while (true) {
            delay(3000)
            if (exoPlayer.isPlaying) {
                onSaveProgress(exoPlayer.currentPosition, exoPlayer.duration)
            }
        }
    }

    // 续播提示框 4 秒后自动淡出
    LaunchedEffect(showResumeToast) {
        if (showResumeToast) {
            delay(4500)
            showResumeToast = false
        }
    }

    BackHandler {
        onClose()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        AndroidView(
            factory = { ctx ->
                StyledPlayerView(ctx).apply {
                    player = exoPlayer
                    useController = true
                    setShowNextButton(false)
                    setShowPreviousButton(false)
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        // 顶部退出按钮与视频名称
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = file.name,
                color = Color.White,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold
            )
            Button(
                onClick = onClose,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0x991F293D))
            ) {
                Text(text = "✕ 退出 (BACK)", color = TVAccentCyan, fontSize = 12.sp)
            }
        }

        // 电视专属断点续播浮动提示框 (居中下方)
        AnimatedVisibility(
            visible = showResumeToast,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 90.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = Color(0xE6131D2E),
                border = BorderStroke(1.dp, TVAccentCyan.copy(alpha = 0.6f)),
                shadowElevation = 10.dp
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(
                        text = "💡 已为您恢复到上次播放位置: $resumeTimeText",
                        color = Color.White,
                        fontSize = 13.sp
                    )
                    Button(
                        onClick = {
                            exoPlayer.seekTo(0)
                            showResumeToast = false
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = TVAccentCyan),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                        modifier = Modifier.height(30.dp)
                    ) {
                        Text(
                            text = "从头播放",
                            color = Color.Black,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

private fun formatTime(ms: Long): String {
    val totalSeconds = (ms / 1000).toInt()
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    return if (hours > 0) {
        String.format("%02d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format("%02d:%02d", minutes, seconds)
    }
}
