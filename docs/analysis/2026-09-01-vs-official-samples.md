# 暮阅 vs 官方样例深度对比分析

> 日期：2026-09-01
> 对照对象：`/Users/lu/AIProjects/tv-samples`（Google 官方 android/tv-samples 完整克隆）
> 分析基线：`main @ 08717ad`（焦点实验改动已 stash）
> 地位：焦点/滚动代码改动前的**必读文档**；修复方案的依据来源。结论均已在官方新克隆上逐行复核。

---

## 0. TL;DR

暮阅的**领域层（阅读器、数据、传输）明显强于官方样例**；**UI 焦点层弱于样例**——不是因为能力不足，而是把官方"组件即焦点治理单元"的架构拆掉，换成了每屏手写焦点的自创机制。近期"越改越乱"的全部症状（焦点乱跳、UI 抖动）都能追溯到 4 个具体提交的具体改动。

---

## 1. 官方样例定位

| 样例 | 技术栈 | 性质 | 对暮阅的价值 |
|---|---|---|---|
| JetStreamCompose | Compose for TV | 完整流媒体 App | **架构母本**：Dashboard/TopBar/Details/双 NavHost 直接源于它 |
| TvMaterialCatalog | Compose for TV | 组件目录（非 App） | **组件用法权威**：`PositionFocusedItemInLazyLayout`（pivot 滚动）出处 |
| Leanback / LeanbackShowcase | Java + Leanback | 旧栈演示 | 无价值（暮阅 No-Leanback） |
| ClassicsKotlin | Media3 | 视频播放 | 无价值 |
| ReferenceAppKotlin | 混合 | Google TV 生态集成 | 无价值 |
| AccessibilityDemo | Java | 无障碍 | 低价值 |

**版本差异（重要）**：两个官方样例用 tv-material **1.0.0**；暮阅已升级到 **1.1.0**（提交 `7ae9bad`）。升级本身干净，但 1.1.0 的 focus/glow/scale 行为与 1.0.0 有差异，**后续焦点修复必须以 1.1.0 行为为准重新走查**，不能假设"官方样例的表现 = 暮阅的表现"。

---

## 2. 焦点退化链（bug 根因，按因果排序）

所有引用均为官方新克隆的实测行号。

### 2.1 顶栏显隐被焦点劫持（`f0b8756` 引入）

- **官方**：`HomeScreen.kt` L93-104 — `onScroll(shouldShowTopBar)` 纯滚动位置驱动；官方的 `immersiveListHasFocus` state 只赋值从不读取（死变量）
- **暮阅现状**：`BookshelfScreen.kt` — `onScroll(shouldShowTopBar && !gridHasFocus)`
- **后果**：网格一聚焦 → 顶栏收起 → `animateScrollToItem(0)` 拉回顶部 → 布局整体上移 → 聚焦项视觉"跳行"；从网格按向上进 toolbar 时顶栏弹出又收起，来回抖动

### 2.2 focusRestorer 的 fallback 被删（`29ece52` 引入）

- **官方模式**（两处独立证实）：
  - JetStream `MoviesRow.kt` L110/180 — `focusRestorer { firstItem }` + index 0 挂 `focusRequester(firstItem)` + 点击时 `saveFocusedChild()`
  - TvMaterialCatalog `ImmersiveListScreen.kt` — 同模式
- **暮阅现状**：无参 `focusRestorer()`（fallback 已删）
- **历史真相**：`f0b8756` 曾引入与官方一致的 `focusRestorer { firstBookRequester }`；但 LazyVerticalGrid 快速滚动会回收首项 → `requestFocus()` 抛 IllegalStateException 闪退 → `29ece52` 为修崩溃把 fallback 整个删掉
- **后果**：恢复失败时落到"任意第一个已组合项"→ 焦点突然跳回第一项（用户感知的"乱跳"主源）
- **正确修法**：保留 restorer，但 fallback 指向**恒组合锚点**（如 toolbar 按钮）而非 Lazy 项；首项 requester 调用走 `requestFocusSafely()`

### 2.3 自定义滚动 tween（`08717ad` 引入）

- **官方**：TvMaterialCatalog `PositionFocusedItemInLazyLayout.kt` 只覆写 `calculateScrollDistance`，**刻意不碰 `scrollAnimationSpec`**
- **暮阅现状**：`tween(220ms, LinearOutSlowInEasing)`
- **后果**：默认弹簧动画支持连发按键时平滑重定向；tween 每次按键从静止重启 → 滚动追不上焦点 → 焦点飞出视口后 2D 搜索兜底乱跳

### 2.4 自创版本号首焦协议（多提交累积）

- **官方**：首焦只有 DashboardTopBar 一处 `LaunchedEffect`；内容区首焦交给 restorer 兜底。**无任何版本号机制**
- **暮阅现状**：`requestInitialFocusVersion` + `onRequestContentFocus` + 每屏 `LaunchedEffect(version)` + TransferScreen 双重 `handled*Version` 状态——四屏四个实现
- **后果**：时序敏感（组合时机/状态 Ready 时机），任何一环不对焦点就落空；难调试

### 2.5 网格 per-item 焦点规则过度防御（`f0b8756` 引入）

- **官方**：`CategoriesScreen.kt` L127-131 — 真网格只有 1 条规则（首列 `left = Cancel`）；TvMaterialCatalog 网格零规则
- **暮阅现状**：4 条/项（left/right/up/down 全配齐）
- **后果**：与搜索/过滤引发的动态重组交互面大，规则与 2D 几何搜索打架

---

## 3. 架构设计问题（非 bug，但结构性债）

### A1. 主题层空转

`theme/Theme.kt` 与官方逐字相同，但全 App **零组件消费 colorScheme**——20 个 colors.xml token + darkColorScheme 整链死配置。所有颜色硬编码 `Color.White.copy(alpha=…)` 散落各屏。官方每个组件都走 `colorScheme.onSurface` 等 token。**新代码建议定义 `DuskColors` object 沉淀常量**（见 DESIGN.md §10-2）。

### A2. 焦点治理责任倒置

官方把焦点逻辑收敛在**容器组件内部**（MoviesRow 组件内 createRefs/restorer/saveFocusedChild，业务屏零焦点代码）；暮阅摊到**每个业务屏**手写（每屏 3-5 个 FocusRequester）。缺少"自带焦点治理的可复用容器"是焦点 bug 每屏复发的结构根源。**修法：书库网格重构为 `BooksGrid` 容器组件（搬 MoviesRow 模式）**。

### A3. 死代码堆积

`BookCard`（零调用者，官方对应物 MovieCard 是全 App 使用最广的组件）、`SectionHeader`、`SecondaryPanel`、`ModifierUtils` 的 `createInitialFocusRestorerModifiers`/`focusOnInitialVisibility`/`ifElse`、`BookshelfScreen` 的 `onGoTransfer`/`onGoBookshelf` 死参数（空状态"前往传书"按钮没做完的残骸）。

### A4. rememberSaveable 覆盖不足

官方系统使用（CarouselSaver/Profile 设置项/顶栏状态）；暮阅仅 5 处且 4 处服务于已废弃的版本号机制。网格滚动位置、搜索词、对话框开关在进程重建后全丢。

### A5. 转场动画失配

官方 MoviesRow 用 `AnimatedContent` 平滑换数据；暮阅全 App 0 处 AnimatedContent（列表瞬变），却关掉了页面级转场。该平滑的没平滑。

### A6. 扫描入口无单一 owner

`scanLocalStorage()` 被 4 处直接调用（MainActivity 权限回调 / FileTransferServer 两处 / SettingsVM / BookshelfVM），无互斥；大书库并发扫描有 Room 写竞争风险。

## 4. 组件使用问题

### B1. 系统组件弃用，手写变体泛滥

| 官方组件 | 暮阅现状 | 差距 |
|---|---|---|
| `StandardCardContainer`（MovieCard） | LibraryBookTile 手写 | 官方容器自带焦点/语义，暮阅手拼 |
| `ListItem`（官方 7 处） | SettingsRow 手写 | 设置行/对话框选项/目录项各自手写 |
| `FilterChip`（筛选行平铺） | DuskTvButton + StandardDialog | 官方即时反馈平铺；暮阅多一层弹窗（设计规范 §5.2 自认反模式） |
| `Carousel`（Hero 轮播，D-pad 换内容） | 静态 Hero 卡 | 官方 Hero 可聚焦轮播；暮阅 Hero 只有按钮可聚焦 |

### B2. 布局骨架细节

- 详情页按钮缺 `BringIntoViewRequester`（官方 MovieDetails 模式；设计规范 §2.4-4 要求未做）
- DashboardTopBar 的 `remember { TopBarFocusRequesters }` 无意义包装（单例顶层 val 套 remember）

### B3. 细节不一致

- `LibraryBookTile` 聚焦边框 **3dp**（L754），规范要求 2dp——书库聚焦态与全局不统一的直接原因
- `JetStreamCardShape = ShapeDefaults.Large(16dp)` 与官方 `ExtraSmall(4dp)` 已分叉（刻意放大，OK），但旧文档声称的"shapes 体系"与实际取值需以 DESIGN.md §3 为准

## 5. 暮阅强于官方的部分（不要在重构中丢掉）

1. **真实数据层**：Room 持久化 + 章节级文件加载 + `@Immutable` entities + 6 个纯逻辑测试类（官方全是内存假数据）
2. **NavHost 转场**：去掉 navigation-compose 默认 700ms 淡入淡出（官方没做）——TV"即时换台"正确手感
3. **自研阅读器**：TextMeasurer 后台分页 + HorizontalPager + LruCache 章节缓存 + 进度字号不变量——官方根本没有对应物
4. **搜索防抖**：`debounce(250ms)` + live/committed 双流（官方搜索无防抖）

## 6. 修复优先级

| 级 | 事项 | 依据 |
|---|---|---|
| P0 | 焦点四项根因修复：onScroll 去焦点耦合 / 删自定义 tween / restorer 恒组合锚点 / 废版本号首焦 | §2.1-2.4 |
| P0 | LibraryBookTile 3dp→2dp 边框 | §B3 |
| P1 | 书库网格重构 `BooksGrid` 容器（MoviesRow 模式） | §A2 |
| P1 | 筛选改 FilterChip 平铺行，删两个 StandardDialog | §B1 |
| P1 | rememberSaveable 覆盖网格滚动/搜索词/对话框状态 | §A4 |
| P2 | 删死代码（BookCard/SectionHeader/死参数/ModifierUtils 死函数） | §A3 |
| P2 | 首页 Hero 换 Carousel | §B1 |
| P2 | AnimatedContent 列表数据过渡 | §A5 |
| P3 | DuskColors 常量沉淀 / 扫描入口单 owner | §A1/A6 |
