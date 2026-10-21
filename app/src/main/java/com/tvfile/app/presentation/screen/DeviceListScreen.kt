package com.tvfile.app.presentation.screen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.Usb
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tvfile.app.data.database.ServerEntity
import com.tvfile.app.data.model.StorageDevice
import com.tvfile.app.data.model.StorageType
import com.tvfile.app.presentation.component.TVCard
import com.tvfile.app.presentation.theme.*

@Composable
fun DeviceListScreen(
    currentTab: StorageType,
    usbDevices: List<StorageDevice>,
    servers: List<ServerEntity>,
    onDeviceClick: (StorageDevice) -> Unit,
    onAddServerClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (currentTab == StorageType.USB) "外接存储设备 (共 ${usbDevices.size} 台)" else "网络存储服务器 (共 ${servers.size} 台)",
                color = TVTextPrimary,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )

            if (currentTab == StorageType.SMB || currentTab == StorageType.WEBDAV) {
                Button(
                    onClick = onAddServerClick,
                    colors = ButtonDefaults.buttonColors(containerColor = TVAccentBlue)
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = null, tint = Color.White)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = "添加服务器", color = Color.White, fontSize = 12.sp)
                }
            }
        }

        // 设备卡片网格
        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 220.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            if (currentTab == StorageType.USB) {
                if (usbDevices.isEmpty()) {
                    item {
                        Text(
                            text = "未检测到已挂载的 U 盘设备，请将 U 盘插入电视 USB 接口...",
                            color = TVTextMuted,
                            fontSize = 13.sp,
                            modifier = Modifier.padding(top = 20.dp)
                        )
                    }
                } else {
                    items(usbDevices) { dev ->
                        UsbDeviceCard(device = dev, onClick = { onDeviceClick(dev) })
                    }
                }
            } else {
                items(servers) { srv ->
                    val dev = StorageDevice(
                        id = "srv_${srv.id}",
                        name = srv.name,
                        path = srv.sharePath,
                        storageType = srv.storageType,
                        host = srv.host,
                        port = srv.port,
                        username = srv.username,
                        password = srv.password,
                        sharePath = srv.sharePath,
                        serverId = srv.id
                    )
                    NetworkServerCard(server = srv, onClick = { onDeviceClick(dev) })
                }
            }
        }
    }
}

@Composable
private fun UsbDeviceCard(
    device: StorageDevice,
    onClick: () -> Unit
) {
    TVCard(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Usb,
                    contentDescription = null,
                    tint = TVAccentGreen,
                    modifier = Modifier.size(28.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(text = device.name, color = TVTextPrimary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                    Text(text = device.fsType, color = TVAccentGreen, fontSize = 11.sp)
                }
            }

            Text(text = device.formattedCapacity, color = TVTextSecondary, fontSize = 11.sp)
            Text(text = "路径: ${device.path}", color = TVTextMuted, fontSize = 10.sp, maxLines = 1)
        }
    }
}

@Composable
private fun NetworkServerCard(
    server: ServerEntity,
    onClick: () -> Unit
) {
    TVCard(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Dns,
                    contentDescription = null,
                    tint = TVAccentCyan,
                    modifier = Modifier.size(28.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(text = server.name, color = TVTextPrimary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                    Text(text = "协议: ${server.storageType.displayName}", color = TVAccentCyan, fontSize = 11.sp)
                }
            }

            Text(text = "地址: ${server.host}:${server.port}", color = TVTextSecondary, fontSize = 11.sp)
            Text(text = "共享目录: ${server.sharePath.ifBlank { "根目录" }}", color = TVTextMuted, fontSize = 10.sp)
        }
    }
}
