package com.tvfile.app.presentation.component

import android.view.KeyEvent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.tvfile.app.presentation.theme.*

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TVCard(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onLongClick: (() -> Unit)? = null,
    onFocusChanged: ((Boolean) -> Unit)? = null,
    shape: Shape = RoundedCornerShape(12.dp),
    focusedBorderColor: Color = TVAccentCyan,
    unfocusedBorderColor: Color = TVBorder,
    focusedBorderWidth: Dp = 2.5.dp,
    unfocusedBorderWidth: Dp = 1.dp,
    focusedBackgroundColor: Color = TVCardHover,
    unfocusedBackgroundColor: Color = TVCardBg,
    content: @Composable ColumnScope.() -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()

    // 焦点变动直接触发监听，确保 selectedServer 100% 实时同步！
    LaunchedEffect(isFocused) {
        onFocusChanged?.invoke(isFocused)
    }

    val scale by animateFloatAsState(
        targetValue = if (isFocused) 1.05f else 1.0f,
        animationSpec = tween(durationMillis = 200),
        label = "tv_card_scale"
    )

    Surface(
        modifier = modifier
            .scale(scale)
            .onPreviewKeyEvent { event ->
                // 原生遥控器 D-Pad OK 键长按拦截与触发
                if (onLongClick != null && event.nativeKeyEvent.action == KeyEvent.ACTION_DOWN) {
                    val code = event.nativeKeyEvent.keyCode
                    if ((code == KeyEvent.KEYCODE_DPAD_CENTER || code == KeyEvent.KEYCODE_ENTER) && event.nativeKeyEvent.repeatCount >= 1) {
                        onLongClick()
                        true
                    } else {
                        false
                    }
                } else {
                    false
                }
            }
            .combinedClickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
                onLongClick = onLongClick
            ),
        shape = shape,
        color = if (isFocused) focusedBackgroundColor else unfocusedBackgroundColor,
        border = BorderStroke(
            width = if (isFocused) focusedBorderWidth else unfocusedBorderWidth,
            color = if (isFocused) focusedBorderColor else unfocusedBorderColor
        ),
        shadowElevation = if (isFocused) 12.dp else 2.dp
    ) {
        Column(content = content)
    }
}
