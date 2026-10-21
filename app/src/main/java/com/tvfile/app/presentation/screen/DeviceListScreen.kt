package com.tvfile.app.presentation.screen

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.Usb
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
import com.tvfile.app.data.database.ServerEntity
import com.tvfile.app.data.model.StorageDevice
import com.tvfile.app.data.model.StorageType
import com.tvfile.app.presentation.component.TVButton
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
    val isNetwork = currentTab == StorageType.SMB || currentTab == StorageType.WEBDAV
    val emptyFocusRequester = remember { FocusRequester() }

    LaunchedEffect(currentTab, servers.size) {
        if (isNetwork && servers.isEmpty()) {
            kotlinx.coroutines.delay(100)
            try {
                emptyFocusRequester.requestFocus()
            } catch (_: Exception) {}
        }
    }

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
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )

            if (isNetwork) {
                TVButton(
                    onClick = onAddServerClick,
                    focusedBackgroundColor = TVAccentCyan,
                    unfocusedBackgroundColor = TVAccentBlue,
                    focusedBorderColor = Color.White,
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                ) { isFocused ->
                    Icon(imageVector = Icons.Default.Add, contentDescription = null, tint = if (isFocused) Color.Black else Color.White, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = "添加服务器", color = if (isFocused) Color.Black else Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        // 设备列表展示
        if (currentTab == StorageType.USB) {
            if (usbDevices.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(text = "🔌", fontSize = 48.sp)
                        Text(
                            text = "未检测到已插入的 U 盘设备",
                            color = TVTextPrimary,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "请将 U 盘或移动硬盘插入电视 USB 接口，系统将自动识别",
                            color = TVTextMuted,
                            fontSize = 13.sp
                        )
                    }
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 240.dp),
                    horizontalArrangement = Arrangement.spacedBy(18.dp),
                    verticalArrangement = Arrangement.spacedBy(18.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(usbDevices) { dev ->
                        UsbDeviceCard(device = dev, onClick = { onDeviceClick(dev) })
                    }
                }
            }
        } else {
            // 网络存储
            if (servers.isEmpty()) {
                // 空状态时：屏幕中央放置醒目的【添加网络存储服务器】焦点引导大卡片！彻底解决焦点无法移上的问题！
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    TVCard(
                        onClick = onAddServerClick,
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier
                            .width(420.dp)
                            .focusRequester(emptyFocusRequester)
                    ) {
                        Column(
                            modifier = Modifier
                                .padding(28.dp)
                                .fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = TVSurface,
                                modifier = Modifier.size(64.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.Add,
                                        contentDescription = null,
                                        tint = TVAccentCyan,
                                        modifier = Modifier.size(36.dp)
                                    )
                                }
                            }
                            Text(
                                text = "暂无网络存储服务器",
                                color = Color.White,
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "点击按 OK 键添加 SMB (群晖/电脑共享) 或 WebDAV 服务器",
                                color = TVTextSecondary,
                                fontSize = 13.sp,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    }
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 240.dp),
                    horizontalArrangement = Arrangement.spacedBy(18.dp),
                    verticalArrangement = Arrangement.spacedBy(18.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
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
}

@Composable
private fun UsbDeviceCard(
    device: StorageDevice,
    onClick: () -> Unit
) {
    TVCard(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Usb,
                    contentDescription = null,
                    tint = TVAccentGreen,
                    modifier = Modifier.size(32.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(text = device.name, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                    Text(text = device.fsType + " 文件系统", color = TVAccentGreen, fontSize = 12.sp)
                }
            }

            Text(text = device.formattedCapacity, color = TVTextSecondary, fontSize = 12.sp)
            Text(text = "路径: ${device.path}", color = TVTextMuted, fontSize = 11.sp, maxLines = 1)
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
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Dns,
                    contentDescription = null,
                    tint = TVAccentCyan,
                    modifier = Modifier.size(32.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(text = server.name, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                    Text(text = "协议: ${server.storageType.displayName}", color = TVAccentCyan, fontSize = 12.sp)
                }
            }

            Text(text = "地址: ${server.host}:${server.port}", color = TVTextSecondary, fontSize = 12.sp)
            Text(text = "共享路径: ${server.sharePath.ifBlank { "/" }}", color = TVTextMuted, fontSize = 11.sp)
        }
    }
}
