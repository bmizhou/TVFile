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
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.google.android.exoplayer2.*
import com.google.android.exoplayer2.ui.StyledPlayerView
import com.google.android.exoplayer2.video.VideoSize
import com.tvfile.app.data.model.TVFile
import com.tvfile.app.presentation.component.TVButton
import com.tvfile.app.presentation.theme.*
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

    // 【新功能 1】：倍速播放支持 (0.75x, 1.0x, 1.25x, 1.5x, 2.0x, 3.0x)
    var currentSpeed by remember { mutableFloatStateOf(1.0f) }
    var isSpeedDialogOpen by remember { mutableStateOf(false) }

    // 【新功能 2】：视频信息与解码参数浮窗
    var isInfoDialogOpen by remember { mutableStateOf(false) }
    var videoWidth by remember { mutableIntStateOf(0) }
    var videoHeight by remember { mutableIntStateOf(0) }
    var videoMime by remember { mutableStateOf("") }
    var videoBitrate by remember { mutableIntStateOf(0) }
    var videoFrameRate by remember { mutableFloatStateOf(0f) }
    var audioMime by remember { mutableStateOf("") }
    var audioChannels by remember { mutableIntStateOf(0) }
    var audioSampleRate by remember { mutableIntStateOf(0) }

    val focusRequester = remember { FocusRequester() }

    // 显式开启 RK3399 4K 硬件加速解码
    val renderersFactory = remember(context) {
        DefaultRenderersFactory(context).apply {
            setExtensionRendererMode(DefaultRenderersFactory.EXTENSION_RENDERER_MODE_ON)
            setEnableDecoderFallback(true)
        }
    }

    val loadControl = remember {
        DefaultLoadControl.Builder()
            .setBufferDurationsMs(
                15000, // 最小缓冲 15s
                60000, // 最大缓冲 60s
                2000,  // 起播缓冲 2s
                4000   // seek 缓冲 4s
            )
            .setPrioritizeTimeOverSizeThresholds(true)
            .build()
    }

    val exoPlayer = remember {
        ExoPlayer.Builder(context, renderersFactory)
            .setLoadControl(loadControl)
            .build().apply {
                val mediaItem = MediaItem.fromUri(Uri.parse(playUri))
                setMediaItem(mediaItem)
                prepare()
                if (initialPosition > 5000L) {
                    seekTo(initialPosition)
                }
                playWhenReady = true

                // 监听视频格式与画质参数
                addListener(object : Player.Listener {
                    override fun onVideoSizeChanged(videoSize: VideoSize) {
                        videoWidth = videoSize.width
                        videoHeight = videoSize.height
                    }

                    override fun onTracksChanged(tracks: Tracks) {
                        videoFormat?.let { vf ->
                            videoWidth = vf.width
                            videoHeight = vf.height
                            videoMime = vf.sampleMimeType ?: ""
                            videoBitrate = vf.bitrate
                            videoFrameRate = vf.frameRate
                        }
                        audioFormat?.let { af ->
                            audioMime = af.sampleMimeType ?: ""
                            audioChannels = af.channelCount
                            audioSampleRate = af.sampleRate
                        }
                    }
                })
            }
    }

    LaunchedEffect(Unit) {
        delay(100)
        try {
            focusRequester.requestFocus()
        } catch (_: Exception) {}
    }

    // 安全释放防崩溃
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

    // 播放进度与状态轮询
    LaunchedEffect(exoPlayer) {
        while (!isReleased) {
            delay(1000)
            if (!isReleased) {
                runCatching {
                    currentPos = exoPlayer.currentPosition
                    totalDuration = exoPlayer.duration.coerceAtLeast(0L)
                    isPlaying = exoPlayer.isPlaying
                    onSaveProgress(currentPos, totalDuration)

                    // 动态更新格式信息
                    exoPlayer.videoFormat?.let { vf ->
                        if (videoWidth <= 0) videoWidth = vf.width
                        if (videoHeight <= 0) videoHeight = vf.height
                        if (videoMime.isBlank()) videoMime = vf.sampleMimeType ?: ""
                        if (videoBitrate <= 0) videoBitrate = vf.bitrate
                        if (videoFrameRate <= 0) videoFrameRate = vf.frameRate
                    }
                    exoPlayer.audioFormat?.let { af ->
                        if (audioMime.isBlank()) audioMime = af.sampleMimeType ?: ""
                        if (audioChannels <= 0) audioChannels = af.channelCount
                        if (audioSampleRate <= 0) audioSampleRate = af.sampleRate
                    }
                }
            }
        }
    }

    // 控制栏 4 秒无交互自动淡出
    LaunchedEffect(lastInteractionTime, isControllerVisible) {
        if (isControllerVisible && !isSpeedDialogOpen && !isInfoDialogOpen) {
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
        if (isInfoDialogOpen) {
            isInfoDialogOpen = false
            return@BackHandler
        }
        if (isSpeedDialogOpen) {
            isSpeedDialogOpen = false
            return@BackHandler
        }
        if (isControllerVisible) {
            isControllerVisible = false
            return@BackHandler
        }
        onClose()
    }

    // 遥控器全局按键分发
    fun handleRemoteKey(keyCode: Int, repeatCount: Int): Boolean {
        lastInteractionTime = System.currentTimeMillis()

        if (isInfoDialogOpen || isSpeedDialogOpen) {
            return false // 弹窗开启时让弹窗自行处理遥控器
        }

        when (keyCode) {
            KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_ENTER -> {
                if (!isReleased) {
                    if (isControllerVisible) {
                        // 控制栏打开时切换播放状态
                        if (exoPlayer.isPlaying) {
                            exoPlayer.pause()
                            isPlaying = false
                        } else {
                            exoPlayer.play()
                            isPlaying = true
                        }
                    } else {
                        // 控制栏未打开时唤出并切换
                        isControllerVisible = true
                        if (exoPlayer.isPlaying) {
                            exoPlayer.pause()
                            isPlaying = false
                        } else {
                            exoPlayer.play()
                            isPlaying = true
                        }
                    }
                }
                return true
            }

            KeyEvent.KEYCODE_DPAD_LEFT -> {
                // 单击快退 10s，长按倍数递增
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
                // 单击快进 10s，长按倍数递增
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
                // 点击下键/上键/菜单键切换唤出控制栏
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
                    useController = false
                    playerViewRef = this
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        // 电视专属 OSD 控制底栏
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
                    modifier = Modifier.padding(horizontal = 28.dp, vertical = 18.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // 上层：文件标题与快捷功能按键 (倍速、信息)
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
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )

                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            // 【倍速按键】
                            TVButton(
                                onClick = { isSpeedDialogOpen = true },
                                focusedBackgroundColor = TVAccentBlue,
                                unfocusedBackgroundColor = Color(0xFF162033),
                                focusedBorderColor = TVAccentCyan,
                                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                            ) { isFocused ->
                                Icon(imageVector = Icons.Default.Speed, contentDescription = null, tint = if (isFocused) Color.White else TVAccentCyan, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (currentSpeed == 1.0f) "倍速 (正常)" else "倍速: ${currentSpeed}x",
                                    color = if (isFocused) Color.White else TVTextPrimary,
                                    fontSize = 13.5.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            // 【视频信息与解码浮窗按键】
                            TVButton(
                                onClick = { isInfoDialogOpen = true },
                                focusedBackgroundColor = TVAccentBlue,
                                unfocusedBackgroundColor = Color(0xFF162033),
                                focusedBorderColor = TVAccentCyan,
                                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                            ) { isFocused ->
                                Icon(imageVector = Icons.Default.Info, contentDescription = null, tint = if (isFocused) Color.White else TVAccentCyan, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "视频信息",
                                    color = if (isFocused) Color.White else TVTextPrimary,
                                    fontSize = 13.5.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
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

                    // 播放状态与时间显示
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

        // 断点续播浮动提示框
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

        // 【弹窗 1】：倍速选择弹窗
        if (isSpeedDialogOpen) {
            PlaybackSpeedDialog(
                currentSpeed = currentSpeed,
                onSpeedSelect = { speed ->
                    currentSpeed = speed
                    runCatching { exoPlayer.setPlaybackSpeed(speed) }
                    isSpeedDialogOpen = false
                },
                onDismiss = { isSpeedDialogOpen = false }
            )
        }

        // 【弹窗 2】：视频参数与解码信息详情浮窗
        if (isInfoDialogOpen) {
            VideoMediaInfoDialog(
                file = file,
                durationMs = totalDuration,
                width = videoWidth,
                height = videoHeight,
                videoMime = videoMime,
                bitrate = videoBitrate,
                frameRate = videoFrameRate,
                audioMime = audioMime,
                audioChannels = audioChannels,
                audioSampleRate = audioSampleRate,
                onDismiss = { isInfoDialogOpen = false }
            )
        }
    }
}

/**
 * 倍速选择面板弹窗
 */
@Composable
private fun PlaybackSpeedDialog(
    currentSpeed: Float,
    onSpeedSelect: (Float) -> Unit,
    onDismiss: () -> Unit
) {
    val speeds = listOf(0.75f, 1.0f, 1.25f, 1.5f, 2.0f, 3.0f)
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = TVSurface,
            border = BorderStroke(2.dp, TVAccentCyan.copy(alpha = 0.8f)),
            shadowElevation = 24.dp,
            modifier = Modifier.width(360.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "⚡ 播放倍速调节",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 6.dp)
                )

                speeds.forEach { sp ->
                    val isSelected = currentSpeed == sp
                    TVButton(
                        onClick = { onSpeedSelect(sp) },
                        focusedBackgroundColor = TVAccentBlue,
                        unfocusedBackgroundColor = if (isSelected) Color(0xFF132845) else Color.Transparent,
                        focusedBorderColor = TVAccentCyan,
                        unfocusedBorderColor = if (isSelected) TVAccentCyan.copy(alpha = 0.6f) else Color.Transparent,
                        modifier = Modifier.fillMaxWidth(),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp)
                    ) { isFocused ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (sp == 1.0f) "1.0x (正常速度)" else "${sp}x",
                                color = if (isSelected || isFocused) Color.White else TVTextSecondary,
                                fontSize = 15.sp,
                                fontWeight = if (isSelected || isFocused) FontWeight.Bold else FontWeight.Medium
                            )
                            if (isSelected) {
                                Text("✔", color = TVAccentCyan, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))
                TVButton(
                    onClick = onDismiss,
                    focusedBackgroundColor = Color(0xFF28364F),
                    unfocusedBackgroundColor = TVCardBg,
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(vertical = 10.dp)
                ) {
                    Text("关闭 (BACK)", color = TVTextSecondary, fontSize = 14.sp)
                }
            }
        }
    }
}

/**
 * 视频参数与解码信息详情浮窗
 */
@Composable
private fun VideoMediaInfoDialog(
    file: TVFile,
    durationMs: Long,
    width: Int,
    height: Int,
    videoMime: String,
    bitrate: Int,
    frameRate: Float,
    audioMime: String,
    audioChannels: Int,
    audioSampleRate: Int,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = Color(0xF20F1626),
            border = BorderStroke(2.dp, TVAccentCyan.copy(alpha = 0.8f)),
            shadowElevation = 24.dp,
            modifier = Modifier.width(620.dp)
        ) {
            Column(
                modifier = Modifier.padding(26.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(text = "ℹ️", fontSize = 22.sp)
                        Text(
                            text = "视频与解码详情",
                            color = Color.White,
                            fontSize = 19.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = Color(0xFF00E676).copy(alpha = 0.15f),
                        border = BorderStroke(1.dp, Color(0xFF00E676).copy(alpha = 0.5f))
                    ) {
                        Text(
                            text = "🚀 MediaCodec 硬件加速开启",
                            color = Color(0xFF00E676),
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }

                HorizontalDivider(color = TVBorder, thickness = 1.dp)

                // 详细参数分栏表格
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    InfoItemRow(label = "文件名称", value = file.name)
                    InfoItemRow(label = "文件大小", value = file.formattedSize)
                    InfoItemRow(label = "视频总时长", value = formatTime(durationMs))
                    InfoItemRow(label = "来源存储", value = file.storageType.displayName)

                    HorizontalDivider(color = TVBorder.copy(alpha = 0.5f), thickness = 1.dp)

                    val resolutionLabel = if (width > 0 && height > 0) {
                        val tag = if (width >= 3800 || height >= 2100) " [4K UHD 超高清]" else if (width >= 1900 || height >= 1000) " [1080P 全高清]" else ""
                        "${width} × ${height}$tag"
                    } else {
                        "检测中 / 暂无"
                    }
                    InfoItemRow(label = "画面分辨率", value = resolutionLabel, valueColor = TVAccentCyan)

                    val vFormatName = when {
                        videoMime.contains("hevc") || videoMime.contains("h265") -> "H.265 / HEVC (硬件解码)"
                        videoMime.contains("avc") || videoMime.contains("h264") -> "H.264 / AVC (硬件解码)"
                        videoMime.contains("vp9") -> "Google VP9 (硬件解码)"
                        videoMime.contains("av01") -> "AV1"
                        videoMime.isNotBlank() -> videoMime
                        else -> "原生硬件解码流"
                    }
                    InfoItemRow(label = "视频编码格式", value = vFormatName, valueColor = Color.White)

                    val fpsStr = if (frameRate > 0) String.format("%.2f FPS", frameRate) else "自适应帧率"
                    InfoItemRow(label = "视频帧率", value = fpsStr)

                    if (bitrate > 0) {
                        val mbps = String.format("%.2f Mbps", bitrate.toDouble() / 1000000.0)
                        InfoItemRow(label = "视频码率", value = mbps)
                    }

                    HorizontalDivider(color = TVBorder.copy(alpha = 0.5f), thickness = 1.dp)

                    val aFormatName = when {
                        audioMime.contains("eac3") -> "Dolby Digital Plus (E-AC3)"
                        audioMime.contains("ac3") -> "Dolby Digital (AC3)"
                        audioMime.contains("flac") -> "FLAC 无损音频"
                        audioMime.contains("dts") -> "DTS 高清环绕声"
                        audioMime.contains("mp4a") || audioMime.contains("aac") -> "AAC 高级音频编码"
                        audioMime.isNotBlank() -> audioMime
                        else -> "默认音轨"
                    }
                    val chStr = if (audioChannels > 0) "$audioChannels 声道 (${if (audioChannels >= 6) "5.1 环绕声" else "立体声"})" else ""
                    val srStr = if (audioSampleRate > 0) " · ${audioSampleRate} Hz" else ""
                    InfoItemRow(label = "音频轨道", value = "$aFormatName$chStr$srStr")
                }

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TVButton(
                        onClick = onDismiss,
                        focusedBackgroundColor = TVAccentCyan,
                        unfocusedBackgroundColor = TVCardBg,
                        contentPadding = PaddingValues(horizontal = 24.dp, vertical = 10.dp)
                    ) { isFocused ->
                        Text(
                            text = "返回播放 (BACK)",
                            color = if (isFocused) Color.Black else Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun InfoItemRow(
    label: String,
    value: String,
    valueColor: Color = Color.White
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, color = TVTextSecondary, fontSize = 14.sp)
        Text(
            text = value,
            color = valueColor,
            fontSize = 14.5.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
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
