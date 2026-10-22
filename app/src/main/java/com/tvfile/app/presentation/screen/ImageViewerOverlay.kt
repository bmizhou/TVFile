package com.tvfile.app.presentation.screen

import android.view.KeyEvent
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import com.tvfile.app.data.model.TVFile
import com.tvfile.app.presentation.theme.TVAccentCyan
import kotlinx.coroutines.delay
import java.io.File

@Composable
fun ImageViewerOverlay(
    file: TVFile,
    imageUri: String,
    onClose: () -> Unit
) {
    val focusRequester = remember { FocusRequester() }
    var isInfoVisible by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        delay(100)
        try {
            focusRequester.requestFocus()
        } catch (_: Exception) {}
    }

    // 图片标题与大小提示在 3.5 秒后自动淡出隐藏，保证 100% 纯净无遮挡
    LaunchedEffect(isInfoVisible) {
        if (isInfoVisible) {
            delay(3500)
            isInfoVisible = false
        }
    }

    BackHandler {
        onClose()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .focusRequester(focusRequester)
            .focusable()
            .onPreviewKeyEvent { event ->
                if (event.nativeKeyEvent.action == KeyEvent.ACTION_DOWN) {
                    when (event.nativeKeyEvent.keyCode) {
                        KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_ENTER,
                        KeyEvent.KEYCODE_DPAD_DOWN, KeyEvent.KEYCODE_DPAD_UP,
                        KeyEvent.KEYCODE_MENU -> {
                            // 按 OK / 下 / 上 / 菜单键可随时唤醒或隐藏信息条
                            isInfoVisible = !isInfoVisible
                            true
                        }
                        KeyEvent.KEYCODE_BACK -> {
                            onClose()
                            true
                        }
                        else -> false
                    }
                } else {
                    false
                }
            },
        contentAlignment = Alignment.Center
    ) {
        val modelData = if (imageUri.startsWith("http://") || imageUri.startsWith("https://") || imageUri.startsWith("content://")) {
            imageUri
        } else {
            File(file.path)
        }

        // 全屏沉浸展示大图
        SubcomposeAsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(modelData)
                .crossfade(true)
                .build(),
            loading = {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                    CircularProgressIndicator(color = TVAccentCyan)
                }
            },
            error = {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                    Text("无法加载图片: ${file.name}", color = Color.White, fontSize = 16.sp)
                }
            },
            contentDescription = null,
            contentScale = ContentScale.Fit,
            modifier = Modifier.fillMaxSize()
        )

        // 仅在刚进入或按键时临时呈现的底部半透明胶囊信息条 (彻底移除右上角关闭按钮，不遮挡画面)
        AnimatedVisibility(
            visible = isInfoVisible,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 32.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = Color(0xD90E1726),
                shadowElevation = 8.dp
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 22.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = file.name,
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "大小: ${file.formattedSize}",
                        color = TVAccentCyan,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "按 BACK 退出 · 按 OK 显隐信息",
                        color = Color(0xFF94A3B8),
                        fontSize = 12.sp
                    )
                }
            }
        }
    }
}
