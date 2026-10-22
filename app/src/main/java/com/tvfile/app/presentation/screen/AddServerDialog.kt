package com.tvfile.app.presentation.screen

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
fun AddServerDialog(
    onDismiss: () -> Unit,
    onConfirm: (ServerEntity) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var storageType by remember { mutableStateOf(StorageType.SMB) }
    var host by remember { mutableStateOf("192.168.1.100") }
    var port by remember { mutableStateOf("445") }
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    // 默认根路径为 /
    var sharePath by remember { mutableStateOf("/") }

    val initialFocusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(200)
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
                Text(
                    text = "添加网络存储服务器",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )

                // 协议切换 (带遥控器清晰高亮焦点)
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    TVButton(
                        onClick = { 
                            storageType = StorageType.SMB
                            port = "445"
                            
                        },
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
                        onClick = { 
                            storageType = StorageType.WEBDAV
                            port = "5005"
                            
                        },
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
                    label = { Text("服务器备注名称 (可空)", fontSize = 13.sp) },
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
                        label = { Text("用户名 (可空)", fontSize = 13.sp) },
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
                        label = { Text("密码 (可空)", fontSize = 13.sp) },
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
                                onConfirm(
                                    ServerEntity(
                                        name = name.ifBlank { host },
                                        storageType = storageType,
                                        host = host.trim(),
                                        port = port.toIntOrNull() ?: if (storageType == StorageType.SMB) 445 else 5005,
                                        username = username.trim(),
                                        password = password.trim(),
                                        sharePath = sharePath.trim().ifBlank { "/" }
                                    )
                                )
                            }
                        },
                        focusedBackgroundColor = TVAccentCyan,
                        unfocusedBackgroundColor = TVAccentBlue,
                        focusedBorderColor = Color.White,
                        contentPadding = PaddingValues(horizontal = 24.dp, vertical = 10.dp)
                    ) { isFocused ->
                        Text("保存并连接", color = if (isFocused) Color.Black else Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                }
            }
        }
    }
}
