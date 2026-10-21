package com.tvfile.app.presentation.component

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tvfile.app.data.model.TransferTask
import com.tvfile.app.presentation.theme.*

@Composable
fun TransferProgressBar(
    task: TransferTask?,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = task != null,
        enter = slideInVertically { it },
        exit = slideOutVertically { it },
        modifier = modifier
    ) {
        if (task != null) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = TVCardBg,
                border = BorderStroke(1.dp, TVAccentCyan.copy(alpha = 0.5f)),
                shadowElevation = 8.dp,
                modifier = Modifier.padding(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "🚀 正在传输: ${task.sourceFile.name}",
                            color = TVTextPrimary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            text = "${task.progressPercent}%",
                            color = TVAccentCyan,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    LinearProgressIndicator(
                        progress = { task.progressPercent / 100f },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp),
                        color = TVAccentCyan,
                        trackColor = TVBorder
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "速度: ${task.formattedSpeed}",
                            color = TVTextSecondary,
                            fontSize = 11.sp
                        )
                        Text(
                            text = "${task.transferredBytes / (1024*1024)}MB / ${task.totalBytes / (1024*1024)}MB",
                            color = TVTextMuted,
                            fontSize = 11.sp
                        )
                    }
                }
            }
        }
    }
}
