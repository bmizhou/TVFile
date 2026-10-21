package com.tvfile.app.presentation.screen

import android.net.Uri
import android.view.KeyEvent
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.FastRewind
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.ui.StyledPlayerView
import com.tvfile.app.data.model.TVFile
import com.tvfile.app.presentation.theme.TVAccentCyan
import com.tvfile.app.presentation.theme.TVAccentBlue
import com.tvfile.app.presentation.theme.TVBorder
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
    var playerViewRef by remember { mutableStateOf<StyledPlayerView?>(null) }
    var isReleased by remember { mutableStateOf(false) }

    // 控制栏显示与定时隐藏状态
    var isControllerVisible by remember { mutableStateOf(false) }
    var lastInteractionTime by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var isPlaying by remember { mutableStateOf(true) }
    var currentPos by remember { mutableLongStateOf(initialPosition) }
    var totalDuration by remember { mutableLongStateOf(0L) }

    val focusRequester = remember { FocusRequester() }

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

    LaunchedEffect(Unit) {
        delay(100)
        try {
            focusRequester.requestFocus()
        } catch (_: Exception) {}
    }

    // 解决退出播放闪退：安全解绑与释放
    DisposableEffect(Unit) {
        onDispose {
            isReleased = true
            runCatching {
                val pos = exoPlayer.currentPosition
                val dur = exoPlayer.duration
                onSaveProgress(pos, dur)
            }
            runCatching {
                exoPlayer.stop()
            }
            playerViewRef?.player = null
            runCatching {
                exoPlayer.release()
            }
        }
    }

    // 进度与状态刷新协程
    LaunchedEffect(exoPlayer) {
        while (!isReleased) {
            delay(1000)
            if (!isReleased) {
                runCatching {
                    currentPos = exoPlayer.currentPosition
                    totalDuration = exoPlayer.duration.coerceAtLeast(0L)
                    isPlaying = exoPlayer.isPlaying
                    onSaveProgress(currentPos, totalDuration)
                }
            }
        }
    }

    // 控制栏无操作 4 秒后自动淡出隐藏
    LaunchedEffect(lastInteractionTime, isControllerVisible) {
        if (isControllerVisible) {
            delay(4000)
            isControllerVisible = false
        }
    }

    LaunchedEffect(showResumeToast) {
        if (showResumeToast) {
            delay(4500)
            showResumeToast = false
        }
    }

    BackHandler {
        onClose()
    }

    // 遥控器按键拦截与处理函数
    fun handleRemoteKey(keyCode: Int, repeatCount: Int): Boolean {
        lastInteractionTime = System.currentTimeMillis()

        when (keyCode) {
            KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_ENTER -> {
                if (!isReleased) {
                    if (exoPlayer.isPlaying) {
                        exoPlayer.pause()
                        isPlaying = false
                    } else {
                        exoPlayer.play()
                        isPlaying = true
                    }
                    isControllerVisible = true
                }
                return true
            }

            KeyEvent.KEYCODE_DPAD_LEFT -> {
                // 快退 10 秒；长按按倍数递增
                if (!isReleased) {
                    val step = if (repeatCount > 0) 10000L * (repeatCount + 1).coerceAtMost(6) else 10000L
                    val target = maxOf(0L, exoPlayer.currentPosition - step)
                    exoPlayer.seekTo(target)
                    currentPos = target
                    isControllerVisible = true
                }
                return true
            }

            KeyEvent.KEYCODE_DPAD_RIGHT -> {
                // 快进 10 秒；长按递增
                if (!isReleased) {
                    val dur = if (exoPlayer.duration > 0) exoPlayer.duration else Long.MAX_VALUE
                    val step = if (repeatCount > 0) 10000L * (repeatCount + 1).coerceAtMost(6) else 10000L
                    val target = minOf(dur, exoPlayer.currentPosition + step)
                    exoPlayer.seekTo(target)
                    currentPos = target
                    isControllerVisible = true
                }
                return true
            }

            KeyEvent.KEYCODE_DPAD_DOWN, KeyEvent.KEYCODE_DPAD_UP, KeyEvent.KEYCODE_MENU -> {
                // 点击下键/上键/菜单键唤出控制栏
                isControllerVisible = !isControllerVisible
                return true
            }

            KeyEvent.KEYCODE_BACK -> {
                onClose()
                return true
            }
        }
        return false
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .focusRequester(focusRequester)
            .focusable()
            .onPreviewKeyEvent { event ->
                if (event.nativeKeyEvent.action == KeyEvent.ACTION_DOWN) {
                    handleRemoteKey(event.nativeKeyEvent.keyCode, event.nativeKeyEvent.repeatCount)
                } else {
                    false
                }
            }
    ) {
        AndroidView(
            factory = { ctx ->
                StyledPlayerView(ctx).apply {
                    player = exoPlayer
                    useController = false // 使用纯 Compose TV 专属定制控制台
                    playerViewRef = this
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        // 电视专属 OSD 控制底栏 (按遥控器下键/菜单键唤出，4秒自动淡出)
        AnimatedVisibility(
            visible = isControllerVisible,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight(),
                color = Color(0xD90B101A)
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 28.dp, vertical = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // 文件标题与遥控器提示
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = file.name,
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1
                        )
                        Text(
                            text = "遥控器: OK 暂停/播放 · ◀ ▶ 快退快进10s · 再次按下键隐藏",
                            color = TVAccentCyan,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    // 进度条
                    val progressRatio = if (totalDuration > 0) (currentPos.toFloat() / totalDuration).coerceIn(0f, 1f) else 0f
                    LinearProgressIndicator(
                        progress = { progressRatio },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp),
                        color = TVAccentCyan,
                        trackColor = TVBorder
                    )

                    // 时间与播放状态
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Surface(
                                shape = CircleShape,
                                color = TVAccentBlue,
                                modifier = Modifier.size(36.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                            }
                            Text(
                                text = if (isPlaying) "正在播放" else "已暂停",
                                color = Color.White,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        Text(
                            text = "${formatTime(currentPos)} / ${formatTime(totalDuration)}",
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // 断点续播浮动提示框 (居中下方)
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
                border = BorderStroke(1.5.dp, TVAccentCyan.copy(alpha = 0.8f)),
                shadowElevation = 12.dp
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "💡 已为您恢复到上次播放位置: $resumeTimeText",
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Button(
                        onClick = {
                            if (!isReleased) {
                                runCatching { exoPlayer.seekTo(0) }
                            }
                            showResumeToast = false
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = TVAccentCyan),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 4.dp),
                        modifier = Modifier.height(32.dp)
                    ) {
                        Text(
                            text = "从头播放",
                            color = Color.Black,
                            fontSize = 12.sp,
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
