package com.tvfile.app.presentation.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.tvfile.app.data.database.ServerEntity
import com.tvfile.app.data.model.*
import com.tvfile.app.data.provider.IStorageProvider
import com.tvfile.app.data.repository.StorageRepository
import com.tvfile.app.data.transfer.FileOperationManager
import com.tvfile.app.util.ApkInstaller
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.File

class MainViewModel(application: Application) : AndroidViewModel(application) {

    val repository = StorageRepository(application)
    val fileOperationManager = FileOperationManager()

    // 1. Tab 状态
    private val _currentTab = MutableStateFlow(StorageType.INTERNAL)
    val currentTab: StateFlow<StorageType> = _currentTab.asStateFlow()

    // 2. 层级状态机 (需求10: 1=设备列表层, 2=具体目录层)
    private val _level = MutableStateFlow(2) // 内部存储默认就是目录层
    val level: StateFlow<Int> = _level.asStateFlow()

    // 3. 当前挂载设备 / 服务器名称
    private val _currentDevice = MutableStateFlow<StorageDevice?>(null)
    val currentDevice: StateFlow<StorageDevice?> = _currentDevice.asStateFlow()

    // 4. 当前浏览路径
    private val _currentPath = MutableStateFlow("")
    val currentPath: StateFlow<String> = _currentPath.asStateFlow()

    // 5. 文件类型过滤 (全部/视频/音乐/图片/安装包)
    private val _filterCategory = MutableStateFlow(FileCategory.ALL)
    val filterCategory: StateFlow<FileCategory> = _filterCategory.asStateFlow()

    // 5.1 排序模式 (最新优先、名称 A-Z、大小等)
    private val _sortMode = MutableStateFlow(com.tvfile.app.data.model.SortMode.TIME_DESC)
    val sortMode: StateFlow<com.tvfile.app.data.model.SortMode> = _sortMode.asStateFlow()

    // 6. 视图模式 (网格 vs 列表)
    private val _viewMode = MutableStateFlow(ViewMode.GRID)
    val viewMode: StateFlow<ViewMode> = _viewMode.asStateFlow()

    // 7. 文件列表与设备列表
    private val _rawFileList = MutableStateFlow<List<TVFile>>(emptyList())
    val usbDevices: StateFlow<List<StorageDevice>> = repository.usbDevices
    val servers: StateFlow<List<ServerEntity>> = repository.getAllServers().stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList()
    )

    // 经过类型过滤和排序模式处理的文件列表 (文件夹永远优先排在前面)
    val fileList: StateFlow<List<TVFile>> = combine(_rawFileList, _filterCategory, _sortMode) { files, cat, sort ->
        val filtered = if (cat == FileCategory.ALL) files else files.filter { it.category == cat }
        val dirs = filtered.filter { it.isDirectory }
        val nonDirs = filtered.filter { !it.isDirectory }

        val comparator = when (sort) {
            com.tvfile.app.data.model.SortMode.TIME_DESC -> compareByDescending<TVFile> { it.lastModified }
            com.tvfile.app.data.model.SortMode.TIME_ASC -> compareBy<TVFile> { it.lastModified }
            com.tvfile.app.data.model.SortMode.NAME_ASC -> compareBy<TVFile> { it.name.lowercase() }
            com.tvfile.app.data.model.SortMode.NAME_DESC -> compareByDescending<TVFile> { it.name.lowercase() }
            com.tvfile.app.data.model.SortMode.SIZE_DESC -> compareByDescending<TVFile> { it.size }
            com.tvfile.app.data.model.SortMode.SIZE_ASC -> compareBy<TVFile> { it.size }
        }

        dirs.sortedWith(comparator) + nonDirs.sortedWith(comparator)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    
    // 视频已播放进度映射 (fileId -> progress 0.0f..1.0f)
    val playbackProgressMap: StateFlow<Map<String, Float>> = repository.getAllProgressFlow().map { list ->
        list.associate { entity ->
            val ratio = if (entity.durationMs > 0) (entity.positionMs.toFloat() / entity.durationMs).coerceIn(0f, 1f) else 0f
            entity.fileId to ratio
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())


    // 10. Web 传输专属状态
    private val _isHttpServerRunning = MutableStateFlow(false)
    val isHttpServerRunning: StateFlow<Boolean> = _isHttpServerRunning.asStateFlow()

    private val _webServerUrl = MutableStateFlow("")
    val webServerUrl: StateFlow<String> = _webServerUrl.asStateFlow()

    private val _receivedFiles = MutableStateFlow<List<TVFile>>(emptyList())
    val receivedFiles: StateFlow<List<TVFile>> = _receivedFiles.asStateFlow()

    private val _webFilterCategory = MutableStateFlow(FileCategory.ALL)
    val webFilterCategory: StateFlow<FileCategory> = _webFilterCategory.asStateFlow()

    val filteredReceivedFiles: StateFlow<List<TVFile>> = combine(_receivedFiles, _webFilterCategory) { files, cat ->
        if (cat == FileCategory.ALL) files else files.filter { it.category == cat }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

// 8. 播放与查看状态
    private val _activeVideoUrl = MutableStateFlow<Triple<TVFile, String, Long>?>(null)
    val activeVideoUrl: StateFlow<Triple<TVFile, String, Long>?> = _activeVideoUrl.asStateFlow()

    private val _activeAudioUrl = MutableStateFlow<Pair<TVFile, String>?>(null)
    val activeAudioUrl: StateFlow<Pair<TVFile, String>?> = _activeAudioUrl.asStateFlow()

    private val _activeImageUrl = MutableStateFlow<Pair<TVFile, String>?>(null)
    val activeImageUrl: StateFlow<Pair<TVFile, String>?> = _activeImageUrl.asStateFlow()

    private val _apkDownloadProgress = MutableStateFlow<Int?>(null)
    val apkDownloadProgress: StateFlow<Int?> = _apkDownloadProgress.asStateFlow()

    private val _downloadingApkFile = MutableStateFlow<TVFile?>(null)
    val downloadingApkFile: StateFlow<TVFile?> = _downloadingApkFile.asStateFlow()

    private var apkDownloadJob: kotlinx.coroutines.Job? = null

    // 9. 选中与操作菜单
    private val _selectedFile = MutableStateFlow<TVFile?>(null)
    val selectedFile: StateFlow<TVFile?> = _selectedFile.asStateFlow()

    private val _isMenuOpen = MutableStateFlow(false)
    val isMenuOpen: StateFlow<Boolean> = _isMenuOpen.asStateFlow()

    private val _isAddServerDialogOpen = MutableStateFlow(false)
    val isAddServerDialogOpen: StateFlow<Boolean> = _isAddServerDialogOpen.asStateFlow()

    init {
        // 初始加载内部存储
        selectTab(StorageType.INTERNAL)
    }

    fun selectTab(tab: StorageType) {
        _currentTab.value = tab
        _filterCategory.value = FileCategory.ALL
        if (tab == StorageType.INTERNAL) {
            _level.value = 2
            _currentDevice.value = StorageDevice(
                id = "internal",
                name = "内部存储",
                path = repository.localProvider.rootPath,
                storageType = StorageType.INTERNAL
            )
            loadFiles(repository.localProvider.rootPath, repository.localProvider)
        } else if (tab == StorageType.WEB_TRANSFER) {
            _level.value = 1
            _currentDevice.value = null
            _currentPath.value = "/tvfile"
            _isHttpServerRunning.value = repository.isWebTransferRunning()
            if (_isHttpServerRunning.value) {
                val ip = com.tvfile.app.util.NetworkUtils.getLocalIpAddress()
                _webServerUrl.value = "http://$ip:45789"
            }
            refreshReceivedFiles()
        } else {
            // 外接存储与网络存储默认进入第1层 (需求10: 无文件过滤)
            _level.value = 1
            _currentDevice.value = null
            _currentPath.value = ""
            _rawFileList.value = emptyList()
        }
    }

    fun enterDevice(device: StorageDevice) {
        _currentDevice.value = device
        _level.value = 2 // 进入第2层目录 (需求10: 展开文件过滤)
        viewModelScope.launch {
            val provider = getProviderForDevice(device) ?: return@launch
            loadFiles(device.path, provider)
        }
    }

    fun enterDirectory(dirFile: TVFile) {
        viewModelScope.launch {
            val provider = getCurrentProvider() ?: return@launch
            loadFiles(dirFile.path, provider)
        }
    }

    fun navigateBack(): Boolean {
        if (_isMenuOpen.value) {
            _isMenuOpen.value = false
            return true
        }
        if (_activeVideoUrl.value != null) {
            _activeVideoUrl.value = null
            return true
        }
        if (_activeAudioUrl.value != null) {
            _activeAudioUrl.value = null
            return true
        }
        if (_activeImageUrl.value != null) {
            _activeImageUrl.value = null
            return true
        }

        // 目录层退回上一级
        if (_level.value == 2) {
            val curPath = _currentPath.value
            val dev = _currentDevice.value
            val rootPath = dev?.path ?: ""

            if (curPath.isNotBlank() && curPath != rootPath && curPath != "/" && curPath.contains('/')) {
                val parent = curPath.trimEnd('/').substringBeforeLast('/', "")
                val targetParent = if (parent.isBlank()) rootPath else parent
                viewModelScope.launch {
                    val provider = getCurrentProvider() ?: return@launch
                    loadFiles(targetParent, provider)
                }
                return true
            } else {
                // 如果在根目录，且是外接/网络存储，按返回键退回第1层设备列表
                if (_currentTab.value == StorageType.USB || _currentTab.value == StorageType.SMB || _currentTab.value == StorageType.WEBDAV) {
                    _level.value = 1
                    _currentDevice.value = null
                    _rawFileList.value = emptyList()
                    return true
                }
            }
        }
        return false
    }

    fun loadFiles(path: String, provider: IStorageProvider) {
        _currentPath.value = path
        viewModelScope.launch {
            val result = provider.listFiles(path)
            _rawFileList.value = result.getOrDefault(emptyList())
        }
    }

    fun toggleViewMode() {
        _viewMode.value = if (_viewMode.value == ViewMode.GRID) ViewMode.LIST else ViewMode.GRID
    }

    fun setFilterCategory(cat: FileCategory) {
        _filterCategory.value = cat
    }

    fun openFile(file: TVFile) {
        if (file.isDirectory) {
            enterDirectory(file)
            return
        }
        viewModelScope.launch {
            val provider = getCurrentProvider() ?: return@launch
            when (file.category) {
                FileCategory.VIDEO -> {
                    val savedPos = getVideoSavedPosition(file.id)
                    if (file.storageType == StorageType.INTERNAL || file.storageType == StorageType.USB) {
                        _activeVideoUrl.value = Triple(file, "file://${file.path}", savedPos)
                    } else {
                        // SMB/WebDAV: 本地 NanoHTTPD 流媒体秒播代理
                        val streamUrl = repository.streamerServer.registerStream(file, provider)
                        _activeVideoUrl.value = Triple(file, streamUrl, savedPos)
                    }
                }
                FileCategory.AUDIO -> {
                    if (file.storageType == StorageType.INTERNAL || file.storageType == StorageType.USB) {
                        _activeAudioUrl.value = Pair(file, "file://${file.path}")
                    } else {
                        val streamUrl = repository.streamerServer.registerStream(file, provider)
                        _activeAudioUrl.value = Pair(file, streamUrl)
                    }
                }
                FileCategory.IMAGE -> {
                    if (file.storageType == StorageType.INTERNAL || file.storageType == StorageType.USB) {
                        _activeImageUrl.value = Pair(file, "file://${file.path}")
                    } else {
                        val streamUrl = repository.streamerServer.registerStream(file, provider)
                        _activeImageUrl.value = Pair(file, streamUrl)
                    }
                }
                FileCategory.APK -> {
                    if (file.storageType == StorageType.INTERNAL || file.storageType == StorageType.USB) {
                        ApkInstaller.installApk(getApplication(), File(file.path))
                    } else {
                        _downloadingApkFile.value = file
                        _apkDownloadProgress.value = 0
                        apkDownloadJob?.cancel()
                        apkDownloadJob = viewModelScope.launch {
                            ApkInstaller.downloadAndInstall(getApplication(), file, provider) { progress ->
                                _apkDownloadProgress.value = progress
                            }
                            _apkDownloadProgress.value = null
                            _downloadingApkFile.value = null
                        }
                    }
                }
                else -> {}
            }
        }
    }

    fun closePlayers() {
        _activeVideoUrl.value = null
        _activeAudioUrl.value = null
        _activeImageUrl.value = null
        _apkDownloadProgress.value = null
    }

    fun openContextMenu(file: TVFile?) {
        _selectedFile.value = file
        _isMenuOpen.value = true
    }

    fun closeContextMenu() {
        _isMenuOpen.value = false
    }

    fun setAddServerDialogOpen(open: Boolean) {
        _isAddServerDialogOpen.value = open
    }

    fun addServer(server: ServerEntity) {
        viewModelScope.launch {
            repository.addServer(server)
            _isAddServerDialogOpen.value = false
        }
    }

    // 文件操作 (复制/剪切/粘贴/删除)
    fun copySelected() {
        viewModelScope.launch {
            val file = _selectedFile.value ?: return@launch
            val provider = getCurrentProvider() ?: return@launch
            fileOperationManager.copyToClipboard(listOf(file), provider)
            closeContextMenu()
        }
    }

    fun cutSelected() {
        viewModelScope.launch {
            val file = _selectedFile.value ?: return@launch
            val provider = getCurrentProvider() ?: return@launch
            fileOperationManager.cutToClipboard(listOf(file), provider)
            closeContextMenu()
        }
    }

    fun pasteToCurrent() {
        viewModelScope.launch {
            val provider = getCurrentProvider() ?: return@launch
            fileOperationManager.startPaste(_currentPath.value, provider, _currentTab.value)
            closeContextMenu()
        }
    }

    fun deleteSelected() {
        viewModelScope.launch {
            val file = _selectedFile.value ?: return@launch
            val provider = getCurrentProvider() ?: return@launch
            provider.delete(file)
            closeContextMenu()
            loadFiles(_currentPath.value, provider)
        }
    }

    suspend fun getCurrentProvider(): IStorageProvider? {
        val dev = _currentDevice.value
        return if (dev != null) getProviderForDevice(dev) else repository.localProvider
    }

    private suspend fun getProviderForDevice(device: StorageDevice): IStorageProvider? {
        return when (device.storageType) {
            StorageType.INTERNAL, StorageType.WEB_TRANSFER -> repository.localProvider
            StorageType.USB -> repository.getUsbProvider(device.path)
            StorageType.SMB, StorageType.WEBDAV -> {
                val sid = device.serverId ?: return null
                repository.getNetworkProvider(sid)
            }
        }
    }

    fun saveVideoProgress(fileId: String, positionMs: Long, durationMs: Long) {
        viewModelScope.launch {
            repository.savePlaybackProgress(fileId, positionMs, durationMs)
        }
    }

    suspend fun getVideoSavedPosition(fileId: String): Long {
        val entity = repository.getPlaybackProgress(fileId) ?: return 0L
        if (entity.durationMs > 0 && entity.positionMs >= (entity.durationMs * 0.95f).toLong()) {
            return 0L
        }
        return entity.positionMs
    }


    fun toggleHttpServer() {
        val currentlyRunning = repository.isWebTransferRunning()
        if (currentlyRunning) {
            repository.stopWebTransferServer()
            _isHttpServerRunning.value = false
            _webServerUrl.value = ""
        } else {
            val started = repository.startWebTransferServer {
                refreshReceivedFiles()
            }
            if (started) {
                _isHttpServerRunning.value = true
                val ip = com.tvfile.app.util.NetworkUtils.getLocalIpAddress()
                _webServerUrl.value = "http://$ip:45789"
                refreshReceivedFiles()
            }
        }
    }

    fun refreshReceivedFiles() {
        viewModelScope.launch {
            _receivedFiles.value = repository.getReceivedFiles()
        }
    }

    fun deleteReceivedFile(file: TVFile) {
        viewModelScope.launch {
            repository.deleteReceivedFile(file)
            refreshReceivedFiles()
        }
    }

    fun clearAllReceivedFiles() {
        viewModelScope.launch {
            repository.clearAllReceivedFiles()
            refreshReceivedFiles()
        }
    }

    fun setWebFilterCategory(category: FileCategory) {
        _webFilterCategory.value = category
    }


    fun setSortMode(mode: com.tvfile.app.data.model.SortMode) {
        _sortMode.value = mode
    }

    fun cancelApkDownload() {
        apkDownloadJob?.cancel()
        _apkDownloadProgress.value = null
        _downloadingApkFile.value = null
    }

}
