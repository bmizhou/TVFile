package com.tvfile.app.presentation.screen

import android.content.res.Configuration
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ViewList
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tvfile.app.data.model.StorageType
import com.tvfile.app.data.model.ViewMode
import com.tvfile.app.presentation.component.*
import com.tvfile.app.presentation.theme.*
import com.tvfile.app.presentation.viewmodel.MainViewModel

@Composable
fun MainScreen(
    viewModel: MainViewModel = viewModel()
) {
    val configuration = LocalConfiguration.current
    val isPortrait = configuration.orientation == Configuration.ORIENTATION_PORTRAIT

    val currentTab by viewModel.currentTab.collectAsState()
    val level by viewModel.level.collectAsState()
    val currentDevice by viewModel.currentDevice.collectAsState()
    val currentPath by viewModel.currentPath.collectAsState()
    val filterCategory by viewModel.filterCategory.collectAsState()
    val viewMode by viewModel.viewMode.collectAsState()
    val files by viewModel.fileList.collectAsState()
    val usbDevices by viewModel.usbDevices.collectAsState()
    val servers by viewModel.servers.collectAsState()
    val playbackProgressMap by viewModel.playbackProgressMap.collectAsState()

    val activeVideoUrl by viewModel.activeVideoUrl.collectAsState()
    val activeAudioUrl by viewModel.activeAudioUrl.collectAsState()
    val activeImageFile by viewModel.activeImageFile.collectAsState()
    val isMenuOpen by viewModel.isMenuOpen.collectAsState()
    val selectedFile by viewModel.selectedFile.collectAsState()
    val isAddServerDialogOpen by viewModel.isAddServerDialogOpen.collectAsState()
    val clipboard by viewModel.fileOperationManager.clipboard.collectAsState()
    val activeTask by viewModel.fileOperationManager.activeTask.collectAsState()

    // 遥控器返回键处理
    BackHandler {
        val handled = viewModel.navigateBack()
        if (!handled) {
            // 已在最外层，退出应用由系统处理
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(TVBackground)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // 1. 顶部状态栏
            TopStatusBar(
                activeTitle = currentDevice?.name ?: currentTab.displayName
            )

            if (isPortrait) {
                // 【竖屏模式】：左侧 Tab 栏移动到顶部，只保留图标+图标文字，文字在图标下方
                PortraitTopTabBar(
                    selectedTab = currentTab,
                    onTabSelected = { tab -> viewModel.selectTab(tab) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                )

                // 竖屏下方主工作区
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    MainContentSection(
                        currentTab = currentTab,
                        level = level,
                        currentDeviceName = currentDevice?.name ?: currentTab.displayName,
                        currentPath = currentPath,
                        viewMode = viewMode,
                        filterCategory = filterCategory,
                        files = files,
                        usbDevices = usbDevices,
                        servers = servers,
                        playbackProgressMap = playbackProgressMap,
                        canPaste = clipboard != null && clipboard!!.files.isNotEmpty(),
                        onRootClick = { viewModel.selectTab(currentTab) },
                        onToggleView = { viewModel.toggleViewMode() },
                        onPaste = { viewModel.pasteToCurrent() },
                        onCategorySelected = { cat -> viewModel.setFilterCategory(cat) },
                        onDeviceClick = { dev -> viewModel.enterDevice(dev) },
                        onAddServerClick = { viewModel.setAddServerDialogOpen(true) },
                        onFileClick = { file -> viewModel.openFile(file) },
                        onFileLongClick = { file -> viewModel.openContextMenu(file) }
                    )
                }
            } else {
                // 【横屏模式】：维持原左侧 Tab 栏 + 右侧内容区双栏布局
                Row(modifier = Modifier.fillMaxSize()) {
                    Sidebar(
                        selectedTab = currentTab,
                        onTabSelected = { tab -> viewModel.selectTab(tab) },
                        modifier = Modifier
                            .width(220.dp)
                            .fillMaxHeight()
                    )

                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                    ) {
                        MainContentSection(
                            currentTab = currentTab,
                            level = level,
                            currentDeviceName = currentDevice?.name ?: currentTab.displayName,
                            currentPath = currentPath,
                            viewMode = viewMode,
                            filterCategory = filterCategory,
                            files = files,
                            usbDevices = usbDevices,
                            servers = servers,
                            playbackProgressMap = playbackProgressMap,
                            canPaste = clipboard != null && clipboard!!.files.isNotEmpty(),
                            onRootClick = { viewModel.selectTab(currentTab) },
                            onToggleView = { viewModel.toggleViewMode() },
                            onPaste = { viewModel.pasteToCurrent() },
                            onCategorySelected = { cat -> viewModel.setFilterCategory(cat) },
                            onDeviceClick = { dev -> viewModel.enterDevice(dev) },
                            onAddServerClick = { viewModel.setAddServerDialogOpen(true) },
                            onFileClick = { file -> viewModel.openFile(file) },
                            onFileLongClick = { file -> viewModel.openContextMenu(file) }
                        )
                    }
                }
            }
        }

        // 3. 悬浮浮窗与弹窗
        if (isMenuOpen) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.5f)),
                contentAlignment = Alignment.CenterEnd
            ) {
                ContextMenuDrawer(
                    targetFile = selectedFile,
                    canPaste = clipboard != null && clipboard!!.files.isNotEmpty(),
                    onCopy = { viewModel.copySelected() },
                    onCut = { viewModel.cutSelected() },
                    onPaste = { viewModel.pasteToCurrent() },
                    onDelete = { viewModel.deleteSelected() },
                    onRename = { viewModel.closeContextMenu() },
                    onDetails = { viewModel.closeContextMenu() },
                    onDismiss = { viewModel.closeContextMenu() }
                )
            }
        }

        // 后台传输进度浮层
        TransferProgressBar(
            task = activeTask,
            modifier = Modifier.align(Alignment.BottomEnd)
        )

        // 视频播放全屏覆盖层 (支持断点记忆续播)
        if (activeVideoUrl != null) {
            VideoPlayerOverlay(
                file = activeVideoUrl!!.first,
                playUri = activeVideoUrl!!.second,
                initialPosition = activeVideoUrl!!.third,
                onSaveProgress = { pos, dur ->
                    viewModel.saveVideoProgress(activeVideoUrl!!.first.id, pos, dur)
                },
                onClose = { viewModel.closePlayers() }
            )
        }

        // 音乐播放全屏覆盖层
        if (activeAudioUrl != null) {
            AudioPlayerOverlay(
                file = activeAudioUrl!!.first,
                playUri = activeAudioUrl!!.second,
                onClose = { viewModel.closePlayers() }
            )
        }

        // 图片查看全屏覆盖层
        if (activeImageFile != null) {
            ImageViewerOverlay(
                file = activeImageFile!!,
                onClose = { viewModel.closePlayers() }
            )
        }

        // 添加服务器弹窗
        if (isAddServerDialogOpen) {
            AddServerDialog(
                onDismiss = { viewModel.setAddServerDialogOpen(false) },
                onConfirm = { srv -> viewModel.addServer(srv) }
            )
        }
    }
}

/**
 * 竖屏模式专属：顶部 Tab 栏
 * 只保留图标 + 图标文字，文字在图标正下方
 */
@Composable
private fun PortraitTopTabBar(
    selectedTab: StorageType,
    onTabSelected: (StorageType) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        PortraitTabItem(
            title = "内部存储",
            icon = Icons.Default.SdCard,
            isSelected = selectedTab == StorageType.INTERNAL,
            onClick = { onTabSelected(StorageType.INTERNAL) },
            modifier = Modifier.weight(1f)
        )
        PortraitTabItem(
            title = "外接存储",
            icon = Icons.Default.Usb,
            isSelected = selectedTab == StorageType.USB,
            onClick = { onTabSelected(StorageType.USB) },
            modifier = Modifier.weight(1f)
        )
        PortraitTabItem(
            title = "网络存储",
            icon = Icons.Default.Dns,
            isSelected = selectedTab == StorageType.SMB || selectedTab == StorageType.WEBDAV,
            onClick = { onTabSelected(StorageType.SMB) },
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun PortraitTabItem(
    title: String,
    icon: ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    TVCard(
        onClick = onClick,
        focusedBorderColor = TVAccentCyan,
        unfocusedBorderColor = if (isSelected) TVAccentCyan.copy(alpha = 0.6f) else Color.Transparent,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .padding(vertical = 8.dp, horizontal = 4.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isSelected) TVAccentCyan else TVTextSecondary,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = title,
                color = if (isSelected) Color.White else TVTextPrimary,
                fontSize = 11.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun MainContentSection(
    currentTab: StorageType,
    level: Int,
    currentDeviceName: String,
    currentPath: String,
    viewMode: ViewMode,
    filterCategory: com.tvfile.app.data.model.FileCategory,
    files: List<com.tvfile.app.data.model.TVFile>,
    usbDevices: List<com.tvfile.app.data.model.StorageDevice>,
    servers: List<com.tvfile.app.data.database.ServerEntity>,
    playbackProgressMap: Map<String, Float>,
    canPaste: Boolean,
    onRootClick: () -> Unit,
    onToggleView: () -> Unit,
    onPaste: () -> Unit,
    onCategorySelected: (com.tvfile.app.data.model.FileCategory) -> Unit,
    onDeviceClick: (com.tvfile.app.data.model.StorageDevice) -> Unit,
    onAddServerClick: () -> Unit,
    onFileClick: (com.tvfile.app.data.model.TVFile) -> Unit,
    onFileLongClick: (com.tvfile.app.data.model.TVFile) -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        // 顶部控制条 (面包屑 + 视图切换 + 粘贴)
        HeaderToolbar(
            rootName = currentDeviceName,
            currentPath = currentPath,
            viewMode = viewMode,
            canPaste = canPaste,
            onRootClick = onRootClick,
            onToggleView = onToggleView,
            onPaste = onPaste
        )

        // 【严格对标需求10】：仅在第2层(具体目录层)时展示文件类型过滤 Chip
        if (level == 2) {
            FilterBar(
                selectedCategory = filterCategory,
                onCategorySelected = onCategorySelected,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp)
            )
        }

        // 主视口切换
        Box(modifier = Modifier.weight(1f)) {
            if (level == 1) {
                // 第1层：设备/服务器列表 (无过滤)
                DeviceListScreen(
                    currentTab = currentTab,
                    usbDevices = usbDevices,
                    servers = servers,
                    onDeviceClick = onDeviceClick,
                    onAddServerClick = onAddServerClick
                )
            } else {
                // 第2层：具体目录文件浏览 (带过滤 + 网格/列表)
                FileBrowserScreen(
                    files = files,
                    viewMode = viewMode,
                    progressMap = playbackProgressMap,
                    onFileClick = onFileClick,
                    onFileLongClick = onFileLongClick
                )
            }
        }
    }
}

@Composable
private fun TopStatusBar(activeTitle: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(44.dp)
            .background(TVSurface)
            .padding(horizontal = 20.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .background(TVAccentCyan, shape = MaterialTheme.shapes.extraSmall)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "TVFile",
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.width(8.dp))
            Surface(
                shape = MaterialTheme.shapes.small,
                color = TVAccentBlue.copy(alpha = 0.2f),
                border = BorderStroke(1.dp, TVAccentCyan.copy(alpha = 0.3f))
            ) {
                Text(
                    text = "TV Edition",
                    color = TVAccentCyan,
                    fontSize = 10.sp,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(text = "当前: $activeTitle", color = TVTextSecondary, fontSize = 12.sp)
            Text(text = "📶 局域网在线", color = TVTextMuted, fontSize = 12.sp)
        }
    }
}

@Composable
private fun Sidebar(
    selectedTab: StorageType,
    onTabSelected: (StorageType) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .background(Color(0xFF0D121C))
            .padding(16.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            SidebarTabItem(
                title = "内部存储",
                subtitle = "本地闪存",
                icon = Icons.Default.SdCard,
                isSelected = selectedTab == StorageType.INTERNAL,
                onClick = { onTabSelected(StorageType.INTERNAL) }
            )
            SidebarTabItem(
                title = "外接存储",
                subtitle = "U盘 / 移动硬盘",
                icon = Icons.Default.Usb,
                isSelected = selectedTab == StorageType.USB,
                onClick = { onTabSelected(StorageType.USB) }
            )
            SidebarTabItem(
                title = "网络存储",
                subtitle = "SMB / WebDAV",
                icon = Icons.Default.Dns,
                isSelected = selectedTab == StorageType.SMB || selectedTab == StorageType.WEBDAV,
                onClick = { onTabSelected(StorageType.SMB) }
            )
        }

        Surface(
            shape = MaterialTheme.shapes.small,
            color = TVCardBg,
            border = BorderStroke(1.dp, TVBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(10.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(text = "遥控器快捷提示", color = TVTextPrimary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                Text(text = "OK: 打开 / 进入", color = TVTextSecondary, fontSize = 10.sp)
                Text(text = "MENU: 文件操作抽屉", color = TVTextSecondary, fontSize = 10.sp)
                Text(text = "BACK: 返回上一级", color = TVTextSecondary, fontSize = 10.sp)
            }
        }
    }
}

@Composable
private fun SidebarTabItem(
    title: String,
    subtitle: String,
    icon: ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    TVCard(
        onClick = onClick,
        focusedBorderColor = TVAccentCyan,
        unfocusedBorderColor = if (isSelected) TVAccentCyan.copy(alpha = 0.5f) else Color.Transparent,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isSelected) TVAccentCyan else TVTextSecondary,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = title,
                    color = if (isSelected) Color.White else TVTextPrimary,
                    fontSize = 13.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                )
                Text(text = subtitle, color = TVTextMuted, fontSize = 10.sp)
            }
        }
    }
}

@Composable
private fun HeaderToolbar(
    rootName: String,
    currentPath: String,
    viewMode: ViewMode,
    canPaste: Boolean,
    onRootClick: () -> Unit,
    onToggleView: () -> Unit,
    onPaste: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        BreadcrumbBar(
            rootName = rootName,
            currentPath = currentPath,
            onRootClick = onRootClick,
            modifier = Modifier.weight(1f)
        )

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                onClick = onToggleView,
                colors = ButtonDefaults.buttonColors(containerColor = TVCardBg)
            ) {
                Icon(
                    imageVector = if (viewMode == ViewMode.GRID) Icons.AutoMirrored.Filled.ViewList else Icons.Default.GridView,
                    contentDescription = null,
                    tint = TVAccentCyan,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (viewMode == ViewMode.GRID) "列表" else "网格",
                    color = TVTextPrimary,
                    fontSize = 11.sp
                )
            }

            if (canPaste) {
                Button(
                    onClick = onPaste,
                    colors = ButtonDefaults.buttonColors(containerColor = TVAccentGreen)
                ) {
                    Icon(imageVector = Icons.Default.ContentPaste, contentDescription = null, tint = Color.Black)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = "粘贴", color = Color.Black, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
