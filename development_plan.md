# Dusk Reader TV - 开发设计文档

## 1. 项目概述
Dusk Reader TV 是一款专为 Android TV 设计的本地及局域网小说阅读器。旨在为用户在大屏幕上提供极致舒适的沉浸式阅读体验，支持常见的电子书格式及私有云存储挂载。

### 核心价值
- **大屏优化**：完全适配 TV 遥控器操作，避免复杂的鼠标模拟点击。
- **私有化**：支持 WebDAV/SMB，方便用户读取 NAS 或网盘中的书籍。
- **纯净阅读**：无广告，无推荐，回归阅读本质。

---

## 2. 功能架构

### 2.1 核心功能
- **书籍管理**：支持从本地存储、USB、WebDAV、SMB 导入书籍。
- **阅读引擎**：
    - 支持 TXT (智能断章、自动编码识别)。
    - 支持 EPUB (解析 CSS、图片、多章节目录)。
- **阅读体验**：
    - 字体大小、行间距、页边距自定义。
    - 多种背景方案 (羊皮纸、夜间模式等)。
    - 翻页动画 (左右翻页、无缝滚动)。
- **进度同步**：本地保存阅读历史，支持书签功能。

### 2.2 TV 特色功能
- **侧边栏导航**：快速切换书架、最近阅读、文件管理、设置。
- **D-Pad 优化**：确保所有 UI 元素在遥控器操作下焦点清晰、逻辑通顺。
- **屏保模式**：阅读时长时间无操作可进入时钟或画报屏保。

---

## 3. 技术栈建议

- **UI 框架**：[Jetpack Compose for TV](https://developer.android.com/tv/compose) (最新 Material3 规范)。
- **架构模式**：MVVM (Model-View-ViewModel) + Repository。
- **数据库**：Room (存储书籍元数据、书签、历史)。
- **网络/WebDAV**：OkHttp + Ktor Client (用于 WebDAV 请求)。
- **本地化存储**：DataStore (存储设置信息)。
- **图片加载**：Coil (加载书籍封面)。
- **书籍解析**：
    - TXT：自定义流式解析。
    - EPUB：[epublib](https://github.com/psiegman/epublib) 或类似的轻量级库。

---

## 4. UI/UX 设计方案

### 4.1 导航布局 (Left Drawer)
- **书架 (Shelf)**：默认首页，展示书籍封面。
- **最近 (Recent)**：展示最近读过的 3-5 本书。
- **文件管理 (File Explorer)**：浏览本地及挂载的网络路径。
- **设置 (Settings)**：阅读偏好、存储管理。

### 4.2 阅读器界面
- **主阅读区**：文字居中，左右留白防止边缘拉伸。
- **菜单控制层 (呼出)**：点击遥控器“确定”键弹出，显示进度条、目录、样式设置。
- **目录菜单**：右侧弹出式列表。

---

## 5. 数据模型设计 (Room)

```kotlin
// 书籍实体
@Entity
data class Book(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val author: String?,
    val path: String,          // 本地或网络路径
    val coverPath: String?,
    val format: String,        // TXT, EPUB
    val lastReadChapter: Int = 0,
    val lastReadPosition: Int = 0,
    val lastReadTime: Long = System.currentTimeMillis()
)

// 书签实体
@Entity
data class Bookmark(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val bookId: Long,
    val chapterIndex: Int,
    val position: Int,
    val content: String,       // 摘要
    val timestamp: Long
)
```

---

## 6. 开发路线图 (Roadmap)

### 第一阶段：基础框架与 MVP
1. 初始化项目，配置 Compose for TV 环境。
2. 实现侧边栏导航逻辑。
3. 实现基础 TXT 阅读器（能够加载并展示文字，支持翻页）。

### 第二阶段：书籍管理与持久化
1. 集成 Room 数据库。
2. 开发本地文件浏览功能。
3. 实现书架功能，支持书籍导入。

### 第三阶段：功能增强
1. 集成 EPUB 解析库。
2. 完善阅读设置（字体、间距、颜色主题）。
3. 实现书签与阅读进度记录。

### 第四阶段：局域网支持与优化
1. 实现 WebDAV 挂载功能。
2. 优化 TV 端的动画效果与性能。
3. 适配不同尺寸的电视大屏。

---

## 7. AI 开发辅助建议 (给你的提示词参考)
当你需要我帮你写代码时，你可以这样问：
- *"帮我用 Compose for TV 写一个左侧抽屉导航的界面框架。"*
- *"我需要一个 Room 数据库的 Repository，用于保存书籍信息。"*
- *"写一个 Kotlin 函数，能把几百 MB 的 TXT 文件按章节名正则切割并流式读取。"*
- *"帮我美化一下电视端的阅读器样式，背景要求类似纸质书感。"*
