package com.tvfile.app.presentation.screen

import android.view.KeyEvent
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
    currentIndex: Int,
    totalCount: Int,
    onNext: () -> Unit,
    onPrev: () -> Unit,
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

    // 每次切换图片时，信息条显示 3.5 秒后自动淡出
    LaunchedEffect(file.id, isInfoVisible) {
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
                        KeyEvent.KEYCODE_DPAD_LEFT, KeyEvent.KEYCODE_PAGE_UP, KeyEvent.KEYCODE_MEDIA_PREVIOUS -> {
                            isInfoVisible = true
                            onPrev()
                            true
                        }
                        KeyEvent.KEYCODE_DPAD_RIGHT, KeyEvent.KEYCODE_PAGE_DOWN, KeyEvent.KEYCODE_MEDIA_NEXT -> {
                            isInfoVisible = true
                            onNext()
                            true
                        }
                        KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_ENTER,
                        KeyEvent.KEYCODE_DPAD_DOWN, KeyEvent.KEYCODE_DPAD_UP,
                        KeyEvent.KEYCODE_MENU -> {
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

        // 全屏大图展示
        key(file.id) {
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
                        Text("无法加载图片: " + file.name, color = Color.White, fontSize = 16.sp)
                    }
                },
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxSize()
            )
        }

        // 触摸屏支持：点击左 30% 上一张，点击右 30% 下一张，点击中间 40% 显隐信息
        Row(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .weight(0.3f)
                    .fillMaxHeight()
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {
                        isInfoVisible = true
                        onPrev()
                    }
            )
            Box(
                modifier = Modifier
                    .weight(0.4f)
                    .fillMaxHeight()
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {
                        isInfoVisible = !isInfoVisible
                    }
            )
            Box(
                modifier = Modifier
                    .weight(0.3f)
                    .fillMaxHeight()
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {
                        isInfoVisible = true
                        onNext()
                    }
            )
        }

        // 浮动底部信息胶囊 (带当前页码与遥控器翻页提示)
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
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = file.name,
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "" + (currentIndex + 1) + " / " + totalCount,
                        color = TVAccentCyan,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "大小: " + file.formattedSize,
                        color = Color(0xFF8B9BB4),
                        fontSize = 15.5.sp
                    )
                    Text(
                        text = "遥控器 ◀ ▶ 翻页 · 按 BACK 退出",
                        color = Color(0xFF64748B),
                        fontSize = 14.5.sp
                    )
                }
            }
        }
    }
}
