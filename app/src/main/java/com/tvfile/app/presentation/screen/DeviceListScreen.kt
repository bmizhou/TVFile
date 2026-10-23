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
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.input.key.onPreviewKeyEvent
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
    onServerManage: (ServerEntity) -> Unit = {},
    onServerFocused: (ServerEntity) -> Unit = {},
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
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                Text(
                    text = if (currentTab == StorageType.USB) "外接存储设备 (共 ${usbDevices.size} 台)" else "网络存储服务器 (共 ${servers.size} 台)",
                    color = Color.White,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold
                )

                if (isNetwork && servers.isNotEmpty()) {
                    Text(
                        text = "提示: 长按 OK 或按 MENU 键可编辑/重命名/删除服务器",
                        color = TVAccentCyan,
                        fontSize = 19.5.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            if (isNetwork) {
                TVButton(
                    onClick = onAddServerClick,
                    focusedBackgroundColor = TVAccentCyan,
                    unfocusedBackgroundColor = TVAccentBlue,
                    focusedBorderColor = Color.White,
                    contentPadding = PaddingValues(horizontal = 18.dp, vertical = 8.dp)
                ) { isFocused ->
                    Icon(imageVector = Icons.Default.Add, contentDescription = null, tint = if (isFocused) Color.Black else Color.White, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = "添加服务器", color = if (isFocused) Color.Black else Color.White, fontSize = 17.sp, fontWeight = FontWeight.Bold)
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
                        Text(text = "🔌", fontSize = 52.sp)
                        Text(
                            text = "未检测到已插入的 U 盘设备",
                            color = TVTextPrimary,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "请将 U 盘或移动硬盘插入电视 USB 接口，系统将自动识别",
                            color = TVTextMuted,
                            fontSize = 14.sp
                        )
                    }
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 240.dp),
                    contentPadding = PaddingValues(top = 16.dp, bottom = 32.dp, start = 8.dp, end = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(18.dp),
                    verticalArrangement = Arrangement.spacedBy(20.dp),
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
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    TVCard(
                        onClick = onAddServerClick,
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier
                            .width(440.dp)
                            .focusRequester(emptyFocusRequester)
                    ) {
                        Column(
                            modifier = Modifier
                                .padding(30.dp)
                                .fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = TVSurface,
                                modifier = Modifier.size(68.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.Add,
                                        contentDescription = null,
                                        tint = TVAccentCyan,
                                        modifier = Modifier.size(38.dp)
                                    )
                                }
                            }
                            Text(
                                text = "暂无网络存储服务器",
                                color = Color.White,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "按遥控器 OK 键添加 SMB (群晖/电脑共享) 或 WebDAV 服务器",
                                color = TVTextSecondary,
                                fontSize = 17.sp,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    }
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 250.dp),
                    contentPadding = PaddingValues(top = 16.dp, bottom = 32.dp, start = 8.dp, end = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(18.dp),
                    verticalArrangement = Arrangement.spacedBy(20.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(servers, key = { it.id }) { srv ->
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
                        NetworkServerCard(
                            server = srv,
                            onClick = { onDeviceClick(dev) },
                            onLongClick = { onServerManage(srv) },
                            onFocused = { onServerFocused(srv) }
                        )
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
                    modifier = Modifier.size(34.dp)
                )
                Spacer(modifier = Modifier.width(14.dp))
                Column {
                    Text(text = device.name, color = Color.White, fontSize = 19.5.sp, fontWeight = FontWeight.Bold)
                    Text(text = device.fsType + " 文件系统", color = TVAccentGreen, fontSize = 16.sp)
                }
            }

            Text(text = device.formattedCapacity, color = TVTextSecondary, fontSize = 16.sp)
            Text(text = "路径: ${device.path}", color = TVTextMuted, fontSize = 11.5.sp, maxLines = 1)
        }
    }
}

@Composable
private fun NetworkServerCard(
    server: ServerEntity,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onFocused: () -> Unit
) {
    TVCard(
        onClick = onClick,
        onLongClick = onLongClick,
        onFocusChanged = { if (it) onFocused() },
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .onPreviewKeyEvent { event ->
                if (event.nativeKeyEvent.action == android.view.KeyEvent.ACTION_DOWN &&
                    event.nativeKeyEvent.keyCode == android.view.KeyEvent.KEYCODE_MENU
                ) {
                    onLongClick()
                    true
                } else {
                    false
                }
            }
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Icon(
                        imageVector = Icons.Default.Dns,
                        contentDescription = null,
                        tint = TVAccentCyan,
                        modifier = Modifier.size(34.dp)
                    )
                    Spacer(modifier = Modifier.width(14.dp))
                    Column {
                        Text(
                            text = server.name,
                            color = Color.White,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                        )
                        Text(text = "协议: ${server.storageType.displayName}", color = TVAccentCyan, fontSize = 16.sp)
                    }
                }

                // 独立管理按钮：遥控器可直接选按，修改名称或删除服务器
                TVButton(
                    onClick = onLongClick,
                    focusedBackgroundColor = TVAccentCyan,
                    unfocusedBackgroundColor = Color(0xFF162338),
                    focusedBorderColor = Color.White,
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                ) { isFocused ->
                    if (isFocused) {
                        LaunchedEffect(Unit) {
                            onFocused()
                        }
                    }
                    Text(
                        text = "⚙️ 管理",
                        color = if (isFocused) Color.Black else TVTextSecondary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Text(text = "地址: ${server.host}:${server.port}", color = TVTextSecondary, fontSize = 16.sp)
            Text(text = "共享路径: ${server.sharePath.ifBlank { "/" }}", color = TVTextMuted, fontSize = 12.sp)
        }
    }
}
