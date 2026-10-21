package com.tvfile.app.presentation.screen

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.MediaItem
import com.tvfile.app.data.model.TVFile
import com.tvfile.app.presentation.theme.*

@Composable
fun AudioPlayerOverlay(
    file: TVFile,
    playUri: String,
    onClose: () -> Unit
) {
    val context = LocalContext.current
    var isPlaying by remember { mutableStateOf(true) }

    val exoPlayer = remember {
        ExoPlayer.Builder(context).build().apply {
            val mediaItem = MediaItem.fromUri(Uri.parse(playUri))
            setMediaItem(mediaItem)
            prepare()
            playWhenReady = true
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            exoPlayer.release()
        }
    }

    BackHandler {
        onClose()
    }

    // 黑胶旋转动画
    val infiniteTransition = rememberInfiniteTransition(label = "disc_spin")
    val angle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 8000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "angle"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xE60B0F17)),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = TVSurface,
            border = BorderStroke(1.dp, TVAccentCyan.copy(alpha = 0.5f)),
            modifier = Modifier.width(560.dp)
        ) {
            Row(
                modifier = Modifier.padding(24.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                // 黑胶唱片
                Surface(
                    shape = CircleShape,
                    color = Color(0xFF0D121C),
                    border = BorderStroke(3.dp, TVBorder),
                    modifier = Modifier
                        .size(130.dp)
                        .rotate(if (isPlaying) angle else 0f)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.MusicNote,
                            contentDescription = null,
                            tint = TVAccentCyan,
                            modifier = Modifier.size(44.dp)
                        )
                    }
                }

                // 歌曲信息与控制
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = file.name,
                        color = TVTextPrimary,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1
                    )
                    Text(
                        text = "无损音频播放 · ${file.formattedSize}",
                        color = TVAccentCyan,
                        fontSize = 12.sp
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Button(
                            onClick = {
                                if (isPlaying) exoPlayer.pause() else exoPlayer.play()
                                isPlaying = !isPlaying
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = TVAccentBlue)
                        ) {
                            Text(if (isPlaying) "⏸ 暂停" else "▶ 播放", color = Color.White)
                        }
                        Button(
                            onClick = onClose,
                            colors = ButtonDefaults.buttonColors(containerColor = TVCardBg)
                        ) {
                            Text("关闭 (BACK)", color = TVTextSecondary)
                        }
                    }
                }
            }
        }
    }
}
