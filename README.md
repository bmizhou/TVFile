# TVFile - Android TV 现代文件管理器

专为智能电视、电视盒子与投影仪量身定制的高性能现代文件管理器工具，基于 Jetpack Compose for TV 与 Material 3 架构打造。

---

## ✨ 核心功能特性

1. **现代电视大屏 UI 与遥控器交互体验**：
   - 护眼深色 OLED 纯黑质感主题，青蓝极光发光描边（Focus Border）；
   - 获取焦点的卡片支持 1.05x 缓动微放大；
   - 遥控器 D-Pad 上下左右无死区流动，按键防丢焦处理；
   - 拦截遥控器 `MENU` 菜单键（或长按 OK 键）滑出文件操作抽屉。

2. **多协议网络存储连接 (SMB & WebDAV)**：
   - **SMB 2/3 原生支持**：基于 `smbj:0.13.0` 纯 Java 实现，连接群晖/威联通/极空间 NAS 及 Windows 局域网电脑共享文件夹；
   - **WebDAV 支持**：基于 OkHttp 实现轻量稳定 WebDAV 客户端（PROPFIND、GET、PUT、MOVE、DELETE、MKCOL），支持 Basic 认证与私有云连接；
   - **Room 数据库**：持久化保存局域网服务器连接配置。

3. **APK 应用安装**：
   - 兼容 Android 8.0+ 未知应用来源安装引导；
   - 使用 `FileProvider` 生成安全 `content://` 协议 URI；
   - 针对远程 SMB/WebDAV 上的 APK，支持带进度条缓存后自动唤起系统安装器。

4. **内置 ExoPlayer 视频/音乐播放（支持远程免下载秒播）**：
   - **视频播放器**：全屏沉浸播放，遥控器左右键快进/快退 10 秒，OK 暂停/播放；
   - **音乐播放器**：大屏卡片播放器，黑胶唱片旋转动效、动态波形跳动频谱；
   - **核心技术创新**：集成 `NanoHTTPD` 本地流媒体代理服务，将远程 SMB / WebDAV 多媒体流转换为带 Range 头的本地 HTTP 直连流，ExoPlayer 随意拖动进度条（Seek），零等待即点即播！

5. **内置图片画廊查看器**：
   - 基于 Coil 的全屏大图查看器，支持电视大图智能降采样防 OOM，遥控器左右切图。

6. **外接 U 盘自动识别与热插拔监听**：
   - 动态监听 USB 广播（`ACTION_MEDIA_MOUNTED` / `UNMOUNTED`）；
   - 自动识别 U 盘卷标、文件系统格式（FAT32/exFAT/NTFS）与剩余可用容量；
   - 针对安卓电视盒子特殊挂载路径进行底层自动扫描兜底。

7. **跨存储文件剪切 / 复制 / 粘贴引擎**：
   - 支持在内部存储、外接 U 盘、SMB、WebDAV 任意两端互拷与移动；
   - 采用 2MB 缓冲块管道流传输，高效且不占内存；
   - 悬浮传输进度条，实时显示传输百分比、已传大小与 MB/s 速度。

8. **网格 (Grid) / 列表 (List) 布局一键切换**：
   - 网格模式：大图标、海报缩略图、绿色 APK 机器人图标；
   - 列表模式：单行紧凑排版，展示文件类型、文件大小与修改时间。

9. **双栏布局与两层层级状态机**：
   - 左侧 Tab 栏：内部存储、外接存储、网络存储、传输任务；
   - **第一层（未点进具体设备/服务器时）**：
     - 展示外接 U 盘设备卡片，或 SMB/WebDAV 服务器列表；
     - **顶部完全隐藏文件类型过滤栏**；
   - **第二层（进入具体设备或内部存储后）**：
     - 顶部动态展开【全部 / 视频 / 音乐 / 图片 / 安装包】类型过滤 Chip；
     - 按遥控器返回键平滑退回第一层设备列表。

---

## 🛠️ 构建与开发环境

- **Gradle**：9.5.0 (Gradle Wrapper)
- **AGP (Android Gradle Plugin)**：9.3.0
- **Kotlin**：2.1.20
- **Compose Compiler**：2.2.10
- **Compose BOM**：2024.10.01
- **Compose for TV**：`androidx.tv:tv-foundation:1.0.0` / `androidx.tv:tv-material:1.1.0`
- **JDK**：Java 17 / Corretto-24
- **compileSdk / targetSdk**：37 (minSdk: 23)

### 编译 APK 命令：
```bash
# 编译 Debug APK
./gradlew assembleDebug

# APK 生成路径：
# app/build/outputs/apk/debug/app-debug.apk
```
