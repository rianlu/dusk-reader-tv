# 暮阅 (Dusk Reader TV) - 设计规范与开发文档

本文档参照 **Apple TV (tvOS) Human Interface Guidelines** 制定，旨在为 Android TV 提供极致灵动的阅读体验。

---

## 1. 设计哲学 (Design Philosophy)
*   **灵动焦点 (Connectedness)**：用户通过遥控器与屏幕产生的每一次交互都应有即时的视觉反馈。
*   **沉浸感 (Immersion)**：利用大屏优势，减少 UI 噪音，让文字内容成为绝对主角。
*   **轻盈导航 (Fluidity)**：减少点击次数，利用“焦点跟随”实现所见即所得。

---

## 2. 视觉与交互规范 (Apple TV Style)

### 2.1 焦点引擎 (The Focus Engine)
*   **缩放动画 (Scaling)**：所有可交互元素（如书籍卡片）在获得焦点时，必须进行 **1.1x - 1.15x** 的放大偏移。
*   **外部发光 (Glow/Shadow)**：获得焦点的元素应带有柔和的投影或外发光，模拟“悬浮”在屏幕上的视觉深度。
*   **视差感 (Parallax)**：在复杂卡片上，焦点移动应伴随微小的倾斜感。

### 2.2 导航体系 (Navigation)
*   **侧边栏联动 (Standard Sidebar)**：采用侧边栏布局。当焦点在导航项上移动时，右侧内容应 **同步切换**（Focus-follows-selection），无需用户按下确认键。
*   **自动收缩**：当焦点离开侧边栏进入内容区时，侧边栏应自动收缩为图标模式，扩充内容视野。

### 2.3 布局与呼吸感 (Layout)
*   **安全区域 (Safe Areas)**：四周必须保留至少 **60dp** 的边距，防止电视物理边缘遮挡或过扫描。
*   **自适应网格**：书架采用网格布局，间距需保持在 **24dp - 48dp** 之间，确保视觉疏朗。

---

## 3. 技术实现架构 (Technical Stack)

*   **UI 框架**：Jetpack Compose for TV (Material 3)。
*   **核心逻辑**：
    *   **Focus-Follows-Switch**：通过 `onFocusChanged` 监听侧边栏焦点并实时更新 `selectedScreen` 状态。
    *   **BookCard 特效**：使用 `CardDefaults.scale` 和 `CardDefaults.glow` 实现。
*   **存储引擎**：Room 数据库持久化书籍元数据。
*   **传书协议**：Ktor HTTP Server (内置无线上传)。

---

## 4. 阅读器细节规范 (Reader Guidelines)

*   **排版**：
    *   默认字号：**32sp** (适合 3 米外的典型观看距离)。
    *   行间距：**1.5x - 1.8x** 字体大小。
*   **色彩方案**：
    *   **羊皮纸**：背景 `#F5F2E9`，文字 `#2C2C2C`（默认，护眼）。
    *   **深海**：背景 `#1A1C2C`，文字 `#A0A5B1`（夜间模式）。
*   **操作**：
    *   **左右键**：切换章节。
    *   **确定键**：呼出半透明控制面板。

---

## 5. 开发状态追踪 (Milestones)

- [x] **Phase 1: Apple TV 导航架构** (侧边栏联动、焦点缩放)。
- [x] **Phase 2: 核心阅读引擎** (TXT 流式解析、自动编码、章节识别)。
- [x] **Phase 3: 暮阅·互联** (无线扫码传书、Documents 公共目录存储)。
- [ ] **Phase 4: 细节打磨** (EPUB 样式解析优化、多主题切换、长按删除交互)。

---

## 6. 维护说明
本项目代码中的中文注释应保持详尽，所有 UI 组件应优先考虑遥控器 D-pad 的焦点寻址逻辑。
