const fs = require('fs');
const path = require('path');

const cssContent = `
#tvfile-prototype-root {
  --bg-color: #0b0f17;
  --card-bg: #161e2e;
  --card-hover: #1f2b42;
  --border-color: #27354f;
  --accent-cyan: #00f0ff;
  --accent-blue: #0077ff;
  --accent-orange: #ff9800;
  --accent-green: #00e676;
  --text-main: #f0f6fc;
  --text-muted: #8b9bb4;
  --focus-glow: 0 0 20px rgba(0, 240, 255, 0.65), 0 0 0 3px #00f0ff;
  
  font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, "PingFang SC", "Microsoft YaHei", sans-serif;
  background-color: #06090e;
  color: var(--text-main);
  padding: 16px;
  box-sizing: border-box;
  width: 100%;
  min-height: 720px;
  position: relative;
  user-select: none;
}

#tvfile-prototype-root * {
  box-sizing: border-box;
  transition: all 0.2s cubic-bezier(0.16, 1, 0.3, 1);
}

/* 控制条与场景快捷切换 */
.scenario-bar {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  justify-content: space-between;
  background: #111726;
  padding: 10px 16px;
  border-radius: 12px;
  margin-bottom: 16px;
  border: 1px solid var(--border-color);
  gap: 8px;
}
.scenario-title {
  font-size: 14px;
  font-weight: 600;
  color: var(--accent-cyan);
  display: flex;
  align-items: center;
  gap: 6px;
}
.scenario-btns {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}
.scenario-btn {
  background: #1d283d;
  color: var(--text-muted);
  border: 1px solid #2a3a57;
  padding: 6px 12px;
  border-radius: 8px;
  font-size: 12px;
  cursor: pointer;
  display: inline-flex;
  align-items: center;
  gap: 5px;
}
.scenario-btn:hover, .scenario-btn.active {
  background: #0077ff22;
  color: #fff;
  border-color: var(--accent-cyan);
  box-shadow: 0 0 10px rgba(0, 240, 255, 0.3);
}

/* 电视主机主视口 + 遥控器双列布局 */
.tv-simulator-wrapper {
  display: flex;
  gap: 20px;
  align-items: flex-start;
  justify-content: center;
}

/* 电视机大屏主体 */
.tv-frame {
  flex: 1;
  background: var(--bg-color);
  border-radius: 20px;
  border: 4px solid #1a2333;
  box-shadow: 0 20px 50px rgba(0,0,0,0.8), 0 0 0 1px rgba(255,255,255,0.05);
  display: flex;
  flex-direction: column;
  overflow: hidden;
  height: 640px;
  position: relative;
}

/* TV 顶部状态栏 */
.tv-status-bar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 10px 24px;
  background: #0f1522;
  border-bottom: 1px solid var(--border-color);
  font-size: 13px;
  color: var(--text-muted);
  height: 44px;
}
.brand-badge {
  color: #fff;
  font-weight: 700;
  letter-spacing: 1px;
  display: flex;
  align-items: center;
  gap: 8px;
}
.brand-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: var(--accent-cyan);
  box-shadow: 0 0 8px var(--accent-cyan);
}
.status-info {
  display: flex;
  align-items: center;
  gap: 16px;
}

/* TV 左右两栏布局 */
.tv-main-body {
  display: flex;
  flex: 1;
  height: calc(100% - 44px);
  overflow: hidden;
}

/* 左侧 Tab 栏 */
.tv-sidebar {
  width: 220px;
  background: #0d121c;
  border-right: 1px solid var(--border-color);
  display: flex;
  flex-direction: column;
  padding: 16px 12px;
  gap: 10px;
  justify-content: space-between;
}
.nav-tab-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}
.nav-tab-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 14px;
  border-radius: 12px;
  color: var(--text-muted);
  cursor: pointer;
  background: transparent;
  border: 1px solid transparent;
  font-size: 14px;
  font-weight: 500;
}
.nav-tab-item .tab-icon {
  font-size: 18px;
  width: 24px;
  text-align: center;
}
.nav-tab-item .tab-info {
  display: flex;
  flex-direction: column;
  flex: 1;
}
.nav-tab-item .tab-name {
  color: var(--text-main);
}
.nav-tab-item .tab-sub {
  font-size: 11px;
  color: var(--text-muted);
  margin-top: 2px;
}
.nav-tab-item.focused, .nav-tab-item:hover {
  background: var(--card-bg);
  color: #fff;
  border-color: var(--border-color);
}
.nav-tab-item.active {
  background: #142845;
  border-color: #0077ff66;
}
.nav-tab-item.tv-focus-target {
  box-shadow: var(--focus-glow) !important;
  transform: scale(1.04);
  background: #1a3258 !important;
  color: #fff !important;
  border-color: var(--accent-cyan) !important;
}

.storage-bar-wrapper {
  margin-top: 4px;
  height: 4px;
  background: #202b3d;
  border-radius: 2px;
  overflow: hidden;
  width: 100%;
}
.storage-bar-progress {
  height: 100%;
  background: var(--accent-cyan);
  border-radius: 2px;
}

/* 侧边栏底部简易遥控器按键提示 */
.sidebar-footer {
  padding: 10px;
  background: #121824;
  border-radius: 10px;
  font-size: 11px;
  color: var(--text-muted);
  border: 1px solid #1c2738;
  line-height: 1.5;
}
.key-badge {
  display: inline-block;
  padding: 1px 5px;
  background: #24324a;
  border-radius: 4px;
  color: #cdd9e5;
  font-weight: 600;
  font-size: 10px;
  margin: 0 2px;
}

/* 右侧主工作区 */
.tv-content-area {
  flex: 1;
  display: flex;
  flex-direction: column;
  background: #0b0f17;
  overflow: hidden;
  position: relative;
}

/* 顶部操作条（面包屑 + 类型过滤 + 切换） */
.content-header {
  padding: 12px 20px;
  background: #111824;
  border-bottom: 1px solid var(--border-color);
  display: flex;
  flex-direction: column;
  gap: 10px;
}
.header-top-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.breadcrumbs {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
  color: var(--text-muted);
}
.crumb-item {
  color: var(--text-main);
  font-weight: 600;
  cursor: pointer;
}
.crumb-item:hover {
  color: var(--accent-cyan);
}
.header-actions {
  display: flex;
  align-items: center;
  gap: 10px;
}
.action-btn {
  background: #1c273a;
  border: 1px solid #283954;
  color: var(--text-main);
  padding: 6px 12px;
  border-radius: 8px;
  font-size: 12px;
  cursor: pointer;
  display: inline-flex;
  align-items: center;
  gap: 6px;
}
.action-btn:hover {
  background: #25354e;
  border-color: var(--accent-cyan);
}
.action-btn.tv-focus-target {
  box-shadow: var(--focus-glow) !important;
  transform: scale(1.05);
  border-color: var(--accent-cyan) !important;
}

/* 需求10：文件类型过滤器（可切换隐藏/显示） */
.filter-row {
  display: flex;
  align-items: center;
  gap: 8px;
  overflow-x: auto;
  padding-bottom: 2px;
}
.filter-chip {
  padding: 5px 14px;
  border-radius: 20px;
  background: #182233;
  color: var(--text-muted);
  font-size: 12px;
  font-weight: 500;
  cursor: pointer;
  border: 1px solid transparent;
  display: inline-flex;
  align-items: center;
  gap: 6px;
}
.filter-chip:hover {
  background: #223048;
  color: #fff;
}
.filter-chip.active {
  background: #0077ff33;
  color: var(--accent-cyan);
  border-color: var(--accent-cyan);
  font-weight: 600;
}
.filter-chip.tv-focus-target {
  box-shadow: var(--focus-glow) !important;
  transform: scale(1.06);
  border-color: var(--accent-cyan) !important;
  color: #fff !important;
}

/* 主内容视口（设备列表层 / 文件列表层） */
.content-scrollable {
  flex: 1;
  overflow-y: auto;
  padding: 16px 20px;
}

/* 网格布局 */
.grid-container {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(140px, 1fr));
  gap: 14px;
}
.grid-card {
  background: var(--card-bg);
  border: 1px solid var(--border-color);
  border-radius: 12px;
  padding: 12px 10px;
  display: flex;
  flex-direction: column;
  align-items: center;
  text-align: center;
  cursor: pointer;
  position: relative;
  overflow: hidden;
}
.grid-card:hover {
  background: var(--card-hover);
  border-color: #3b4d6e;
}
.grid-card.tv-focus-target {
  box-shadow: var(--focus-glow) !important;
  transform: scale(1.06) translateY(-2px);
  border-color: var(--accent-cyan) !important;
  background: #1a2842 !important;
  z-index: 5;
}
.card-icon-wrapper {
  width: 58px;
  height: 58px;
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 8px;
  font-size: 28px;
  background: #111724;
}
.card-title {
  font-size: 13px;
  font-weight: 500;
  color: var(--text-main);
  max-width: 100%;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  margin-bottom: 4px;
}
.card-subtitle {
  font-size: 11px;
  color: var(--text-muted);
}
.card-badge {
  position: absolute;
  top: 6px;
  right: 6px;
  font-size: 9px;
  padding: 2px 5px;
  border-radius: 4px;
  font-weight: 600;
  text-transform: uppercase;
}
.badge-video { background: #e5091444; color: #ff5252; border: 1px solid #ff525244; }
.badge-audio { background: #9c27b044; color: #e040fb; border: 1px solid #e040fb44; }
.badge-image { background: #00bcd444; color: #18ffff; border: 1px solid #18ffff44; }
.badge-apk { background: #4caf5044; color: #69f0ae; border: 1px solid #69f0ae44; }

/* 列表布局 */
.list-container {
  display: flex;
  flex-direction: column;
  gap: 6px;
}
.list-row {
  display: flex;
  align-items: center;
  padding: 10px 14px;
  background: var(--card-bg);
  border: 1px solid var(--border-color);
  border-radius: 10px;
  cursor: pointer;
  font-size: 13px;
}
.list-row:hover {
  background: var(--card-hover);
}
.list-row.tv-focus-target {
  box-shadow: var(--focus-glow) !important;
  transform: scale(1.02);
  border-color: var(--accent-cyan) !important;
  background: #1a2842 !important;
  z-index: 5;
}
.list-icon {
  font-size: 20px;
  width: 32px;
  text-align: center;
  margin-right: 12px;
}
.list-name {
  flex: 2;
  font-weight: 500;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.list-meta {
  flex: 1;
  color: var(--text-muted);
  font-size: 12px;
}
.list-action {
  color: var(--accent-cyan);
  font-size: 12px;
  font-weight: 600;
}

/* 设备/服务器卡片大格样式（外接存储/网络存储 第1层） */
.device-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(220px, 1fr));
  gap: 16px;
}
.device-card {
  background: var(--card-bg);
  border: 1px solid var(--border-color);
  border-radius: 14px;
  padding: 16px;
  cursor: pointer;
  position: relative;
  display: flex;
  flex-direction: column;
  gap: 12px;
}
.device-card:hover {
  background: var(--card-hover);
}
.device-card.tv-focus-target {
  box-shadow: var(--focus-glow) !important;
  transform: scale(1.05);
  border-color: var(--accent-cyan) !important;
  background: #17263d !important;
}
.device-card-header {
  display: flex;
  align-items: center;
  gap: 12px;
}
.device-icon-box {
  width: 44px;
  height: 44px;
  border-radius: 10px;
  background: #111a29;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 22px;
  border: 1px solid #20314a;
}
.device-info-text {
  flex: 1;
}
.device-name {
  font-size: 14px;
  font-weight: 600;
  color: #fff;
}
.device-proto {
  font-size: 11px;
  color: var(--accent-cyan);
  margin-top: 2px;
}
.device-detail-bar {
  display: flex;
  flex-direction: column;
  gap: 6px;
  font-size: 11px;
  color: var(--text-muted);
}
.status-pill {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  font-size: 10px;
  padding: 2px 6px;
  border-radius: 4px;
  width: fit-content;
}
.status-online { background: #00e67622; color: #00e676; border: 1px solid #00e67644; }
.status-offline { background: #ff525222; color: #ff5252; border: 1px solid #ff525244; }

/* 模拟遥控器区域 */
.remote-control-panel {
  width: 170px;
  background: #111622;
  border-radius: 28px;
  border: 2px solid #222e44;
  padding: 20px 14px;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 16px;
  box-shadow: 0 10px 30px rgba(0,0,0,0.5);
}
.remote-title {
  font-size: 11px;
  text-transform: uppercase;
  letter-spacing: 1px;
  color: var(--text-muted);
  font-weight: 700;
}
.dpad-container {
  width: 130px;
  height: 130px;
  position: relative;
  background: #1c263a;
  border-radius: 50%;
  border: 2px solid #2a3c5a;
  box-shadow: inset 0 2px 10px rgba(0,0,0,0.6);
}
.dpad-btn {
  position: absolute;
  background: transparent;
  border: none;
  color: #94a3b8;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 16px;
  font-weight: bold;
}
.dpad-btn:hover, .dpad-btn:active {
  color: var(--accent-cyan);
}
.dpad-up { top: 4px; left: 45px; width: 40px; height: 35px; }
.dpad-down { bottom: 4px; left: 45px; width: 40px; height: 35px; }
.dpad-left { left: 4px; top: 45px; width: 35px; height: 40px; }
.dpad-right { right: 4px; top: 45px; width: 35px; height: 40px; }
.dpad-center {
  top: 38px;
  left: 38px;
  width: 54px;
  height: 54px;
  background: #25334e;
  border-radius: 50%;
  border: 1px solid #364b6e;
  color: #fff;
  font-size: 13px;
  font-weight: 700;
}
.dpad-center:hover {
  background: #0077ff;
  box-shadow: 0 0 12px var(--accent-cyan);
}

.remote-action-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 10px;
  width: 100%;
}
.remote-key-btn {
  background: #1b263b;
  border: 1px solid #2e4060;
  color: #cbd5e1;
  padding: 8px 4px;
  border-radius: 12px;
  font-size: 11px;
  font-weight: 600;
  cursor: pointer;
  text-align: center;
}
.remote-key-btn:hover {
  background: #283955;
  color: #fff;
  border-color: var(--accent-cyan);
}

/* 浮层与弹窗：ExoPlayer 视频播放器 */
.player-modal-backdrop {
  position: absolute;
  inset: 0;
  background: rgba(0, 0, 0, 0.95);
  display: none;
  flex-direction: column;
  justify-content: space-between;
  z-index: 100;
  padding: 24px;
  backdrop-filter: blur(8px);
}
.player-video-dummy {
  position: absolute;
  inset: 0;
  background: radial-gradient(circle at center, #182848 0%, #000 70%);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: -1;
}
.player-osd-top {
  display: flex;
  justify-content: space-between;
  align-items: center;
  background: linear-gradient(180deg, rgba(0,0,0,0.8) 0%, transparent 100%);
  padding: 10px 16px;
  border-radius: 8px;
}
.player-osd-bottom {
  background: linear-gradient(0deg, rgba(0,0,0,0.9) 0%, transparent 100%);
  padding: 20px;
  border-radius: 12px;
  display: flex;
  flex-direction: column;
  gap: 12px;
}
.progress-bar-container {
  height: 6px;
  background: #334155;
  border-radius: 3px;
  position: relative;
  cursor: pointer;
}
.progress-bar-fill {
  height: 100%;
  width: 48%;
  background: var(--accent-cyan);
  border-radius: 3px;
  box-shadow: 0 0 8px var(--accent-cyan);
}
.player-controls-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 13px;
}

/* 音乐播放器弹窗 */
.audio-modal-backdrop {
  position: absolute;
  inset: 0;
  background: rgba(6, 10, 18, 0.96);
  display: none;
  align-items: center;
  justify-content: center;
  z-index: 100;
  padding: 30px;
}
.audio-player-card {
  width: 600px;
  background: #111a29;
  border: 1px solid var(--border-color);
  border-radius: 20px;
  padding: 24px;
  display: flex;
  gap: 24px;
  align-items: center;
  box-shadow: 0 20px 40px rgba(0,0,0,0.8);
}
.vinyl-disc {
  width: 140px;
  height: 140px;
  border-radius: 50%;
  background: radial-gradient(circle, #2b394e 20%, #0d121c 21%, #0d121c 70%, #202b3c 71%);
  border: 3px solid #334460;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 36px;
  animation: spin 8s linear infinite;
  box-shadow: 0 0 20px rgba(0,0,0,0.7);
}
@keyframes spin { 100% { transform: rotate(360deg); } }

/* 音频波形频谱条 */
.equalizer-bars {
  display: flex;
  align-items: flex-end;
  gap: 4px;
  height: 28px;
  margin-top: 10px;
}
.eq-bar {
  width: 4px;
  background: var(--accent-cyan);
  border-radius: 2px;
  animation: bounce 1.2s ease-in-out infinite alternate;
}
@keyframes bounce {
  0% { height: 4px; }
  100% { height: 26px; }
}

/* 图片查看器 */
.image-modal-backdrop {
  position: absolute;
  inset: 0;
  background: rgba(0, 0, 0, 0.98);
  display: none;
  align-items: center;
  justify-content: center;
  z-index: 100;
}

/* APK 安装弹窗 */
.apk-modal-backdrop {
  position: absolute;
  inset: 0;
  background: rgba(0, 0, 0, 0.85);
  display: none;
  align-items: center;
  justify-content: center;
  z-index: 100;
}
.apk-card {
  width: 420px;
  background: #141d2c;
  border: 2px solid var(--accent-green);
  border-radius: 16px;
  padding: 24px;
  display: flex;
  flex-direction: column;
  gap: 16px;
  box-shadow: 0 10px 40px rgba(0,230,118,0.25);
}

/* 菜单操作抽屉 */
.menu-drawer {
  position: absolute;
  top: 44px;
  right: 0;
  bottom: 0;
  width: 260px;
  background: #131b29;
  border-left: 2px solid var(--accent-cyan);
  display: none;
  flex-direction: column;
  padding: 20px 16px;
  gap: 10px;
  z-index: 50;
  box-shadow: -10px 0 30px rgba(0,0,0,0.7);
}

/* 传输小浮窗 */
.transfer-float-widget {
  position: absolute;
  bottom: 16px;
  right: 20px;
  background: #131e31;
  border: 1px solid var(--border-color);
  border-radius: 12px;
  padding: 10px 16px;
  display: flex;
  align-items: center;
  gap: 12px;
  font-size: 12px;
  box-shadow: 0 8px 24px rgba(0,0,0,0.5);
  z-index: 20;
}
`;

fs.writeFileSync(path.join(__dirname, 'prototype_styles.css'), cssContent, 'utf-8');
console.log('CSS generated');
