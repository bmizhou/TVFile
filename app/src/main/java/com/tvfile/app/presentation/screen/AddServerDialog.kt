package com.tvfile.app.presentation.screen

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.tvfile.app.data.database.ServerEntity
import com.tvfile.app.data.model.StorageType
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
    var sharePath by remember { mutableStateOf("video") }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = TVSurface,
            border = BorderStroke(1.dp, TVAccentCyan.copy(alpha = 0.5f)),
            modifier = Modifier.width(420.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "添加网络存储服务器",
                    color = TVTextPrimary,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button(
                        onClick = { storageType = StorageType.SMB; port = "445" },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (storageType == StorageType.SMB) TVAccentBlue else TVCardBg
                        )
                    ) {
                        Text(text = "SMB 协议")
                    }
                    Button(
                        onClick = { storageType = StorageType.WEBDAV; port = "5005" },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (storageType == StorageType.WEBDAV) TVAccentBlue else TVCardBg
                        )
                    ) {
                        Text(text = "WebDAV 协议")
                    }
                }

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("服务器备注名称") },
                    modifier = Modifier.fillMaxWidth()
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = host,
                        onValueChange = { host = it },
                        label = { Text("IP 地址 / 主机名") },
                        modifier = Modifier.weight(2f)
                    )
                    OutlinedTextField(
                        value = port,
                        onValueChange = { port = it },
                        label = { Text("端口") },
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = username,
                        onValueChange = { username = it },
                        label = { Text("用户名(可空)") },
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text("密码(可空)") },
                        modifier = Modifier.weight(1f)
                    )
                }

                OutlinedTextField(
                    value = sharePath,
                    onValueChange = { sharePath = it },
                    label = { Text("共享名称 / 路径 (如 video)") },
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("取消", color = TVTextSecondary)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (host.isNotBlank()) {
                                onConfirm(
                                    ServerEntity(
                                        name = name.ifBlank { host },
                                        storageType = storageType,
                                        host = host,
                                        port = port.toIntOrNull() ?: if (storageType == StorageType.SMB) 445 else 5005,
                                        username = username,
                                        password = password,
                                        sharePath = sharePath
                                    )
                                )
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = TVAccentCyan)
                    ) {
                        Text("保存连接", color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
