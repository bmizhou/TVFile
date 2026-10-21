package com.tvfile.app.presentation.screen

import android.app.Activity
import android.content.res.Configuration
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tvfile.app.data.model.FileCategory
import com.tvfile.app.data.model.SortMode
import com.tvfile.app.data.model.StorageType
import com.tvfile.app.data.model.ViewMode
import com.tvfile.app.presentation.component.*
import com.tvfile.app.presentation.theme.*
import com.tvfile.app.presentation.viewmodel.MainViewModel

@Composable
fun MainScreen(
    viewModel: MainViewModel = viewModel()
) {
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    val isPortrait = configuration.orientation == Configuration.ORIENTATION_PORTRAIT

    val currentTab by viewModel.currentTab.collectAsState()
    val level by viewModel.level.collectAsState()
    val currentDevice by viewModel.currentDevice.collectAsState()
    val currentPath by viewModel.currentPath.collectAsState()
    val filterCategory by viewModel.filterCategory.collectAsState()
    val sortMode by viewModel.sortMode.collectAsState()
    val viewMode by viewModel.viewMode.collectAsState()
    val files by viewModel.fileList.collectAsState()
    val usbDevices by viewModel.usbDevices.collectAsState()
    val servers by viewModel.servers.collectAsState()
    val playbackProgressMap by viewModel.playbackProgressMap.collectAsState()

    // Web 传输状态
    val isHttpServerRunning by viewModel.isHttpServerRunning.collectAsState()
    val webServerUrl by viewModel.webServerUrl.collectAsState()
    val receivedFiles by viewModel.receivedFiles.collectAsState()
    val filteredReceivedFiles by viewModel.filteredReceivedFiles.collectAsState()
    val webFilterCategory by viewModel.webFilterCategory.collectAsState()

    // 播放与弹窗状态
    val activeVideoUrl by viewModel.activeVideoUrl.collectAsState()
    val activeAudioUrl by viewModel.activeAudioUrl.collectAsState()
    val activeImageFile by viewModel.activeImageFile.collectAsState()
    val apkDownloadProgress by viewModel.apkDownloadProgress.collectAsState()
    val downloadingApkFile by viewModel.downloadingApkFile.collectAsState()
    val isMenuOpen by viewModel.isMenuOpen.collectAsState()
    val selectedFile by viewModel.selectedFile.collectAsState()
    val isAddServerDialogOpen by viewModel.isAddServerDialogOpen.collectAsState()
    val clipboard by viewModel.fileOperationManager.clipboard.collectAsState()
    val activeTask by viewModel.fileOperationManager.activeTask.collectAsState()

    // 排序选择弹窗控制
    var isSortDialogOpen by remember { mutableStateOf(false) }

    // 【解决问题 9】：主页按返回键防误触双击退出
    var lastBackPressTime by remember { mutableLongStateOf(0L) }
    BackHandler {
        val handled = viewModel.navigateBack()
        if (!handled) {
            val now = System.currentTimeMillis()
            if (now - lastBackPressTime < 2000L) {
                (context as? Activity)?.finish()
            } else {
                lastBackPressTime = now
                Toast.makeText(context, "再按一次返回键退出 TVFile", Toast.LENGTH_SHORT).show()
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(TVBackground)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // 顶部状态栏 (无 TV Edition 标签)
            TopStatusBar(
                activeTitle = currentDevice?.name ?: currentTab.displayName
            )

            if (isPortrait) {
                // 竖屏顶部 Tab 栏
                PortraitTopTabBar(
                    selectedTab = currentTab,
                    onTabSelected = { tab -> viewModel.selectTab(tab) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                )

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    if (currentTab == StorageType.WEB_TRANSFER) {
                        WebTransferScreen(
                            isServerRunning = isHttpServerRunning,
                            serverUrl = webServerUrl,
                            files = filteredReceivedFiles,
                            totalFilesCount = receivedFiles.size,
                            selectedCategory = webFilterCategory,
                            onToggleServer = { viewModel.toggleHttpServer() },
                            onCategorySelected = { cat -> viewModel.setWebFilterCategory(cat) },
                            onRefreshList = { viewModel.refreshReceivedFiles() },
                            onClearAll = { viewModel.clearAllReceivedFiles() },
                            onFileClick = { file -> viewModel.openFile(file) },
                            onDeleteFile = { file -> viewModel.deleteReceivedFile(file) }
                        )
                    } else {
                        MainContentSection(
                            currentTab = currentTab,
                            level = level,
                            currentDeviceName = currentDevice?.name ?: currentTab.displayName,
                            currentPath = currentPath,
                            viewMode = viewMode,
                            filterCategory = filterCategory,
                            sortMode = sortMode,
                            files = files,
                            usbDevices = usbDevices,
                            servers = servers,
                            playbackProgressMap = playbackProgressMap,
                            canPaste = clipboard != null && clipboard!!.files.isNotEmpty(),
                            onRootClick = { viewModel.selectTab(currentTab) },
                            onToggleView = { viewModel.toggleViewMode() },
                            onOpenSortDialog = { isSortDialogOpen = true },
                            onPaste = { viewModel.pasteToCurrent() },
                            onCategorySelected = { cat -> viewModel.setFilterCategory(cat) },
                            onDeviceClick = { dev -> viewModel.enterDevice(dev) },
                            onAddServerClick = { viewModel.setAddServerDialogOpen(true) },
                            onFileClick = { file -> viewModel.openFile(file) },
                            onFileLongClick = { file -> viewModel.openContextMenu(file) }
                        )
                    }
                }
            } else {
                // 横屏经典双栏布局
                Row(modifier = Modifier.fillMaxSize()) {
                    Sidebar(
                        selectedTab = currentTab,
                        onTabSelected = { tab -> viewModel.selectTab(tab) },
                        modifier = Modifier
                            .width(240.dp)
                            .fillMaxHeight()
                    )

                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                    ) {
                        if (currentTab == StorageType.WEB_TRANSFER) {
                            WebTransferScreen(
                                isServerRunning = isHttpServerRunning,
                                serverUrl = webServerUrl,
                                files = filteredReceivedFiles,
                                totalFilesCount = receivedFiles.size,
                                selectedCategory = webFilterCategory,
                                onToggleServer = { viewModel.toggleHttpServer() },
                                onCategorySelected = { cat -> viewModel.setWebFilterCategory(cat) },
                                onRefreshList = { viewModel.refreshReceivedFiles() },
                                onClearAll = { viewModel.clearAllReceivedFiles() },
                                onFileClick = { file -> viewModel.openFile(file) },
                                onDeleteFile = { file -> viewModel.deleteReceivedFile(file) }
                            )
                        } else {
                            MainContentSection(
                                currentTab = currentTab,
                                level = level,
                                currentDeviceName = currentDevice?.name ?: currentTab.displayName,
                                currentPath = currentPath,
                                viewMode = viewMode,
                                filterCategory = filterCategory,
                                sortMode = sortMode,
                                files = files,
                                usbDevices = usbDevices,
                                servers = servers,
                                playbackProgressMap = playbackProgressMap,
                                canPaste = clipboard != null && clipboard!!.files.isNotEmpty(),
                                onRootClick = { viewModel.selectTab(currentTab) },
                                onToggleView = { viewModel.toggleViewMode() },
                                onOpenSortDialog = { isSortDialogOpen = true },
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
        }

        // 操作抽屉 (解决问题 7: 自动聚焦首选项)
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

        // 排序选择弹窗 (解决问题 8)
        if (isSortDialogOpen) {
            SortSelectionDialog(
                currentSortMode = sortMode,
                onSelectSort = { mode ->
                    viewModel.setSortMode(mode)
                    isSortDialogOpen = false
                },
                onDismiss = { isSortDialogOpen = false }
            )
        }

        // 【解决问题 10】：远程 APK 下载进度弹窗
        if (apkDownloadProgress != null && downloadingApkFile != null) {
            ApkDownloadDialog(
                file = downloadingApkFile!!,
                progress = apkDownloadProgress!!,
                onCancel = { viewModel.cancelApkDownload() }
            )
        }

        // 后台传输进度浮层
        TransferProgressBar(
            task = activeTask,
            modifier = Modifier.align(Alignment.BottomEnd)
        )

        // 视频播放全屏覆盖层
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

        // 添加服务器弹窗 (解决问题 4: 默认路径 /, 全遥控器焦点适配)
        if (isAddServerDialogOpen) {
            AddServerDialog(
                onDismiss = { viewModel.setAddServerDialogOpen(false) },
                onConfirm = { srv -> viewModel.addServer(srv) }
            )
        }
    }
}

/**
 * 排序选择弹窗组件
 */
@Composable
private fun SortSelectionDialog(
    currentSortMode: SortMode,
    onSelectSort: (SortMode) -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = TVSurface,
            border = BorderStroke(2.dp, TVAccentCyan.copy(alpha = 0.8f)),
            shadowElevation = 24.dp,
            modifier = Modifier.width(380.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "🔀 选择文件排序方式",
                    color = Color.White,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 6.dp)
                )

                SortMode.values().forEach { mode ->
                    val isSelected = currentSortMode == mode
                    TVButton(
                        onClick = { onSelectSort(mode) },
                        focusedBackgroundColor = TVAccentBlue,
                        unfocusedBackgroundColor = if (isSelected) Color(0xFF132845) else Color.Transparent,
                        focusedBorderColor = TVAccentCyan,
                        unfocusedBorderColor = if (isSelected) TVAccentCyan.copy(alpha = 0.6f) else Color.Transparent,
                        modifier = Modifier.fillMaxWidth(),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp)
                    ) { isFocused ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = mode.displayName,
                                color = if (isSelected || isFocused) Color.White else TVTextSecondary,
                                fontSize = 14.sp,
                                fontWeight = if (isSelected || isFocused) FontWeight.Bold else FontWeight.Medium
                            )
                            if (isSelected) {
                                Text("✔", color = TVAccentCyan, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))
                TVButton(
                    onClick = onDismiss,
                    focusedBackgroundColor = Color(0xFF28364F),
                    unfocusedBackgroundColor = TVCardBg,
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(vertical = 8.dp)
                ) {
                    Text("取消 (BACK)", color = TVTextSecondary, fontSize = 13.sp)
                }
            }
        }
    }
}

/**
 * 竖屏模式专属：顶部 Tab 栏
 */
@Composable
private fun PortraitTopTabBar(
    selectedTab: StorageType,
    onTabSelected: (StorageType) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
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
        PortraitTabItem(
            title = "Web 传输",
            icon = Icons.Default.CloudSync,
            isSelected = selectedTab == StorageType.WEB_TRANSFER,
            onClick = { onTabSelected(StorageType.WEB_TRANSFER) },
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
                .padding(vertical = 10.dp, horizontal = 2.dp)
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
                fontSize = 11.5.sp,
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
    filterCategory: FileCategory,
    sortMode: SortMode,
    files: List<com.tvfile.app.data.model.TVFile>,
    usbDevices: List<com.tvfile.app.data.model.StorageDevice>,
    servers: List<com.tvfile.app.data.database.ServerEntity>,
    playbackProgressMap: Map<String, Float>,
    canPaste: Boolean,
    onRootClick: () -> Unit,
    onToggleView: () -> Unit,
    onOpenSortDialog: () -> Unit,
    onPaste: () -> Unit,
    onCategorySelected: (FileCategory) -> Unit,
    onDeviceClick: (com.tvfile.app.data.model.StorageDevice) -> Unit,
    onAddServerClick: () -> Unit,
    onFileClick: (com.tvfile.app.data.model.TVFile) -> Unit,
    onFileLongClick: (com.tvfile.app.data.model.TVFile) -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        // 顶部控制条 (解决问题 2: TVButton 带清晰发光描边; 解决问题 8: 排序切换)
        HeaderToolbar(
            rootName = currentDeviceName,
            currentPath = currentPath,
            viewMode = viewMode,
            sortMode = sortMode,
            canPaste = canPaste,
            onRootClick = onRootClick,
            onToggleView = onToggleView,
            onOpenSortDialog = onOpenSortDialog,
            onPaste = onPaste
        )

        // 仅在第2层(具体目录层)时展示文件类型过滤 Chip
        if (level == 2) {
            FilterBar(
                selectedCategory = filterCategory,
                onCategorySelected = onCategorySelected,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp)
            )
        }

        Box(modifier = Modifier.weight(1f)) {
            if (level == 1) {
                // 第1层：设备/服务器列表 (解决问题 3: 空列表时大号添加引导卡片)
                DeviceListScreen(
                    currentTab = currentTab,
                    usbDevices = usbDevices,
                    servers = servers,
                    onDeviceClick = onDeviceClick,
                    onAddServerClick = onAddServerClick
                )
            } else {
                // 第2层：具体目录文件浏览 (解决问题 1: 固定 5 列排版，大图标大字体)
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

/**
 * 顶部状态栏：无 TV Edition 标签
 */
@Composable
private fun TopStatusBar(activeTitle: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(46.dp)
            .background(TVSurface)
            .padding(horizontal = 22.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(9.dp)
                    .background(TVAccentCyan, shape = MaterialTheme.shapes.extraSmall)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = "TVFile",
                color = Color.White,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            Text(text = "当前: $activeTitle", color = TVTextSecondary, fontSize = 13.5.sp)
            Text(text = "📶 局域网在线", color = TVTextMuted, fontSize = 13.5.sp)
        }
    }
}

/**
 * 左侧侧边栏：【解决问题 1】：字体大幅放大，图标放大，3米大屏视距清晰可见
 */
@Composable
private fun Sidebar(
    selectedTab: StorageType,
    onTabSelected: (StorageType) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .background(Color(0xFF0D121C))
            .padding(18.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
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
            SidebarTabItem(
                title = "Web 传输",
                subtitle = "HTTP: 45789",
                icon = Icons.Default.CloudSync,
                isSelected = selectedTab == StorageType.WEB_TRANSFER,
                onClick = { onTabSelected(StorageType.WEB_TRANSFER) }
            )
        }

        Surface(
            shape = RoundedCornerShape(12.dp),
            color = TVCardBg,
            border = BorderStroke(1.dp, TVBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(text = "遥控器快捷提示", color = TVTextPrimary, fontSize = 12.5.sp, fontWeight = FontWeight.Bold)
                Text(text = "OK: 打开 / 进入", color = TVTextSecondary, fontSize = 11.5.sp)
                Text(text = "MENU: 文件操作抽屉", color = TVTextSecondary, fontSize = 11.5.sp)
                Text(text = "BACK: 返回上一级", color = TVTextSecondary, fontSize = 11.5.sp)
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
        shape = RoundedCornerShape(14.dp),
        focusedBorderColor = TVAccentCyan,
        unfocusedBorderColor = if (isSelected) TVAccentCyan.copy(alpha = 0.6f) else Color.Transparent,
        focusedBackgroundColor = Color(0xFF1B2F4C),
        unfocusedBackgroundColor = if (isSelected) Color(0xFF131D2D) else Color.Transparent,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 14.dp, vertical = 14.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isSelected) TVAccentCyan else TVTextSecondary,
                modifier = Modifier.size(30.dp)
            )
            Spacer(modifier = Modifier.width(14.dp))
            Column {
                Text(
                    text = title,
                    color = if (isSelected) Color.White else TVTextPrimary,
                    fontSize = 16.5.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold
                )
                Text(text = subtitle, color = TVTextMuted, fontSize = 12.sp)
            }
        }
    }
}

/**
 * 顶部工具栏：【解决问题 2】：全部换用 TVButton，带青蓝高亮边框；【解决问题 8】：增加排序按钮
 */
@Composable
private fun HeaderToolbar(
    rootName: String,
    currentPath: String,
    viewMode: ViewMode,
    sortMode: SortMode,
    canPaste: Boolean,
    onRootClick: () -> Unit,
    onToggleView: () -> Unit,
    onOpenSortDialog: () -> Unit,
    onPaste: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        BreadcrumbBar(
            rootName = rootName,
            currentPath = currentPath,
            onRootClick = onRootClick,
            modifier = Modifier.weight(1f)
        )

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            // 排序按钮 (解决问题 8)
            TVButton(
                onClick = onOpenSortDialog,
                focusedBackgroundColor = TVAccentBlue,
                unfocusedBackgroundColor = TVCardBg,
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
            ) { isFocused ->
                Icon(
                    imageVector = Icons.Default.Sort,
                    contentDescription = null,
                    tint = if (isFocused) Color.White else TVAccentCyan,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = sortMode.displayName.substringBefore(" ("),
                    color = if (isFocused) Color.White else TVTextPrimary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            // 视图切换按钮 (解决问题 2: TVButton 带清晰焦点发光描边)
            TVButton(
                onClick = onToggleView,
                focusedBackgroundColor = TVAccentBlue,
                unfocusedBackgroundColor = TVCardBg,
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
            ) { isFocused ->
                Icon(
                    imageVector = if (viewMode == ViewMode.GRID) Icons.AutoMirrored.Filled.ViewList else Icons.Default.GridView,
                    contentDescription = null,
                    tint = if (isFocused) Color.White else TVAccentCyan,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (viewMode == ViewMode.GRID) "列表" else "网格",
                    color = if (isFocused) Color.White else TVTextPrimary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            if (canPaste) {
                TVButton(
                    onClick = onPaste,
                    focusedBackgroundColor = TVAccentGreen,
                    unfocusedBackgroundColor = TVAccentGreen.copy(alpha = 0.8f),
                    focusedBorderColor = Color.White,
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                ) {
                    Icon(imageVector = Icons.Default.ContentPaste, contentDescription = null, tint = Color.Black)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = "粘贴", color = Color.Black, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
