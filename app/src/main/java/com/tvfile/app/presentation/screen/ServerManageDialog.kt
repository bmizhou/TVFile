package com.tvfile.app.presentation.screen

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.tvfile.app.data.database.ServerEntity
import com.tvfile.app.data.model.StorageType
import com.tvfile.app.presentation.component.TVButton
import com.tvfile.app.presentation.theme.*

@Composable
fun ServerManageDialog(
    server: ServerEntity,
    onDismiss: () -> Unit,
    onSave: (ServerEntity) -> Unit,
    onDelete: (ServerEntity) -> Unit
) {
    key(server.id) {
        var name by remember(server.id) { mutableStateOf(server.name) }
        var storageType by remember(server.id) { mutableStateOf(server.storageType) }
        var host by remember(server.id) { mutableStateOf(server.host) }
        var port by remember(server.id) { mutableStateOf(server.port.toString()) }
        var username by remember(server.id) { mutableStateOf(server.username) }
        var password by remember(server.id) { mutableStateOf(server.password) }
        var sharePath by remember(server.id) { mutableStateOf(server.sharePath.ifBlank { "/" }) }

        var showDeleteConfirm by remember(server.id) { mutableStateOf(false) }
        val initialFocusRequester = remember(server.id) { FocusRequester() }

        LaunchedEffect(server.id) {
            kotlinx.coroutines.delay(150)
            try {
                initialFocusRequester.requestFocus()
            } catch (_: Exception) {}
        }

        Dialog(
            onDismissRequest = onDismiss,
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = TVSurface,
                border = BorderStroke(2.dp, TVAccentCyan.copy(alpha = 0.8f)),
                shadowElevation = 24.dp,
                modifier = Modifier
                    .width(560.dp)
                    .wrapContentHeight()
            ) {
                Column(
                    modifier = Modifier
                        .padding(24.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "⚙️ 管理服务器: " + server.name,
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )

                        // 删除服务器按钮
                        TVButton(
                            onClick = { showDeleteConfirm = true },
                            focusedBackgroundColor = TVAccentRed,
                            unfocusedBackgroundColor = Color(0xFF331518),
                            focusedBorderColor = Color.White,
                            unfocusedBorderColor = TVAccentRed.copy(alpha = 0.5f),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                        ) { isFocused ->
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = null,
                                tint = if (isFocused) Color.White else TVAccentRed,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "删除此服务器",
                                color = if (isFocused) Color.White else TVAccentRed,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    // 协议切换
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        TVButton(
                            onClick = { storageType = StorageType.SMB },
                            focusedBackgroundColor = TVAccentBlue,
                            unfocusedBackgroundColor = if (storageType == StorageType.SMB) Color(0xFF1B2F4C) else TVCardBg,
                            focusedBorderColor = TVAccentCyan,
                            unfocusedBorderColor = if (storageType == StorageType.SMB) TVAccentCyan else TVBorder,
                            focusRequester = initialFocusRequester,
                            modifier = Modifier.weight(1f)
                        ) { isFocused ->
                            Text(
                                text = if (storageType == StorageType.SMB) "● SMB 协议" else "SMB 协议",
                                color = if (storageType == StorageType.SMB || isFocused) Color.White else TVTextSecondary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        }

                        TVButton(
                            onClick = { storageType = StorageType.WEBDAV },
                            focusedBackgroundColor = TVAccentBlue,
                            unfocusedBackgroundColor = if (storageType == StorageType.WEBDAV) Color(0xFF1B2F4C) else TVCardBg,
                            focusedBorderColor = TVAccentCyan,
                            unfocusedBorderColor = if (storageType == StorageType.WEBDAV) TVAccentCyan else TVBorder,
                            modifier = Modifier.weight(1f)
                        ) { isFocused ->
                            Text(
                                text = if (storageType == StorageType.WEBDAV) "● WebDAV 协议" else "WebDAV 协议",
                                color = if (storageType == StorageType.WEBDAV || isFocused) Color.White else TVTextSecondary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        }
                    }

                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("服务器备注名称", fontSize = 13.sp) },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = TVAccentCyan,
                            unfocusedBorderColor = TVBorder,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedTextField(
                            value = host,
                            onValueChange = { host = it },
                            label = { Text("IP 地址 / 主机名", fontSize = 13.sp) },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = TVAccentCyan,
                                unfocusedBorderColor = TVBorder,
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            ),
                            modifier = Modifier.weight(2f)
                        )
                        OutlinedTextField(
                            value = port,
                            onValueChange = { port = it },
                            label = { Text("端口", fontSize = 13.sp) },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = TVAccentCyan,
                                unfocusedBorderColor = TVBorder,
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            ),
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedTextField(
                            value = username,
                            onValueChange = { username = it },
                            label = { Text("用户名", fontSize = 13.sp) },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = TVAccentCyan,
                                unfocusedBorderColor = TVBorder,
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            ),
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = password,
                            onValueChange = { password = it },
                            label = { Text("密码", fontSize = 13.sp) },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = TVAccentCyan,
                                unfocusedBorderColor = TVBorder,
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            ),
                            modifier = Modifier.weight(1f)
                        )
                    }

                    OutlinedTextField(
                        value = sharePath,
                        onValueChange = { sharePath = it },
                        label = { Text("共享根路径 (默认为 /)", fontSize = 13.sp) },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = TVAccentCyan,
                            unfocusedBorderColor = TVBorder,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TVButton(
                            onClick = onDismiss,
                            focusedBackgroundColor = Color(0xFF28364F),
                            unfocusedBackgroundColor = TVCardBg,
                            contentPadding = PaddingValues(horizontal = 18.dp, vertical = 10.dp)
                        ) {
                            Text("取消 (BACK)", color = TVTextSecondary, fontSize = 13.sp)
                        }

                        Spacer(modifier = Modifier.width(14.dp))

                        TVButton(
                            onClick = {
                                if (host.isNotBlank()) {
                                    onSave(
                                        server.copy(
                                            name = name.ifBlank { host },
                                            storageType = storageType,
                                            host = host.trim(),
                                            port = port.toIntOrNull() ?: if (storageType == StorageType.SMB) 445 else 5005,
                                            username = username.trim(),
                                            password = password.trim(),
                                            sharePath = if (storageType == StorageType.SMB) sharePath.trim('/', '\\').ifBlank { "video" } else sharePath.trim().ifBlank { "/" }
                                        )
                                    )
                                }
                            },
                            focusedBackgroundColor = TVAccentCyan,
                            unfocusedBackgroundColor = TVAccentBlue,
                            focusedBorderColor = Color.White,
                            contentPadding = PaddingValues(horizontal = 24.dp, vertical = 10.dp)
                        ) { isFocused ->
                            Text("保存更改", color = if (isFocused) Color.Black else Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }
                    }
                }
            }
        }

        if (showDeleteConfirm) {
            AlertDialog(
                onDismissRequest = { showDeleteConfirm = false },
                title = { Text("确认删除该服务器？", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold) },
                text = { Text("即将移除服务器配置：" + server.name + " (" + server.host + ")，不会删除您 NAS 上的真实文件。", color = TVTextSecondary, fontSize = 14.sp) },
                confirmButton = {
                    TVButton(
                        onClick = {
                            showDeleteConfirm = false
                            onDelete(server)
                        },
                        focusedBackgroundColor = TVAccentRed,
                        unfocusedBackgroundColor = TVAccentRed
                    ) {
                        Text("确认删除", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteConfirm = false }) {
                        Text("取消", color = TVTextSecondary)
                    }
                },
                containerColor = TVCardBg
            )
        }
    }
}
